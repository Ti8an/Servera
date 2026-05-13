package com.tivanstudio.servera.presentation.servers.add

data class AddServerUiState(
    val name: String = "",
    val host: String = "",
    val port: String = "22",
    val login: String = "",
    val password: String = "",
    val privateKey: String = "",
    val timeout: String = "30",
    val isPasswordVisible: Boolean = false,
    val isAdvancedExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: Boolean? = null,
    val error: String? = null,
    val isEditing: Boolean = false
)

sealed class AddServerEvent {
    object Saved : AddServerEvent()
    data class ShowError(val msg: String) : AddServerEvent()
}
