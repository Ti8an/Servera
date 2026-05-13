package com.tivanstudio.servera.presentation.history

import com.tivanstudio.servera.domain.entity.CommandHistory

data class HistoryUiState(
    val history: List<CommandHistory> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
