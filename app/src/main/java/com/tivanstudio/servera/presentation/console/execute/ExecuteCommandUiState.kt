package com.tivanstudio.servera.presentation.console.execute

import com.tivanstudio.servera.domain.entity.CommandResult

data class ExecuteCommandUiState(
    val command: String = "",
    val isExecuting: Boolean = false,
    val error: String? = null
)

sealed class ExecuteCommandEvent {
    data class NavigateToResult(val result: CommandResult) : ExecuteCommandEvent()
    data class ShowError(val msg: String) : ExecuteCommandEvent()
}
