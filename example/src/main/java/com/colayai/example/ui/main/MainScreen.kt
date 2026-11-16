package com.colayai.example.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Reads state from the ViewModel and forwards user actions to it. Contains no business logic. */
@Composable
fun MainRoute(viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onScreenShown() }
    MainScreen(
        state = state,
        onToggleConsent = viewModel::toggleConsent,
        onViewProduct = viewModel::viewProduct,
        onAddToCart = viewModel::addToCart,
        onSignUp = viewModel::signUp,
        onPurchase = viewModel::purchase,
        onFlush = viewModel::flush,
        onSignOut = viewModel::signOut,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    onToggleConsent: () -> Unit,
    onViewProduct: () -> Unit,
    onAddToCart: () -> Unit,
    onSignUp: () -> Unit,
    onPurchase: () -> Unit,
    onFlush: () -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Colay AI Android SDK") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Consent (analytics and ads)", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Switch(checked = state.consentGranted, onCheckedChange = { onToggleConsent() })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(state.lastAction ?: "Grant consent first.", style = MaterialTheme.typography.bodyMedium)
                    Text("Sent: ${state.sentCount}", style = MaterialTheme.typography.bodySmall)
                }
            }
            ActionButton("Screen: Product detail", onViewProduct)
            ActionButton("Event: Add to cart", onAddToCart)
            ActionButton("Event: Sign up", onSignUp)
            ActionButton("Purchase: 149.90 USD", onPurchase)
            OutlinedButton(onClick = onFlush, modifier = Modifier.fillMaxWidth()) { Text("Send now (flush)") }
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out (reset)") }
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
}
