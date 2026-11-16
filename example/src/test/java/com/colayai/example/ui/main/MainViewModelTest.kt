package com.colayai.example.ui.main

import com.colayai.example.analytics.AnalyticsTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {

    private class FakeTracker : AnalyticsTracker {
        val calls = mutableListOf<String>()
        override fun setConsent(analytics: Boolean, ads: Boolean) { calls += "consent:$analytics" }
        override fun screen(name: String, properties: Map<String, Any?>) { calls += "screen:$name" }
        override fun track(event: String, properties: Map<String, Any?>) { calls += "track:$event" }
        override fun identify(userId: String, traits: Map<String, Any?>) { calls += "identify:$userId" }
        override fun purchase(amount: Double, currency: String, properties: Map<String, Any?>) { calls += "purchase:$amount:$currency" }
        override fun flush() { calls += "flush" }
        override fun reset() { calls += "reset" }
    }

    @Test
    fun eventsAreNotSentBeforeConsent() {
        val tracker = FakeTracker()
        val vm = MainViewModel(tracker)
        vm.addToCart()
        assertTrue(tracker.calls.isEmpty())
        assertEquals(0, vm.state.value.sentCount)
    }

    @Test
    fun eventsAreSentAfterConsent() {
        val tracker = FakeTracker()
        val vm = MainViewModel(tracker)
        vm.toggleConsent()
        vm.signUp()
        vm.purchase()
        assertEquals(listOf("consent:true", "track:sign_up", "identify:example-user-1", "purchase:149.9:USD"), tracker.calls)
        assertEquals(2, vm.state.value.sentCount)
    }

    @Test
    fun signOutResetsTrackerAndCounter() {
        val tracker = FakeTracker()
        val vm = MainViewModel(tracker)
        vm.toggleConsent(); vm.addToCart(); vm.signOut()
        assertEquals("reset", tracker.calls.last())
        assertEquals(0, vm.state.value.sentCount)
    }
}
