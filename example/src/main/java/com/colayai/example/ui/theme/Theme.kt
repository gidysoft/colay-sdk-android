package com.colayai.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColayOrange = Color(0xFFF97316)

@Composable
fun ExampleTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = ColayOrange) else lightColorScheme(primary = ColayOrange)
    MaterialTheme(colorScheme = colors, content = content)
}
