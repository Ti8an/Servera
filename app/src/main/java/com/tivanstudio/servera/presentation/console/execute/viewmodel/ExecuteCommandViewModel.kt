package com.tivanstudio.servera.presentation.console.execute.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.di.CommandResultHolder
import com.tivanstudio.servera.domain.repository.ServerRepository
import com.tivanstudio.servera.domain.usecase.ssh.ExecuteCommandUseCase
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
class ExecuteCommandViewModel @Inject constructor(
    private val executeCommand: ExecuteCommandUseCase,
    private val serverRepository: ServerRepository,
    private val resultHolder: CommandResultHolder,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = checkNotNull(savedStateHandle["serverId"])

    private val _uiState = MutableStateFlow(ExecuteCommandUiState())
    val uiState: StateFlow<ExecuteCommandUiState> = _uiState.asStateFlow()

    private val _events = Channel<ExecuteCommandEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onCommandChange(value: String) = _uiState.update { it.copy(command = value, error = null) }

    fun setCommand(cmd: String) = _uiState.update { it.copy(command = cmd) }

    fun execute() {
        val cmd = _uiState.value.command.trim()
        if (cmd.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, error = null) }
            val server = serverRepository.getServerById(serverId)
            if (server == null) {
                _uiState.update { it.copy(isExecuting = false, error = "Сервер не найден") }
                return@launch
            }
            executeCommand(server, cmd)
                .onSuccess { result ->
                    resultHolder.result   = result
                    resultHolder.serverId = serverId
                    _uiState.update { it.copy(isExecuting = false) }
                    _events.send(ExecuteCommandEvent.NavigateToResult(result))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isExecuting = false, error = e.message ?: "Ошибка выполнения") }
                }
        }
    }
}
