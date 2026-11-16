package com.colayai.example.analytics

/**
 * Abstraction over the app's analytics needs.
 *
 * ViewModels depend on this interface rather than on the SDK directly, so tests can pass a fake
 * implementation and adding another analytics tool later does not change the ViewModel.
 */
interface AnalyticsTracker {
    fun setConsent(analytics: Boolean, ads: Boolean)
    fun screen(name: String, properties: Map<String, Any?> = emptyMap())
    fun track(event: String, properties: Map<String, Any?> = emptyMap())
    fun identify(userId: String, traits: Map<String, Any?> = emptyMap())
    fun purchase(amount: Double, currency: String, properties: Map<String, Any?> = emptyMap())
    fun flush()
    fun reset()
}
