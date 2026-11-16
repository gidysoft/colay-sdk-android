package com.colayai.example

import android.app.Application
import com.colayai.sdk.Colay
import com.colayai.sdk.ColayConfig
import com.colayai.sdk.Consent

/**
 * Application entry point.
 *
 * **Step 1 — Initialize the SDK.** Call `Colay.init` once when the app starts.
 * - Key: Dashboard → Ads → Tracking setup → Android source (starts with `colay_pk_`; it is not a secret).
 * - For users covered by GDPR/KVKK, start with [Consent.DENIED] until explicit consent is given;
 *   call `Colay.setConsent` after the consent screen (here: [com.colayai.example.ui.main.MainViewModel]).
 */
class ExampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Colay.init(
            context = this,
            key = BuildConfig.COLAY_KEY,
            config = ColayConfig(
                baseUrl = BuildConfig.COLAY_BASE_URL,
                defaultConsent = Consent.DENIED,
                flushAt = 5,              // send quickly in the example (default is 20)
                flushIntervalSeconds = 5,
                debug = BuildConfig.DEBUG, // Logcat tag: ColaySDK
            ),
        )
    }
}
