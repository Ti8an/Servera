package com.tivanstudio.servera.presentation.console.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.data.preferences.AppPreferences
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.repository.ServerRepository
import com.tivanstudio.servera.domain.usecase.history.GetCommandHistoryUseCase
import com.tivanstudio.servera.domain.usecase.quickcommand.DeleteQuickCommandUseCase
import com.tivanstudio.servera.domain.usecase.quickcommand.GetQuickCommandsUseCase
import com.tivanstudio.servera.domain.usecase.quickcommand.SaveQuickCommandUseCase
import com.tivanstudio.servera.domain.usecase.ssh.ExecuteCommandUseCase
import com.tivanstudio.servera.domain.usecase.ssh.FetchServerInfoUseCase
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
class ConsoleViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val getHistory: GetCommandHistoryUseCase,
    private val getQuickCommands: GetQuickCommandsUseCase,
    private val saveQuickCommand: SaveQuickCommandUseCase,
    private val deleteQuickCommand: DeleteQuickCommandUseCase,
    private val fetchServerInfo: FetchServerInfoUseCase,
    private val executeCommand: ExecuteCommandUseCase,
    private val appPreferences: AppPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val serverId: Long = checkNotNull(savedStateHandle["serverId"])

    private val _uiState = MutableStateFlow(ConsoleUiState())
    val uiState: StateFlow<ConsoleUiState> = _uiState.asStateFlow()

    private val _events = Channel<ConsoleEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadServer()
        observeHistory()
        observeQuickCommands()
    }

    private fun loadServer() {
        viewModelScope.launch {
            val server = serverRepository.getServerById(serverId)
            _uiState.update { it.copy(server = server, isLoading = false) }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            getHistory.forServer(serverId).collect { history ->
                _uiState.update { it.copy(recentHistory = history.take(10)) }
            }
        }
    }

    private fun observeQuickCommands() {
        viewModelScope.launch {
            getQuickCommands().collect { cmds ->
                _uiState.update { it.copy(quickCommands = cmds) }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
        if (index == 1 && _uiState.value.serverInfo == null) {
            loadServerInfo()
        }
    }

    private fun loadServerInfo() {
        val server = _uiState.value.server ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingServerInfo = true, serverInfoError = null) }
            fetchServerInfo(server)
                .onSuccess { info ->
                    _uiState.update { it.copy(serverInfo = info, isLoadingServerInfo = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingServerInfo = false, serverInfoError = e.message) }
                }
        }
    }

    fun navigateToExecute() {
        viewModelScope.launch { _events.send(ConsoleEvent.NavigateToExecute(serverId)) }
    }

    fun executeQuickCommand(cmd: QuickCommand) {
        val server = _uiState.value.server ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(commandStatuses = it.commandStatuses + (cmd.id to QuickCommandStatus.Running))
            }
            executeCommand(server, cmd.command, saveOnFailure = appPreferences.isSaveCommandsAlways.value)
                .onSuccess {
                    _uiState.update {
                        it.copy(commandStatuses = it.commandStatuses + (cmd.id to QuickCommandStatus.Success))
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(commandStatuses = it.commandStatuses + (cmd.id to QuickCommandStatus.Failure(e.message ?: "Unknown error")))
                    }
                }
        }
    }

    fun startAddCommand() {
        val nextOrder = _uiState.value.quickCommands.size
        _uiState.update { it.copy(editingCommand = QuickCommand(id = 0, label = "", command = "", sortOrder = nextOrder)) }
    }

    fun startEditCommand(cmd: QuickCommand) {
        _uiState.update { it.copy(editingCommand = cmd) }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(editingCommand = null) }
    }

    fun saveEditedCommand(label: String, command: String) {
        val editing = _uiState.value.editingCommand ?: return
        viewModelScope.launch {
            saveQuickCommand(editing.copy(label = label.trim(), command = command.trim()))
            _uiState.update { it.copy(editingCommand = null) }
        }
    }

    fun saveAndRunEditedCommand(label: String, command: String) {
        val editing = _uiState.value.editingCommand ?: return
        val server = _uiState.value.server ?: return
        val updatedCmd = editing.copy(label = label.trim(), command = command.trim())
        viewModelScope.launch {
            val savedId: Long = saveQuickCommand(updatedCmd)
            val runnableId: Long = if (updatedCmd.id != 0L) updatedCmd.id else savedId
            _uiState.update { it.copy(editingCommand = null) }
            _uiState.update {
                it.copy(commandStatuses = it.commandStatuses + (runnableId to QuickCommandStatus.Running))
            }
            executeCommand(server, updatedCmd.command, saveOnFailure = appPreferences.isSaveCommandsAlways.value)
                .onSuccess {
                    _uiState.update {
                        it.copy(commandStatuses = it.commandStatuses + (runnableId to QuickCommandStatus.Success))
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(commandStatuses = it.commandStatuses + (runnableId to QuickCommandStatus.Failure(e.message ?: "Unknown error")))
                    }
                }
        }
    }

    fun deleteCommand(id: Long) {
        viewModelScope.launch {
            deleteQuickCommand(id)
            _uiState.update { it.copy(commandStatuses = it.commandStatuses - id) }
        }
    }
}
