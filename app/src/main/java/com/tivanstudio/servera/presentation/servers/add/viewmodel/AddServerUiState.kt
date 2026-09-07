package com.tivanstudio.servera.presentation.servers.add.viewmodel

import com.tivanstudio.servera.domain.entity.ScannedCredentials

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
    val isEditing: Boolean = false,
    val isScannerVisible: Boolean = false,
    /** Set after a scan so the form can ask the user to double-check the values. */
    val showScanReviewHint: Boolean = false,
    /** Last non-empty parse of the current scanning session; reset when it opens. */
    val lastScanResult: ScannedCredentials? = null
) {
    val isHostValid: Boolean get() = hostErrorRes == null
}

sealed class AddServerEvent {
    object Saved : AddServerEvent()
    data class ShowError(val msg: String) : AddServerEvent()
}
