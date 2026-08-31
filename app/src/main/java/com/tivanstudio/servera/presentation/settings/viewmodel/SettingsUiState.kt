package com.tivanstudio.servera.presentation.settings.viewmodel

import com.tivanstudio.servera.data.crypto.SecurityLevel

data class SettingsUiState(
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val appVersion: String = "",
    val isDarkTheme: Boolean = true,
    val isSaveCommandsAlways: Boolean = false,
    val isSaveResultInHistory: Boolean = false,
    val enhancedEnabled: Boolean = false,
    val currentLevel: SecurityLevel = SecurityLevel.MIN,
    /** Set while the user is choosing a level in the picker dialog. */
    val showLevelPicker: Boolean = false,
    /** Set while the user is confirming that enhanced security should go off. */
    val showDisableConfirm: Boolean = false,
    /**
     * The level waiting to be applied once the password is in. Non-null exactly while the
     * password dialog is up.
     */
    val pendingLevel: SecurityLevel? = null,
    /** Set while the re-wrap runs -- a full PBKDF2 derivation each way, so about a second. */
    val isChangingLevel: Boolean = false,
    val levelPasswordError: Boolean = false,
    /** String resource for a one-shot snackbar, cleared by [SettingsViewModel.onMessageShown]. */
    val messageRes: Int? = null
)
