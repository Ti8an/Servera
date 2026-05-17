package com.tivanstudio.servera.presentation.servers.add.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.repository.ServerRepository
import com.tivanstudio.servera.domain.usecase.server.AddServerUseCase
import com.tivanstudio.servera.domain.usecase.server.UpdateServerUseCase
import com.tivanstudio.servera.domain.usecase.ssh.TestConnectionUseCase
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
class AddServerViewModel @Inject constructor(
    private val addServer: AddServerUseCase,
    private val updateServer: UpdateServerUseCase,
    private val testConnection: TestConnectionUseCase,
    private val serverRepository: ServerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editId: Long = savedStateHandle.get<Long>("serverId") ?: -1L

    private val _uiState = MutableStateFlow(AddServerUiState(isEditing = editId != -1L))
    val uiState: StateFlow<AddServerUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddServerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        if (editId != -1L) loadServer(editId)
    }

    private fun loadServer(id: Long) {
        viewModelScope.launch {
            val server = serverRepository.getServerById(id) ?: return@launch
            _uiState.update {
                it.copy(
                    name       = server.name,
                    host       = server.host,
                    port       = server.port.toString(),
                    login      = server.login,
                    password   = server.password,
                    privateKey = server.privateKey ?: "",
                    timeout    = server.timeout.toString()
                )
            }
        }
    }

    fun onNameChange(v: String)       = _uiState.update { it.copy(name = v, error = null) }
    fun onHostChange(v: String)       = _uiState.update { it.copy(host = v, error = null) }
    fun onPortChange(v: String)       = _uiState.update { it.copy(port = v, error = null) }
    fun onLoginChange(v: String)      = _uiState.update { it.copy(login = v, error = null) }
    fun onPasswordChange(v: String)   = _uiState.update { it.copy(password = v, error = null) }
    fun onPrivateKeyChange(v: String) = _uiState.update { it.copy(privateKey = v) }
    fun onTimeoutChange(v: String)    = _uiState.update { it.copy(timeout = v) }
    fun onTogglePassword()            = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    fun onToggleAdvanced()            = _uiState.update { it.copy(isAdvancedExpanded = !it.isAdvancedExpanded) }

    fun save() {
        val state = _uiState.value
        val server = buildServer(state) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val outcome: Result<*> = if (state.isEditing) updateServer(server) else addServer(server)
            outcome.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                _events.send(AddServerEvent.Saved)
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun testConn() {
        val state = _uiState.value
        val server = buildServer(state) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }
            val ok = testConnection(server).getOrDefault(false)
            _uiState.update { it.copy(isTesting = false, testResult = ok) }
        }
    }

    private fun buildServer(state: AddServerUiState): Server? {
        val port = state.port.toIntOrNull()
        if (port == null || port !in 1..65535) {
            _uiState.update { it.copy(error = "Неверный порт (1–65535)") }
            return null
        }
        return Server(
            id         = if (state.isEditing) editId else 0,
            name       = state.name.trim(),
            host       = state.host.trim(),
            port       = port,
            login      = state.login.trim(),
            password   = state.password,
            privateKey = state.privateKey.takeIf { it.isNotBlank() },
            timeout    = state.timeout.toIntOrNull() ?: 30
        )
    }
}
