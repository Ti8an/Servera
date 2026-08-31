package com.tivanstudio.servera.presentation.settings.viewmodel

import com.tivanstudio.servera.data.crypto.SecurityLevel
import javax.crypto.Cipher

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
    /**
     * Set while the enrollment prompt should be up. The UI drives BiometricPrompt off this and
     * clears it through [SettingsViewModel.onBiometricEnrollSuccess] or
     * [SettingsViewModel.onBiometricEnrollError].
     */
    val showBiometricPrompt: Boolean = false,
    /**
     * The un-finished encrypt cipher waiting for the prompt to release the BEK. Non-null exactly
     * while [showBiometricPrompt] is set.
     */
    val pendingBiometricCipher: Cipher? = null,
    /** String resource for a one-shot snackbar, cleared by [SettingsViewModel.onMessageShown]. */
    val messageRes: Int? = null
)
