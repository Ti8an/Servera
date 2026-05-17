package com.tivanstudio.servera.presentation.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.domain.usecase.history.ClearHistoryUseCase
import com.tivanstudio.servera.domain.usecase.history.GetCommandHistoryUseCase
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
    private val clearHistoryUseCase: ClearHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            getHistory.getAll().collect { history ->
                _uiState.update { it.copy(history = history, isLoading = false) }
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            _uiState.value.history
                .map { it.serverId }
                .distinct()
                .forEach { serverId -> clearHistoryUseCase(serverId) }
        }
    }
}
