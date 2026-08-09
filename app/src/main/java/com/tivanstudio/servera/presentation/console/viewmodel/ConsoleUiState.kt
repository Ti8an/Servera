package com.tivanstudio.servera.presentation.console.viewmodel

import com.tivanstudio.servera.domain.entity.CommandHistory
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo

data class ConsoleUiState(
    val server: Server? = null,
    val isLoading: Boolean = true,
    val selectedTab: Int = 0,
    val presets: List<Preset> = emptyList(),
    val groups: List<PresetGroup> = emptyList(),
    val attachedCommands: List<QuickCommand> = emptyList(),
    val showCommandDialog: Boolean = false,
    /** Attached command the open dialog edits; null means it is adding a new one. */
    val editingCommand: QuickCommand? = null,
    val runStates: Map<Long, CommandRunState> = emptyMap(),
    val showHistory: Boolean = false,
    val recentHistory: List<CommandHistory> = emptyList(),
    val serverInfo: ServerInfo? = null,
    val isLoadingServerInfo: Boolean = false,
    /** String resource describing why the last info fetch failed; null when there was none. */
    val serverInfoErrorRes: Int? = null,
    val error: String? = null
) {
    /** Catalog suggestions per group, in group order; groups without presets are dropped. */
    val grouped: List<Pair<PresetGroup, List<Preset>>>
        get() = groups
            .sortedBy { it.sortOrder }
            .map { group ->
                group to presets
                    .filter { it.groupId == group.id }
                    .sortedBy { it.sortOrder }
            }
            .filter { (_, presets) -> presets.isNotEmpty() }

    val attachedCommandStrings: Set<String> get() = attachedCommands.map { it.command }.toSet()
}

sealed interface CommandRunState {
    object Running : CommandRunState
    data class Done(val stdout: String, val stderr: String, val exitCode: Int) : CommandRunState
    data class Failure(val message: String) : CommandRunState
}

sealed class ConsoleEvent {
    data class NavigateToExecute(val serverId: Long) : ConsoleEvent()
    object NavigateToResult : ConsoleEvent()
}
