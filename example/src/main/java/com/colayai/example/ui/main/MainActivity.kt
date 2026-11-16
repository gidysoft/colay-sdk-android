package com.colayai.example.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.colayai.example.ui.theme.ExampleTheme
import com.colayai.sdk.Colay

/**
 * Single Activity. **Step 7 — Deep links:** when the app is launched from a link, the SDK reads it automatically;
 * links received while the app is running are passed on in [onNewIntent] with `Colay.handleDeepLink`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ExampleTheme { MainRoute() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let(Colay::handleDeepLink)
    }
}
