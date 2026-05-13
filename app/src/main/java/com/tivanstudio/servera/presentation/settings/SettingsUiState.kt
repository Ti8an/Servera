package com.tivanstudio.servera.presentation.settings

data class SettingsUiState(
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val appVersion: String = "1.0"
)
