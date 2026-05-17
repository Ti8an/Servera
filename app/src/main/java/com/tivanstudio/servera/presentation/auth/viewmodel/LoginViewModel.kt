package com.tivanstudio.servera.presentation.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.domain.usecase.auth.IsBiometricEnabledUseCase
import com.tivanstudio.servera.domain.usecase.auth.IsPasswordSetUseCase
import com.tivanstudio.servera.domain.usecase.auth.VerifyPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val isPasswordSet: IsPasswordSetUseCase,
    private val verifyPassword: VerifyPasswordUseCase,
    private val isBiometricEnabled: IsBiometricEnabledUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val passwordSet = isPasswordSet()
            _uiState.update {
                it.copy(
                    isFirstLaunch = !passwordSet,
                    isBiometricEnabled = isBiometricEnabled(),
                    isLoading = false
                )
            }
        }
    }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun onTogglePasswordVisibility() =
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun login() {
        val password = _uiState.value.password
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val ok = verifyPassword(password)
            if (ok) {
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                _events.send(LoginEvent.NavigateToServers)
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Неверный пароль") }
            }
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch { _events.send(LoginEvent.NavigateToServers) }
    }

    fun navigateToCreatePassword() {
        viewModelScope.launch { _events.send(LoginEvent.NavigateToCreatePassword) }
    }
}
