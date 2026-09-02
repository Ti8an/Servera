package com.tivanstudio.servera.presentation.tools.network

import com.tivanstudio.servera.domain.entity.NetworkDevice

data class NetworkScanUiState(
    val isScanning: Boolean = false,
    val progress: Float = 0f,
    val scanned: Int = 0,
    val total: Int = 254,
    val devices: List<NetworkDevice> = emptyList(),
    val subnet: String? = null,
    /** String resource of the last failure; null when there was none. */
    val error: Int? = null,
    val hasScanned: Boolean = false
)
