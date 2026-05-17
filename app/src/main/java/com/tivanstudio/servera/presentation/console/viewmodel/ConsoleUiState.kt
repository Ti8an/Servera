package com.tivanstudio.servera.presentation.console.viewmodel

import com.tivanstudio.servera.domain.entity.CommandHistory
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo

data class ConsoleUiState(
    val server: Server? = null,
    val isLoading: Boolean = true,
    val selectedTab: Int = 0,
    val quickCommands: List<QuickCommand> = emptyList(),
    val recentHistory: List<CommandHistory> = emptyList(),
    val serverInfo: ServerInfo? = null,
    val isLoadingServerInfo: Boolean = false,
    val serverInfoError: String? = null,
    val error: String? = null
)

sealed class ConsoleEvent {
    data class NavigateToExecute(val serverId: Long) : ConsoleEvent()
}
