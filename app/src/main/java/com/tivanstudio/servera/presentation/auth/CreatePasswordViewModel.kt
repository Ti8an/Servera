package com.tivanstudio.servera.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.domain.usecase.auth.SetPasswordUseCase
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
    val error: String? = null
)

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

    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, error = null) }
    fun onConfirmChange(v: String)  = _uiState.update { it.copy(confirm = v, error = null) }
    fun onToggleVisibility()        = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun createPassword() {
        val state = _uiState.value
        when {
            state.password.length < 4 -> _uiState.update { it.copy(error = "Пароль слишком короткий (мин. 4 символа)") }
            state.password != state.confirm -> _uiState.update { it.copy(error = "Пароли не совпадают") }
            else -> viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                setPassword(state.password)
                _uiState.update { it.copy(isLoading = false) }
                _events.send(CreatePasswordEvent.PasswordCreated)
            }
        }
    }
}
