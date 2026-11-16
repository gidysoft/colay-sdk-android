package com.colayai.sdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * SDK core. All state changes happen on a single thread ([worker]);
 * public API calls only hand work to that thread and never block the app.
 */
internal class ColayClient(
    context: Context,
    private val key: String,
    private val config: ColayConfig,
) {
    private val app: Context = context.applicationContext
    private val worker: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "colay-sdk").apply { isDaemon = true }
    }
    private val prefs: SharedPreferences = app.getSharedPreferences("colay_sdk", Context.MODE_PRIVATE)
    private val transport = Transport(config.baseUrl, key)

    // ── read and written on the worker thread only ──
    private lateinit var queue: EventQueue
    private lateinit var anonymousId: String
    private var userId: String? = null
    private var consent: Consent = config.defaultConsent
    private var sessionId: String? = null
    private var lastEventAt = 0L
    private var pendingAttribution: Map<String, Any?> = emptyMap()
    private var contextJson: JSONObject? = null
    private var adIdLoaded = false
    private var adId: Pair<String, Boolean>? = null
    private var backoffSeconds = 0L
    private var backoffUntil = 0L
    private var disabled = false
    private var startedActivities = 0
    private var foregroundSince = 0L

    @Volatile var currentAnonymousId: String = ""
        private set

    init {
        worker.execute {
            queue = EventQueue(File(app.filesDir, "colay/queue.jsonl"), config.maxQueueSize)
            anonymousId = prefs.getString(K_ANON, null) ?: newId().also { prefs.edit().putString(K_ANON, it).apply() }
            currentAnonymousId = anonymousId
            userId = prefs.getString(K_USER, null)
            if (prefs.contains(K_CONSENT_A)) {
                consent = Consent(prefs.getBoolean(K_CONSENT_A, true), prefs.getBoolean(K_CONSENT_ADS, true))
            }
            sessionId = prefs.getString(K_SESSION, null)
            lastEventAt = prefs.getLong(K_LAST, 0L)
            log("ready: anonymousId=$anonymousId, consent=$consent, queue=${queue.size}")
            if (config.trackLifecycle && !prefs.getBoolean(K_INSTALL, false)) sendInstall()
        }
        worker.scheduleWithFixedDelay({ flushNow() }, config.flushIntervalSeconds, config.flushIntervalSeconds, TimeUnit.SECONDS)
        (app as? Application)?.registerActivityLifecycleCallbacks(Lifecycle())
    }

    // ───────────────────────── public operations ─────────────────────────

    fun track(name: String, properties: Map<String, Any?>, revenue: Double? = null, currency: String? = null) =
        worker.execute { enqueue("track", name, properties, revenue = revenue, currency = currency) }

    fun screen(name: String, properties: Map<String, Any?>) =
        worker.execute { enqueue("screen", name, properties) }

    fun identify(id: String, traits: Map<String, Any?>) = worker.execute {
        userId = id
        prefs.edit().putString(K_USER, id).apply()
        enqueue("identify", "identify", emptyMap(), traits = traits)
    }

    fun reset() = worker.execute {
        flushNow()
        userId = null
        anonymousId = newId()
        currentAnonymousId = anonymousId
        sessionId = null
        lastEventAt = 0L
        prefs.edit().remove(K_USER).remove(K_SESSION).remove(K_LAST).putString(K_ANON, anonymousId).apply()
    }

    fun setConsent(c: Consent) = worker.execute {
        consent = c
        prefs.edit().putBoolean(K_CONSENT_A, c.analytics).putBoolean(K_CONSENT_ADS, c.ads).apply()
        if (!c.analytics) queue.clear()
        if (!c.ads) {
            adId = null
            adIdLoaded = false
        }
        log("consent: $c")
    }

    fun handleDeepLink(url: String) = worker.execute {
        val attr = Attribution.fromUrl(url)
        val full = LinkedHashMap<String, Any?>(attr).apply { put("deeplink", url) }
        if (attr.isNotEmpty()) pendingAttribution = full
        enqueue("track", "deeplink_open", mapOf("url" to url), attribution = full)
    }

    fun flush() = worker.execute { flushNow() }

    // ───────────────────────── internals ─────────────────────────

    private fun sendInstall() {
        Platform.fetchInstallReferrer(app, worker, 10) { info ->
            val attr = LinkedHashMap<String, Any?>()
            if (info?.referrer != null) {
                attr.putAll(Attribution.extract(info.referrer))
                attr["installReferrer"] = info.referrer
                if (info.clickTs > 0) attr["referrerClickTs"] = info.clickTs
                if (info.installBeginTs > 0) attr["installBeginTs"] = info.installBeginTs
            }
            if (pendingAttribution.isEmpty() && attr.isNotEmpty()) pendingAttribution = attr
            enqueue("track", "app_install", emptyMap(), attribution = attr)
            prefs.edit().putBoolean(K_INSTALL, true).apply()
            flushNow()
        }
    }

    private fun enqueue(
        type: String,
        name: String,
        properties: Map<String, Any?>,
        traits: Map<String, Any?>? = null,
        revenue: Double? = null,
        currency: String? = null,
        attribution: Map<String, Any?>? = null,
    ) {
        if (!consent.analytics || disabled) return
        val now = System.currentTimeMillis()
        touchSession(now)
        queue.add(buildEvent(type, name, properties, traits, revenue, currency, attribution, now))
        log("event: $type $name (queue ${queue.size})")
        if (queue.size >= config.flushAt) flushNow()
    }

    private fun touchSession(now: Long) {
        val expired = sessionId == null || now - lastEventAt > config.sessionTimeoutSeconds * 1000
        if (expired) {
            sessionId = newId()
            if (config.trackLifecycle) {
                val attr = pendingAttribution
                pendingAttribution = emptyMap()
                queue.add(buildEvent("track", "session_start", emptyMap(), null, null, null, attr.ifEmpty { null }, now))
            }
        }
        lastEventAt = now
        prefs.edit().putString(K_SESSION, sessionId).putLong(K_LAST, now).apply()
    }

    private fun buildEvent(
        type: String,
        name: String,
        properties: Map<String, Any?>,
        traits: Map<String, Any?>?,
        revenue: Double?,
        currency: String?,
        attribution: Map<String, Any?>?,
        at: Long,
    ): JSONObject {
        val e = JSONObject()
            .put("id", newId())
            .put("type", type)
            .put("name", name.take(64))
            .put("timestamp", Json.iso(at))
            .put("anonymousId", anonymousId)
            .put("sessionId", sessionId)
            .put("properties", Json.obj(Json.limitProps(properties)))
            .put("consent", JSONObject().put("analytics", consent.analytics).put("ads", consent.ads))
        userId?.let { e.put("userId", it) }
        if (traits != null) e.put("traits", Json.obj(Json.limitProps(traits)))
        if (revenue != null) e.put("revenue", revenue)
        if (currency != null) e.put("currency", currency.uppercase())
        if (!attribution.isNullOrEmpty()) {
            val a = if (consent.ads) attribution else Attribution.stripClickIds(attribution)
            if (a.isNotEmpty()) e.put("attribution", Json.obj(a))
        }
        return e
    }

    private fun context(): JSONObject {
        val base = contextJson ?: JSONObject()
            .put("app", Platform.appInfo(app))
            .put("device", Platform.deviceInfo(app))
            .also { contextJson = it }
        val c = JSONObject(base.toString())
        if (config.collectAdvertisingId && consent.ads) {
            if (!adIdLoaded) {
                adId = Platform.advertisingId(app)
                adIdLoaded = true
            }
            adId?.let { (id, limited) ->
                if (!limited) c.put("advertisingId", id)
                c.put("limitAdTracking", limited)
            }
        }
        return c
    }

    private fun flushNow() {
        if (!::queue.isInitialized || disabled || !consent.analytics) return
        if (System.currentTimeMillis() < backoffUntil) return
        while (queue.size > 0) {
            val batch = queue.peek(MAX_BATCH, MAX_BYTES)
            if (batch.isEmpty()) return
            if (!sendBatch(batch)) return
        }
    }

    /** true: keep going; false: stop for now. */
    private fun sendBatch(batch: List<JSONObject>): Boolean {
        val body = JSONObject()
            .put("sentAt", Json.isoNow())
            .put("sdk", JSONObject().put("name", "colay-android").put("version", BuildConfig.SDK_VERSION))
            .put("context", context())
            .put("events", JSONArray(batch))
            .toString()
        return when (val r = transport.send(body)) {
            SendResult.Ok -> {
                queue.remove(batch); backoffSeconds = 0; log("sent: ${batch.size}"); true
            }
            SendResult.Drop -> {
                queue.remove(batch); log("batch rejected by the server, dropped: ${batch.size}"); true
            }
            SendResult.Unauthorized -> {
                queue.clear(); disabled = true
                Log.w(TAG, "ColayAI key is invalid or disabled; sending stopped"); false
            }
            SendResult.TooLarge -> {
                if (batch.size == 1) { queue.remove(batch); true }
                else sendBatch(batch.subList(0, batch.size / 2))
            }
            is SendResult.Retry -> {
                backoffSeconds = r.retryAfterSeconds ?: (if (backoffSeconds == 0L) 5 else minOf(backoffSeconds * 2, 600))
                backoffUntil = System.currentTimeMillis() + backoffSeconds * 1000
                log("send failed, retrying in $backoffSeconds s"); false
            }
        }
    }

    private fun log(msg: String) {
        if (config.debug) Log.d(TAG, msg)
    }

    // ───────────────────────── app lifecycle ─────────────────────────

    private inner class Lifecycle : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(a: Activity, b: Bundle?) {
            if (config.trackDeepLinks && b == null) {
                a.intent?.data?.toString()?.let { handleDeepLink(it) }
            }
        }

        override fun onActivityStarted(a: Activity) {
            worker.execute {
                if (startedActivities++ == 0) {
                    foregroundSince = System.currentTimeMillis()
                    if (config.trackLifecycle) enqueue("track", "app_open", emptyMap())
                }
            }
        }

        override fun onActivityResumed(a: Activity) {
            if (config.trackActivities) screen(a.javaClass.simpleName, emptyMap())
        }

        override fun onActivityStopped(a: Activity) {
            worker.execute {
                startedActivities = maxOf(0, startedActivities - 1)
                if (startedActivities == 0) {
                    if (config.trackLifecycle) {
                        val secs = (System.currentTimeMillis() - foregroundSince) / 1000
                        enqueue("track", "app_background", mapOf("duration" to secs))
                    }
                    flushNow()
                }
            }
        }

        override fun onActivityPaused(a: Activity) {}
        override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
        override fun onActivityDestroyed(a: Activity) {}
    }

    private companion object {
        const val TAG = "ColaySDK"
        const val MAX_BATCH = 100
        const val MAX_BYTES = 450_000
        const val K_ANON = "anonymous_id"
        const val K_USER = "user_id"
        const val K_SESSION = "session_id"
        const val K_LAST = "last_event_at"
        const val K_INSTALL = "install_sent"
        const val K_CONSENT_A = "consent_analytics"
        const val K_CONSENT_ADS = "consent_ads"

        fun newId(): String = UUID.randomUUID().toString()
    }
}
