package com.tivanstudio.servera.presentation.console.execute.viewmodel

import com.tivanstudio.servera.domain.entity.CommandResult

data class ExecuteCommandUiState(
    val command: String = "",
    val isExecuting: Boolean = false,
    /** String resource of the last failure; null when there was none. */
    val errorRes: Int? = null
)

sealed class ExecuteCommandEvent {
    data class NavigateToResult(val result: CommandResult) : ExecuteCommandEvent()
    data class ShowError(val msg: String) : ExecuteCommandEvent()
}
