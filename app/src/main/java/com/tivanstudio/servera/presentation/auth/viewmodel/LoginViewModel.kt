package com.tivanstudio.servera.presentation.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.usecase.auth.IsBiometricEnabledUseCase
import com.tivanstudio.servera.domain.usecase.auth.IsPasswordSetUseCase
import com.tivanstudio.servera.domain.usecase.auth.ResetVaultUseCase
import com.tivanstudio.servera.domain.usecase.auth.VerifyPasswordUseCase
import com.tivanstudio.servera.di.SessionKeyHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TODO(biometrics): biometric login is off while the DEK can only be unwrapped by the
 *  password-derived KEK -- a fingerprint alone cannot open the vault, so letting it through
 *  would leave the session locked and every read would fail. Turn this back on once the DEK
 *  gets a second, biometric-wrapped copy (separate step). The Settings toggle keeps working
 *  and its value is preserved.
 */
private const val BIOMETRIC_UNLOCK_SUPPORTED = false

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val isPasswordSet: IsPasswordSetUseCase,
    private val verifyPassword: VerifyPasswordUseCase,
    private val isBiometricEnabled: IsBiometricEnabledUseCase,
    private val resetVault: ResetVaultUseCase,
    private val session: SessionKeyHolder
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
            // The DEK outlives this screen: it is dropped only on process death, never on
            // backgrounding. Coming back to an unlocked session must not ask for the password
            // again -- and must not pay for another PBKDF2 derivation to learn nothing new.
            if (session.isUnlocked()) {
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                _events.send(LoginEvent.NavigateToServers)
                return@launch
            }

            val passwordSet = isPasswordSet()
            _uiState.update {
                it.copy(
                    isFirstLaunch = !passwordSet,
                    isBiometricEnabled = isBiometricEnabled() && BIOMETRIC_UNLOCK_SUPPORTED,
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
            // Set before the call so the spinner gets a frame: verifyPassword now suspends
            // onto Dispatchers.Default instead of blocking this one.
            _uiState.update { it.copy(isLoading = true, error = null) }
            val ok = verifyPassword(password)
            if (ok) {
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                _events.send(LoginEvent.NavigateToServers)
            } else {
                _uiState.update { it.copy(isLoading = false, error = R.string.error_wrong_password) }
            }
        }
    }

    fun onBiometricSuccess() {
        // Unreachable while BIOMETRIC_UNLOCK_SUPPORTED is false: the button is hidden.
        if (!BIOMETRIC_UNLOCK_SUPPORTED) return
        viewModelScope.launch { _events.send(LoginEvent.NavigateToServers) }
    }

    /** Only reachable on a genuine first launch, when no vault exists yet. */
    fun navigateToCreatePassword() {
        viewModelScope.launch { _events.send(LoginEvent.NavigateToCreatePassword) }
    }

    fun onForgotPassword() = _uiState.update { it.copy(showResetDialog = true) }

    fun onDismissResetDialog() = _uiState.update { it.copy(showResetDialog = false) }

    /** Wipes everything and sends the user to create a password for the new, empty vault. */
    fun confirmReset() {
        viewModelScope.launch {
            _uiState.update { it.copy(isResetting = true) }
            resetVault()
            _uiState.update {
                it.copy(
                    showResetDialog = false,
                    isResetting = false,
                    isFirstLaunch = true,
                    isBiometricEnabled = false,
                    password = "",
                    error = null
                )
            }
            _events.send(LoginEvent.NavigateToCreatePassword)
        }
    }
}
