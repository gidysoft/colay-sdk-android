package com.colayai.example.analytics

import com.colayai.sdk.Colay
import com.colayai.sdk.Consent

/** [AnalyticsTracker] implementation backed by the Colay AI SDK. */
class ColayAnalyticsTracker : AnalyticsTracker {
    override fun setConsent(analytics: Boolean, ads: Boolean) = Colay.setConsent(Consent(analytics, ads))
    override fun screen(name: String, properties: Map<String, Any?>) = Colay.screen(name, properties)
    override fun track(event: String, properties: Map<String, Any?>) = Colay.track(event, properties)
    override fun identify(userId: String, traits: Map<String, Any?>) = Colay.identify(userId, traits)
    override fun purchase(amount: Double, currency: String, properties: Map<String, Any?>) =
        Colay.purchase(amount, currency, properties)
    override fun flush() = Colay.flush()
    override fun reset() = Colay.reset()
}
