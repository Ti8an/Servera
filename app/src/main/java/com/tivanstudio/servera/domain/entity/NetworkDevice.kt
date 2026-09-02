package com.tivanstudio.servera.domain.entity

data class NetworkDevice(
    val ip: String,
    val hostname: String? = null,
    val mac: String? = null,
    val isSelf: Boolean = false,
    val isGateway: Boolean = false
)
