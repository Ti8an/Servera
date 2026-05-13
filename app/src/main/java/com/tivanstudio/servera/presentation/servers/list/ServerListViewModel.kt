package com.tivanstudio.servera.presentation.servers.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.domain.usecase.server.CheckServerStatusUseCase
import com.tivanstudio.servera.domain.usecase.server.DeleteServerUseCase
import com.tivanstudio.servera.domain.usecase.server.GetServersUseCase
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
    private val checkStatus: CheckServerStatusUseCase
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
                            isOnline = false,
                            isChecking = true
                        )
                    }
                }
                .collect { models ->
                    _uiState.update { it.copy(servers = models, isLoading = false) }
                    pingAll(models)
                }
        }
    }

    private fun pingAll(servers: List<ServerUiModel>) {
        servers.forEach { model ->
            viewModelScope.launch {
                val online = checkStatus(model.id)
                _uiState.update { state ->
                    state.copy(servers = state.servers.map {
                        if (it.id == model.id) it.copy(isOnline = online, isChecking = false) else it
                    })
                }
            }
        }
    }

    fun deleteServer(id: Long) = viewModelScope.launch { deleteServer.invoke(id) }

    fun onSearch(q: String) = _uiState.update { it.copy(searchQuery = q) }

    fun refreshStatus() {
        pingAll(_uiState.value.servers)
    }
}
