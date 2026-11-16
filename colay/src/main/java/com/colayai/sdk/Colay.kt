package com.colayai.sdk

import android.content.Context
import android.net.Uri
import android.util.Log

/**
 * ColayAI tracking SDK.
 *
 * ```kotlin
 * class App : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         Colay.init(this, "colay_pk_xxx")
 *     }
 * }
 * Colay.screen("Cart")
 * Colay.track("sign_up", mapOf("method" to "email"))
 * Colay.identify("u_123", mapOf("email" to "jane@example.com"))
 * Colay.purchase(149.90, "USD", mapOf("orderId" to "A-1001"))
 * ```
 *
 * Every call returns immediately; work happens on a background thread. Calls made before [init] are ignored.
 */
object Colay {

    @Volatile private var client: ColayClient? = null

    /** Initializes the SDK. Call it once from `Application.onCreate`. */
    @JvmStatic
    @JvmOverloads
    fun init(context: Context, key: String, config: ColayConfig = ColayConfig()) {
        if (client != null) return
        synchronized(this) {
            if (client != null) return
            require(key.startsWith("colay_pk_")) { "ColayAI: the publishable key must start with 'colay_pk_'" }
            client = ColayClient(context, key, config)
        }
    }

    /** Custom or standard event (see PROTOCOL.md: sign_up, login, lead, purchase…). */
    @JvmStatic
    @JvmOverloads
    fun track(name: String, properties: Map<String, Any?> = emptyMap()) {
        c()?.track(name, properties)
    }

    /** Screen view. Screen funnels are built from these events. */
    @JvmStatic
    @JvmOverloads
    fun screen(name: String, properties: Map<String, Any?> = emptyMap()) {
        c()?.screen(name, properties)
    }

    /** The user signed in or signed up: earlier anonymous events are linked to this user. */
    @JvmStatic
    @JvmOverloads
    fun identify(userId: String, traits: Map<String, Any?> = emptyMap()) {
        c()?.identify(userId, traits)
    }

    /** Purchase. The spend vs. revenue report is built from this event. */
    @JvmStatic
    @JvmOverloads
    fun purchase(amount: Double, currency: String, properties: Map<String, Any?> = emptyMap()) {
        c()?.track("purchase", properties, revenue = amount, currency = currency)
    }

    /** The user's consent. It is persisted and applies to later app launches too. */
    @JvmStatic
    fun setConsent(consent: Consent) {
        c()?.setConsent(consent)
    }

    /** Call when the app is opened from a link (especially in `onNewIntent`). */
    @JvmStatic
    fun handleDeepLink(uri: Uri) {
        c()?.handleDeepLink(uri.toString())
    }

    @JvmStatic
    fun handleDeepLink(url: String) {
        c()?.handleDeepLink(url)
    }

    /** The user signed out: the user link is cleared and a new anonymous ID is generated. */
    @JvmStatic
    fun reset() {
        c()?.reset()
    }

    /** Tries to send queued events right away. */
    @JvmStatic
    fun flush() {
        c()?.flush()
    }

    /** The device's anonymous ID (for support and debugging). */
    @JvmStatic
    val anonymousId: String
        get() = client?.currentAnonymousId ?: ""

    private fun c(): ColayClient? = client ?: run {
        Log.w("ColaySDK", "Used before Colay.init was called; event ignored")
        null
    }
}
