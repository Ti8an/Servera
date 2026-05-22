package com.tivanstudio.servera.presentation.settings.viewmodel

data class SettingsUiState(
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val appVersion: String = "",
    val isDarkTheme: Boolean = true,
    val isSaveCommandsAlways: Boolean = false
)
