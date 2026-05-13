package com.tivanstudio.servera.presentation.auth

data class LoginUiState(
    val isFirstLaunch: Boolean = false,
    val isLoading: Boolean = true,
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

sealed class LoginEvent {
    object NavigateToServers        : LoginEvent()
    object NavigateToCreatePassword : LoginEvent()
    data class ShowError(val msg: String) : LoginEvent()
}
