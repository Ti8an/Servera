package com.tivanstudio.servera.presentation.console.result.viewmodel

import com.tivanstudio.servera.domain.entity.CommandResult

data class CommandResultUiState(
    val result: CommandResult? = null,
    val serverId: Long = -1L
)
