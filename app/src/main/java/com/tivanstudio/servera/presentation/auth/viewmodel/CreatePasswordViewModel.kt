package com.tivanstudio.servera.presentation.auth.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.usecase.auth.SetPasswordUseCase
import com.tivanstudio.servera.presentation.auth.PasswordStrength
import com.tivanstudio.servera.presentation.auth.checkPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatePasswordUiState(
    val password: String = "",
    val confirm: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val strength: PasswordStrength = PasswordStrength.WEAK,
    /** Why the password itself is rejected; null once it satisfies the minimum. */
    @StringRes val passwordError: Int? = null,
    @StringRes val error: Int? = null
) {
    /** The password passes the rules and the confirmation matches it. */
    val canSubmit: Boolean
        get() = passwordError == null && password.isNotEmpty() && password == confirm
}

sealed class CreatePasswordEvent {
    object PasswordCreated : CreatePasswordEvent()
    data class ShowError(val msg: String) : CreatePasswordEvent()
}

@HiltViewModel
class CreatePasswordViewModel @Inject constructor(
    private val setPassword: SetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePasswordUiState())
    val uiState: StateFlow<CreatePasswordUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreatePasswordEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onPasswordChange(v: String) {
        val check = checkPassword(v)
        _uiState.update {
            it.copy(
                password = v,
                strength = check.strength,
                passwordError = check.errorRes,
                error = null
            )
        }
    }

    fun onConfirmChange(v: String) = _uiState.update { it.copy(confirm = v, error = null) }
    fun onToggleVisibility()       = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun createPassword() {
        val state = _uiState.value
        when {
            state.passwordError != null ->
                _uiState.update { it.copy(error = state.passwordError) }
            state.password != state.confirm ->
                _uiState.update { it.copy(error = R.string.error_passwords_dont_match) }
            else -> viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                setPassword(state.password)
                _uiState.update { it.copy(isLoading = false) }
                _events.send(CreatePasswordEvent.PasswordCreated)
            }
        }
    }
}
