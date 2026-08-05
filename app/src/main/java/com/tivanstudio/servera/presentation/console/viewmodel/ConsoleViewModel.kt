package com.tivanstudio.servera.presentation.console.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.data.preferences.AppPreferences
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.repository.ServerRepository
import com.tivanstudio.servera.domain.usecase.history.GetCommandHistoryUseCase
import com.tivanstudio.servera.domain.usecase.preset.GetPresetsUseCase
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
    private val getPresets: GetPresetsUseCase,
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
        observePresets()
        observeAttached()
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

    private fun observePresets() {
        viewModelScope.launch {
            getPresets().collect { presets ->
                _uiState.update { it.copy(presets = presets) }
            }
        }
    }

    private fun observeAttached() {
        viewModelScope.launch {
            getQuickCommands(serverId).collect { commands ->
                _uiState.update { it.copy(attachedCommands = commands) }
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

    fun openAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun dismissAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun toggleHistory() {
        _uiState.update { it.copy(showHistory = !it.showHistory) }
    }

    fun attachTyped(label: String, command: String, showOutput: Boolean) {
        val state = _uiState.value
        if (label.isBlank() || command.isBlank()) return
        viewModelScope.launch {
            saveQuickCommand(
                QuickCommand(
                    id         = 0,
                    serverId   = serverId,
                    label      = label.trim(),
                    command    = command.trim(),
                    sortOrder  = state.attachedCommands.size,
                    showOutput = showOutput
                )
            )
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun removeAttached(id: Long) {
        viewModelScope.launch { deleteQuickCommand(id) }
    }

    fun runAttached(cmd: QuickCommand) {
        val server = _uiState.value.server ?: return
        if (_uiState.value.runStates[cmd.id] is CommandRunState.Running) return
        viewModelScope.launch {
            setRunState(cmd.id, CommandRunState.Running)
            executeCommand(server, cmd.command, saveOnFailure = appPreferences.isSaveCommandsAlways.value)
                .onSuccess { result ->
                    setRunState(
                        cmd.id,
                        CommandRunState.Done(
                            stdout   = result.stdout,
                            stderr   = result.stderr,
                            exitCode = result.exitCode
                        )
                    )
                }
                .onFailure { e ->
                    setRunState(cmd.id, CommandRunState.Failure(e.message ?: "Unknown error"))
                }
        }
    }

    private fun setRunState(id: Long, state: CommandRunState) {
        _uiState.update { it.copy(runStates = it.runStates + (id to state)) }
    }
}
