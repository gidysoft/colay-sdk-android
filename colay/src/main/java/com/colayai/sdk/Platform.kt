package com.colayai.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Play Store install referrer. */
internal data class InstallInfo(
    val referrer: String?,
    val clickTs: Long,
    val installBeginTs: Long,
)

internal object Platform {

    fun appInfo(ctx: Context): JSONObject {
        val o = JSONObject().put("id", ctx.packageName)
        try {
            val pi = if (Build.VERSION.SDK_INT >= 33) {
                ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            }
            o.put("version", pi.versionName ?: "")
            val build = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else {
                @Suppress("DEPRECATION")
                pi.versionCode.toLong()
            }
            o.put("build", build.toString())
        } catch (_: Exception) { }
        return o
    }

    fun deviceInfo(ctx: Context): JSONObject {
        val dm = ctx.resources.displayMetrics
        return JSONObject()
            .put("platform", "android")
            .put("os", "Android")
            .put("osVersion", Build.VERSION.RELEASE ?: "")
            .put("manufacturer", Build.MANUFACTURER ?: "")
            .put("model", Build.MODEL ?: "")
            .put("locale", Locale.getDefault().toLanguageTag())
            .put("timezone", TimeZone.getDefault().id)
            .put("screenWidth", dm.widthPixels)
            .put("screenHeight", dm.heightPixels)
    }

    /**
     * Google advertising ID. Returns null if the app does not include
     * `com.google.android.gms:play-services-ads-identifier`. Do not call on the main thread.
     */
    fun advertisingId(ctx: Context): Pair<String, Boolean>? = try {
        val cls = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
        val info = cls.getMethod("getAdvertisingIdInfo", Context::class.java).invoke(null, ctx)
        val id = info?.javaClass?.getMethod("getId")?.invoke(info) as? String
        val limited = info?.javaClass?.getMethod("isLimitAdTrackingEnabled")?.invoke(info) as? Boolean ?: false
        if (id.isNullOrEmpty() || id == "00000000-0000-0000-0000-000000000000") null else id to limited
    } catch (_: Throwable) {
        null
    }

    /** Reads the Play Install Referrer. The result arrives via [onResult] within [timeoutSeconds]. */
    fun fetchInstallReferrer(
        ctx: Context,
        scheduler: ScheduledExecutorService,
        timeoutSeconds: Long,
        onResult: (InstallInfo?) -> Unit,
    ) {
        val done = AtomicBoolean(false)
        var client: InstallReferrerClient? = null
        fun finish(info: InstallInfo?) {
            if (!done.compareAndSet(false, true)) return
            try { client?.endConnection() } catch (_: Throwable) { }
            scheduler.execute { onResult(info) }
        }
        try {
            val c = InstallReferrerClient.newBuilder(ctx).build()
            client = c
            c.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        try {
                            val d = c.installReferrer
                            finish(InstallInfo(d.installReferrer, d.referrerClickTimestampSeconds, d.installBeginTimestampSeconds))
                        } catch (_: Throwable) {
                            finish(null)
                        }
                    } else {
                        finish(null) // Play Store missing or not supported
                    }
                }

                override fun onInstallReferrerServiceDisconnected() = finish(null)
            })
        } catch (_: Throwable) {
            finish(null)
        }
        scheduler.schedule({ finish(null) }, timeoutSeconds, TimeUnit.SECONDS)
    }
}
