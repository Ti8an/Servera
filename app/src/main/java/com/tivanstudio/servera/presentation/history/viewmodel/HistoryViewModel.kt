package com.tivanstudio.servera.presentation.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.domain.usecase.history.ClearHistoryUseCase
import com.tivanstudio.servera.domain.usecase.history.GetCommandHistoryUseCase
import com.tivanstudio.servera.domain.usecase.preset.GetGroupsUseCase
import com.tivanstudio.servera.domain.usecase.server.GetServersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistory: GetCommandHistoryUseCase,
    private val getServers: GetServersUseCase,
    private val getGroups: GetGroupsUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
        observeServers()
        observeGroups()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            getHistory.getAll().collect { history ->
                _uiState.update { it.copy(allHistory = history, isLoading = false) }
            }
        }
    }

    private fun observeServers() {
        viewModelScope.launch {
            getServers().collect { servers ->
                _uiState.update { it.copy(servers = servers) }
            }
        }
    }

    private fun observeGroups() {
        viewModelScope.launch {
            getGroups().collect { groups ->
                _uiState.update { it.copy(groups = groups) }
            }
        }
    }

    fun openFilter() {
        _uiState.update { it.copy(showFilterSheet = true) }
    }

    fun closeFilter() {
        _uiState.update { it.copy(showFilterSheet = false) }
    }

    fun updateFilter(filter: HistoryFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun resetFilter() {
        _uiState.update { it.copy(filter = HistoryFilter()) }
    }

    fun clearAll() {
        viewModelScope.launch {
            _uiState.value.allHistory
                .map { it.serverId }
                .distinct()
                .forEach { serverId -> clearHistoryUseCase(serverId) }
        }
    }
}
