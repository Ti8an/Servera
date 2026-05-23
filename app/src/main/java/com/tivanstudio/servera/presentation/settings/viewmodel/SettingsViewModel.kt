package com.tivanstudio.servera.presentation.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.BuildConfig
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
                isSaveCommandsAlways   = appPreferences.isSaveCommandsAlways.value
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
}
