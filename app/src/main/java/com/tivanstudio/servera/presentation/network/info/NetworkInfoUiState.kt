package com.tivanstudio.servera.presentation.network.info

import com.tivanstudio.servera.data.network.NetworkScanner

data class NetworkInfoUiState(
    val details: NetworkScanner.NetworkDetails = NetworkScanner.NetworkDetails(
        transport    = NetworkScanner.Transport.NONE,
        isVpnActive  = false,
        localIp      = null,
        subnetBase   = null,
        prefixLength = null,
        gatewayIp    = null,
        dnsServers   = emptyList()
    )
) {
    /** "192.168.1.0/24"; null unless both halves are known. */
    val subnet: String?
        get() {
            val base   = details.subnetBase ?: return null
            val prefix = details.prefixLength ?: return null
            return "${base}0/$prefix"
        }

    val hasConnection: Boolean get() = details.transport != NetworkScanner.Transport.NONE
}
