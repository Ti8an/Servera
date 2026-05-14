package com.tivanstudio.servera.presentation.servers.list.viewmodel

data class ServerListUiState(
    val servers: List<ServerUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

data class ServerUiModel(
    val id: Long,
    val name: String,
    val host: String,
    val port: Int,
    val login: String,
    val isOnline: Boolean = false,
    val isChecking: Boolean = false
)
