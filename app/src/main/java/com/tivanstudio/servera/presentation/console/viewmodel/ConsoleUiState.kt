package com.tivanstudio.servera.presentation.console.viewmodel

import com.tivanstudio.servera.domain.entity.CommandHistory
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo

data class ConsoleUiState(
    val server: Server? = null,
    val isLoading: Boolean = true,
    val selectedTab: Int = 0,
    val presets: List<Preset> = emptyList(),
    val attachedCommands: List<QuickCommand> = emptyList(),
    val showAddDialog: Boolean = false,
    val runningId: Long? = null,
    val runError: String? = null,
    val showHistory: Boolean = false,
    val recentHistory: List<CommandHistory> = emptyList(),
    val serverInfo: ServerInfo? = null,
    val isLoadingServerInfo: Boolean = false,
    val serverInfoError: String? = null,
    val error: String? = null
) {
    val groupedPresets: Map<String, List<Preset>> get() = presets.groupBy { it.category }
    val attachedCommandStrings: Set<String> get() = attachedCommands.map { it.command }.toSet()
}

sealed class ConsoleEvent {
    data class NavigateToExecute(val serverId: Long) : ConsoleEvent()
    object NavigateToResult : ConsoleEvent()
}
