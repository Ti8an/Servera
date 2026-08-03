package com.tivanstudio.servera.presentation.console.viewmodel

import com.tivanstudio.servera.domain.entity.CommandHistory
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo

data class ConsoleUiState(
    val server: Server? = null,
    val isLoading: Boolean = true,
    val selectedTab: Int = 0,
    val recentHistory: List<CommandHistory> = emptyList(),
    val serverInfo: ServerInfo? = null,
    val isLoadingServerInfo: Boolean = false,
    val serverInfoError: String? = null,
    val error: String? = null,
    val presets: List<Preset> = emptyList(),
    val editingPreset: Preset? = null,
    val runningPresetId: Long? = null,
    val presetError: String? = null
) {
    val groupedPresets: Map<String, List<Preset>> get() = presets.groupBy { it.category }
    val categories: List<String> get() = presets.map { it.category }.distinct().sorted()
}

sealed class ConsoleEvent {
    data class NavigateToExecute(val serverId: Long) : ConsoleEvent()
    object NavigateToResult : ConsoleEvent()
}
