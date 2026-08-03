package com.tivanstudio.servera.presentation.console.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.data.preferences.AppPreferences
import com.tivanstudio.servera.di.CommandResultHolder
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetSource
import com.tivanstudio.servera.domain.repository.ServerRepository
import com.tivanstudio.servera.domain.usecase.history.GetCommandHistoryUseCase
import com.tivanstudio.servera.domain.usecase.preset.AddCustomPresetUseCase
import com.tivanstudio.servera.domain.usecase.preset.DeleteCustomPresetUseCase
import com.tivanstudio.servera.domain.usecase.preset.GetPresetsUseCase
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
    private val fetchServerInfo: FetchServerInfoUseCase,
    private val executeCommand: ExecuteCommandUseCase,
    private val getPresets: GetPresetsUseCase,
    private val addCustomPreset: AddCustomPresetUseCase,
    private val deleteCustomPreset: DeleteCustomPresetUseCase,
    private val resultHolder: CommandResultHolder,
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

    fun runPreset(preset: Preset) {
        val server = _uiState.value.server ?: return
        if (_uiState.value.runningPresetId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(runningPresetId = preset.id, presetError = null) }
            executeCommand(server, preset.command, saveOnFailure = appPreferences.isSaveCommandsAlways.value)
                .onSuccess { result ->
                    resultHolder.result   = result
                    resultHolder.serverId = serverId
                    _uiState.update { it.copy(runningPresetId = null, presetError = null) }
                    _events.send(ConsoleEvent.NavigateToResult)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(runningPresetId = null, presetError = e.message ?: "Unknown error")
                    }
                }
        }
    }

    fun startAddPreset() {
        val nextOrder = _uiState.value.presets.size
        _uiState.update {
            it.copy(
                editingPreset = Preset(
                    id = 0,
                    category = "",
                    label = "",
                    command = "",
                    source = PresetSource.CUSTOM,
                    sortOrder = nextOrder
                )
            )
        }
    }

    fun startEditPreset(preset: Preset) {
        _uiState.update { it.copy(editingPreset = preset) }
    }

    fun dismissPresetDialog() {
        _uiState.update { it.copy(editingPreset = null) }
    }

    fun savePreset(category: String, label: String, command: String) {
        val editing = _uiState.value.editingPreset ?: return
        viewModelScope.launch {
            addCustomPreset(
                editing.copy(
                    category = category.trim(),
                    label    = label.trim(),
                    command  = command.trim(),
                    source   = PresetSource.CUSTOM
                )
            )
            _uiState.update { it.copy(editingPreset = null) }
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch { deleteCustomPreset(id) }
    }
}
