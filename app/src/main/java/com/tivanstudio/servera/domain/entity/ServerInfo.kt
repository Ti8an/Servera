package com.tivanstudio.servera.domain.entity

data class ServerInfo(
    val hostname: String,
    val os: String,
    val cpuInfo: String,
    val ramTotal: String,
    val ramFree: String,
    val diskUsage: String,
    val uptime: String
)
