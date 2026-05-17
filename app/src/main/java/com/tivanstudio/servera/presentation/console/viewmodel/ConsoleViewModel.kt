package com.tivanstudio.servera.presentation.console.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.domain.repository.ServerRepository
import com.tivanstudio.servera.domain.usecase.history.GetCommandHistoryUseCase
import com.tivanstudio.servera.domain.usecase.quickcommand.GetQuickCommandsUseCase
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
    private val fetchServerInfo: FetchServerInfoUseCase,
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
}
