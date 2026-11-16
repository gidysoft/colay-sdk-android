package com.colayai.example.ui.main

/** Main screen state. The screen only renders this state; changes go through [MainViewModel]. */
data class MainUiState(
    val consentGranted: Boolean = false,
    val lastAction: String? = null,
    val sentCount: Int = 0,
)
