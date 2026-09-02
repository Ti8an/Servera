package com.tivanstudio.servera.presentation.servers.list.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.di.ServerCache
import com.tivanstudio.servera.domain.analytics.Analytics
import com.tivanstudio.servera.domain.analytics.AnalyticsEvent
import com.tivanstudio.servera.domain.usecase.server.CheckServerStatusUseCase
import com.tivanstudio.servera.domain.usecase.server.DeleteServerUseCase
import com.tivanstudio.servera.domain.usecase.server.GetServersUseCase
import com.tivanstudio.servera.presentation.common.toSshErrorRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerListViewModel @Inject constructor(
    private val getServers: GetServersUseCase,
    private val deleteServer: DeleteServerUseCase,
    private val checkStatus: CheckServerStatusUseCase,
    private val serverCache: ServerCache,
    private val analytics: Analytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerListUiState())
    val uiState: StateFlow<ServerListUiState> = _uiState.asStateFlow()

    init {
        observeServers()
    }

    private fun observeServers() {
        viewModelScope.launch {
            getServers()
                .map { list ->
                    list.map { s ->
                        ServerUiModel(
                            id       = s.id,
                            name     = s.name,
                            host     = s.host,
                            port     = s.port,
                            login    = s.login,
                            isOnline = serverCache.statusOf(s.id),
                            isChecking = false,
                            isCorrupted = s.isCorrupted
                        )
                    }
                }
                .collect { models ->
                    _uiState.update { it.copy(servers = models, isLoading = false) }
                }
        }
    }

    private fun updateServer(id: Long, transform: (ServerUiModel) -> ServerUiModel) {
        _uiState.update { state ->
            state.copy(servers = state.servers.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun setChecking(id: Long, checking: Boolean) {
        updateServer(id) { it.copy(isChecking = checking) }
    }

    fun deleteServer(id: Long) = viewModelScope.launch {
        deleteServer.invoke(id)
        analytics.log(AnalyticsEvent.ServerDeleted)
    }

    fun onSearch(q: String) = _uiState.update { it.copy(searchQuery = q) }

    fun checkOne(id: Long) {
        if (_uiState.value.servers.any { it.id == id && (it.isChecking || it.isCorrupted) }) return
        viewModelScope.launch {
            setChecking(id, true)
            checkStatus(id)
                .onSuccess {
                    analytics.log(AnalyticsEvent.ServerConnectSuccess)
                    serverCache.putStatus(id, true)
                    updateServer(id) { it.copy(isOnline = true, isChecking = false) }
                }
                .onFailure { e ->
                    // The cause stays out of it: a failure reason can name the host.
                    analytics.log(AnalyticsEvent.ServerConnectFail)
                    serverCache.putStatus(id, false)
                    updateServer(id) { it.copy(isOnline = false, isChecking = false) }
                    _uiState.update {
                        it.copy(
                            statusError = StatusError(
                                serverId   = id,
                                messageRes = e.toSshErrorRes()
                            )
                        )
                    }
                }
        }
    }

    fun dismissStatusError() {
        _uiState.update { it.copy(statusError = null) }
    }
}
