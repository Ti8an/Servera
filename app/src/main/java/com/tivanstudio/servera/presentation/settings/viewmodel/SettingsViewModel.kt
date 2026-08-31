package com.tivanstudio.servera.presentation.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.BuildConfig
import com.tivanstudio.servera.R
import com.tivanstudio.servera.data.crypto.PasswordKeyManager
import com.tivanstudio.servera.data.crypto.SecurityLevel
import com.tivanstudio.servera.data.preferences.AppPreferences
import com.tivanstudio.servera.data.preferences.ThemePreferences
import com.tivanstudio.servera.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themePreferences: ThemePreferences,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                isBiometricEnabled     = authRepository.isBiometricEnabled(),
                appVersion             = "${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}",
                isDarkTheme            = themePreferences.isDarkTheme.value,
                isSaveCommandsAlways   = appPreferences.isSaveCommandsAlways.value,
                isSaveResultInHistory  = appPreferences.saveResultInHistory.value,
                enhancedEnabled        = authRepository.isEnhancedEnabled(),
                currentLevel           = authRepository.getSecurityLevel()
            )
        }
        viewModelScope.launch {
            themePreferences.isDarkTheme.collect { dark ->
                _uiState.update { it.copy(isDarkTheme = dark) }
            }
        }
        viewModelScope.launch {
            appPreferences.isSaveCommandsAlways.collect { enabled ->
                _uiState.update { it.copy(isSaveCommandsAlways = enabled) }
            }
        }
        viewModelScope.launch {
            appPreferences.saveResultInHistory.collect { enabled ->
                _uiState.update { it.copy(isSaveResultInHistory = enabled) }
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            authRepository.setBiometricEnabled(enabled)
            _uiState.update { it.copy(isBiometricEnabled = enabled) }
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        themePreferences.setDarkTheme(enabled)
    }

    fun toggleSaveCommandsAlways(enabled: Boolean) {
        appPreferences.setSaveCommandsAlways(enabled)
    }

    fun toggleSaveResultInHistory(enabled: Boolean) {
        appPreferences.setSaveResultInHistory(enabled)
    }

    /**
     * The switch never flips on its own: throwing it either way only opens a dialog, and
     * `enhancedEnabled` moves once the re-wrap has actually succeeded.
     */
    fun onToggleEnhanced(enable: Boolean) {
        _uiState.update {
            if (enable) it.copy(showLevelPicker = true) else it.copy(showDisableConfirm = true)
        }
    }

    /** A level was chosen in the picker; the re-wrap still needs the password. */
    fun onPickLevel(level: SecurityLevel) {
        _uiState.update {
            it.copy(showLevelPicker = false, pendingLevel = level, levelPasswordError = false)
        }
    }

    /** Disabling was confirmed; going back to the default level also needs the password. */
    fun onConfirmDisableEnhanced() {
        _uiState.update {
            it.copy(
                showDisableConfirm = false,
                pendingLevel = PasswordKeyManager.DEFAULT_LEVEL,
                levelPasswordError = false
            )
        }
    }

    fun onSubmitLevelPassword(password: String) {
        val level = _uiState.value.pendingLevel ?: return
        if (_uiState.value.isChangingLevel) return

        viewModelScope.launch {
            _uiState.update { it.copy(isChangingLevel = true, levelPasswordError = false) }

            val result = authRepository.changeSecurityLevel(level, password)

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isChangingLevel = false,
                        pendingLevel = null,
                        enhancedEnabled = authRepository.isEnhancedEnabled(),
                        currentLevel = authRepository.getSecurityLevel(),
                        messageRes = R.string.level_changed
                    )
                } else {
                    // The vault is untouched, so the switch stays where it was.
                    it.copy(isChangingLevel = false, levelPasswordError = true)
                }
            }
        }
    }

    /** Backing out of any step of the flow leaves the level exactly as it was. */
    fun onDismissLevelChange() {
        if (_uiState.value.isChangingLevel) return
        _uiState.update {
            it.copy(
                showLevelPicker = false,
                showDisableConfirm = false,
                pendingLevel = null,
                levelPasswordError = false
            )
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(messageRes = null) }
    }
}
