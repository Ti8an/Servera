package com.tivanstudio.servera.presentation.auth.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.usecase.auth.ChangePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirm: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    @StringRes val error: Int? = null
)

sealed class ChangePasswordEvent {
    object PasswordChanged : ChangePasswordEvent()
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val changePassword: ChangePasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChangePasswordEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onOldPasswordChange(v: String) = _uiState.update { it.copy(oldPassword = v, error = null) }
    fun onNewPasswordChange(v: String) = _uiState.update { it.copy(newPassword = v, error = null) }
    fun onConfirmChange(v: String)     = _uiState.update { it.copy(confirm = v, error = null) }
    fun onToggleVisibility()           = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun submit() {
        val state = _uiState.value
        when {
            state.newPassword.length < CreatePasswordViewModel.MIN_PASSWORD_LENGTH ->
                _uiState.update { it.copy(error = R.string.error_password_too_short) }
            state.newPassword != state.confirm ->
                _uiState.update { it.copy(error = R.string.error_passwords_dont_match) }
            else -> viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val ok = changePassword(state.oldPassword, state.newPassword)
                if (ok) {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(ChangePasswordEvent.PasswordChanged)
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = R.string.error_wrong_password)
                    }
                }
            }
        }
    }
}
