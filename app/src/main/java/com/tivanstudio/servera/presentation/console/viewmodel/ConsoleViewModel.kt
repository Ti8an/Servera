package com.tivanstudio.servera.presentation.console.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.R
import com.tivanstudio.servera.data.preferences.AppPreferences
import com.tivanstudio.servera.di.CommandResultHolder
import com.tivanstudio.servera.di.ServerCache
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.entity.SshErrorType
import com.tivanstudio.servera.domain.entity.SshException
import com.tivanstudio.servera.domain.repository.ServerRepository
import com.tivanstudio.servera.domain.usecase.preset.GetGroupsUseCase
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val getPresets: GetPresetsUseCase,
    private val getGroups: GetGroupsUseCase,
    private val getQuickCommands: GetQuickCommandsUseCase,
    private val saveQuickCommand: SaveQuickCommandUseCase,
    private val deleteQuickCommand: DeleteQuickCommandUseCase,
    private val fetchServerInfo: FetchServerInfoUseCase,
    private val executeCommand: ExecuteCommandUseCase,
    private val resultHolder: CommandResultHolder,
    private val serverCache: ServerCache,
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
        observePresets()
        observeAttached()
    }

    private fun loadServer() {
        viewModelScope.launch {
            val server = serverRepository.getServerById(serverId)
            _uiState.update { it.copy(server = server, isLoading = false) }
        }
    }

    private fun observePresets() {
        viewModelScope.launch {
            combine(getPresets(), getGroups()) { presets, groups -> presets to groups }
                .collect { (presets, groups) ->
                    _uiState.update { it.copy(presets = presets, groups = groups) }
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
    }

    fun onInfoTabSelected() {
        serverCache.infoOf(serverId)?.let { info ->
            _uiState.update { it.copy(serverInfo = info) }
        }
    }

    fun refreshServerInfo() {
        val server = _uiState.value.server ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingServerInfo = true, serverInfoErrorRes = null) }
            fetchServerInfo(server)
                .onSuccess { info ->
                    serverCache.putInfo(serverId, info)
                    _uiState.update { it.copy(serverInfo = info, isLoadingServerInfo = false) }
                }
                .onFailure { e ->
                    val resId = when ((e as? SshException)?.type) {
                        SshErrorType.AUTH           -> R.string.ssh_error_auth
                        SshErrorType.TIMEOUT        -> R.string.ssh_error_timeout
                        SshErrorType.UNREACHABLE    -> R.string.ssh_error_unreachable
                        SshErrorType.HOST_NOT_FOUND -> R.string.ssh_error_host
                        else                        -> R.string.ssh_error_unknown
                    }
                    _uiState.update {
                        it.copy(isLoadingServerInfo = false, serverInfoErrorRes = resId)
                    }
                }
        }
    }

    fun navigateToExecute() {
        viewModelScope.launch { _events.send(ConsoleEvent.NavigateToExecute(serverId)) }
    }

    fun openCommandDialog() {
        _uiState.update { it.copy(showCommandDialog = true, editingCommand = null) }
    }

    fun startEdit(cmd: QuickCommand) {
        _uiState.update { it.copy(editingCommand = cmd, showCommandDialog = true) }
    }

    fun dismissCommandDialog() {
        _uiState.update { it.copy(showCommandDialog = false, editingCommand = null) }
    }

    /**
     * Attaches a catalog preset, keeping a snapshot of the group it came from.
     * The dialog stays open so several presets can be attached in a row.
     */
    fun attachFromCatalog(preset: Preset) {
        val state = _uiState.value
        val group = state.groups.firstOrNull { it.id == preset.groupId }
        viewModelScope.launch {
            saveQuickCommand(
                QuickCommand(
                    id            = 0,
                    serverId      = serverId,
                    label         = preset.label,
                    command       = preset.command,
                    sortOrder     = state.attachedCommands.size,
                    showOutput    = true,
                    groupName     = group?.name,
                    groupColorHex = group?.colorHex
                )
            )
        }
    }

    /** Saves a hand-typed command; an edit keeps its id, so REPLACE overwrites the row. */
    fun saveOwn(label: String, command: String, showOutput: Boolean, group: PresetGroup) {
        if (label.isBlank() || command.isBlank()) return
        val state   = _uiState.value
        val editing = state.editingCommand
        viewModelScope.launch {
            saveQuickCommand(
                QuickCommand(
                    id            = editing?.id ?: 0,
                    serverId      = editing?.serverId ?: serverId,
                    label         = label.trim(),
                    command       = command.trim(),
                    sortOrder     = editing?.sortOrder ?: state.attachedCommands.size,
                    showOutput    = showOutput,
                    groupName     = group.name,
                    groupColorHex = group.colorHex
                )
            )
            dismissCommandDialog()
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
            executeCommand(
                server,
                cmd.command,
                saveOnFailure = appPreferences.isSaveCommandsAlways.value,
                groupName     = cmd.groupName,
                saveResult    = appPreferences.saveResultInHistory.value
            )
                .onSuccess { result ->
                    setRunState(
                        cmd.id,
                        CommandRunState.Done(
                            stdout   = result.stdout,
                            stderr   = result.stderr,
                            exitCode = result.exitCode
                        )
                    )
                    if (cmd.showOutput) {
                        resultHolder.result      = result
                        resultHolder.serverId    = serverId
                        resultHolder.serverName  = server.name
                        resultHolder.serverHost  = server.host
                        resultHolder.groupName   = null
                        resultHolder.command     = result.command
                        resultHolder.exitCode    = result.exitCode
                        resultHolder.outputSaved = true
                        _events.send(ConsoleEvent.NavigateToResult)
                    }
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
