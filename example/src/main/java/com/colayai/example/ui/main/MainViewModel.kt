package com.colayai.example.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.colayai.example.analytics.AnalyticsTracker
import com.colayai.example.analytics.ColayAnalyticsTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Main screen logic. Each user action maps to one analytics call:
 *
 * - **Step 2 — Consent:** [toggleConsent]
 * - **Step 3 — Screens:** [onScreenShown]
 * - **Step 4 — Events:** [addToCart], [signUp] (standard event names are listed in the README)
 * - **Step 5 — User:** `identify` after sign-in
 * - **Step 6 — Purchase:** [purchase]
 */
class MainViewModel(private val tracker: AnalyticsTracker) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    fun onScreenShown() = tracker.screen("Home")

    fun toggleConsent() {
        val granted = !_state.value.consentGranted
        tracker.setConsent(analytics = granted, ads = granted)
        _state.update {
            it.copy(
                consentGranted = granted,
                lastAction = if (granted) "Consent granted: events will be sent." else "Consent withdrawn: nothing is sent.",
            )
        }
    }

    fun viewProduct() = record("screen_view: Product detail") {
        tracker.screen("Product detail", mapOf("productId" to PRODUCT_ID))
    }

    fun addToCart() = record("add_to_cart") {
        tracker.track("add_to_cart", mapOf("productId" to PRODUCT_ID, "price" to PRICE))
    }

    fun signUp() = record("sign_up + identify") {
        tracker.track("sign_up", mapOf("method" to "email"))
        tracker.identify("example-user-1", mapOf("email" to "user@example.com"))
    }

    fun purchase() = record("purchase: 149.90 USD") {
        tracker.purchase(PRICE, "USD", mapOf("orderId" to "EXAMPLE-${System.currentTimeMillis()}"))
    }

    fun flush() {
        tracker.flush()
        _state.update { it.copy(lastAction = "Sending the queue to the server") }
    }

    fun signOut() {
        tracker.reset()
        _state.update { it.copy(lastAction = "User and session reset", sentCount = 0) }
    }

    private inline fun record(label: String, send: () -> Unit) {
        if (!_state.value.consentGranted) {
            _state.update { it.copy(lastAction = "Grant consent first (no events are sent without consent).") }
            return
        }
        send()
        _state.update { it.copy(lastAction = "$label sent", sentCount = it.sentCount + 1) }
    }

    companion object {
        private const val PRODUCT_ID = "thermos-500"
        private const val PRICE = 149.90

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { MainViewModel(ColayAnalyticsTracker()) }
        }
    }
}
