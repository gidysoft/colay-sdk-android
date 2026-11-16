package com.colayai.sdk

/**
 * User consent.
 * - When [analytics] is false, no events are sent and the queue is cleared.
 * - When [ads] is false, events are still sent but without the advertising ID and click IDs,
 *   and the server does not forward events to ad platforms.
 */
data class Consent(val analytics: Boolean, val ads: Boolean) {
    companion object {
        @JvmField val GRANTED = Consent(analytics = true, ads = true)
        @JvmField val DENIED = Consent(analytics = false, ads = false)
        @JvmField val ANALYTICS_ONLY = Consent(analytics = true, ads = false)
    }
}

/** SDK settings. Every field has a sensible default. */
data class ColayConfig @JvmOverloads constructor(
    /** ColayAI server. */
    val baseUrl: String = "https://colayai.com",
    /**
     * Default used before the app records consent.
     * For users covered by GDPR/KVKK, [Consent.DENIED] is recommended until explicit consent is given;
     * the app then calls [Colay.setConsent]. The user's choice is persisted.
     */
    val defaultConsent: Consent = Consent.GRANTED,
    /** Events are sent right away once this many are queued. */
    val flushAt: Int = 20,
    /** Seconds between periodic sends. */
    val flushIntervalSeconds: Long = 15,
    /** Maximum number of events kept on the device. */
    val maxQueueSize: Int = 1000,
    /** Seconds of inactivity after which a new session starts. */
    val sessionTimeoutSeconds: Long = 30 * 60,
    /** Send app_install, app_open, app_background and session_start events automatically. */
    val trackLifecycle: Boolean = true,
    /** Send a screen_view named after the class whenever an Activity resumes (Compose/fragment apps should leave this off and call [Colay.screen]). */
    val trackActivities: Boolean = false,
    /** Read UTM parameters and click IDs from the link that opened the Activity automatically. */
    val trackDeepLinks: Boolean = true,
    /**
     * Whether to read the Google advertising ID. The app must include `play-services-ads-identifier`
     * and ads consent must be granted. Off by default (Play policy requires declaring this use).
     */
    val collectAdvertisingId: Boolean = false,
    /** Verbose logging to Logcat. */
    val debug: Boolean = false,
)
