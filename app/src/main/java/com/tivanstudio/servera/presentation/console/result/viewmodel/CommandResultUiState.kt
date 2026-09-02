package com.tivanstudio.servera.presentation.console.result.viewmodel

import com.tivanstudio.servera.domain.entity.CommandResult

data class CommandResultUiState(
    val result: CommandResult? = null,
    val serverId: Long = -1L,
    val serverName: String? = null,
    val serverHost: String? = null,
    val groupName: String? = null,
    val command: String? = null,
    val exitCode: Int? = null,
    /** False when the run was recorded without its output; the output area shows "no data". */
    val outputSaved: Boolean = true
)
