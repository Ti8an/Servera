package com.tivanstudio.servera.presentation.servers.add.viewmodel

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
    /** Error shown under the host field; null while the host is acceptable. */
    val hostErrorRes: Int? = null,
    val isEditing: Boolean = false
) {
    val isHostValid: Boolean get() = hostErrorRes == null
}

sealed class AddServerEvent {
    object Saved : AddServerEvent()
    data class ShowError(val msg: String) : AddServerEvent()
}
