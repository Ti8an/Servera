package com.tivanstudio.servera.domain.entity

data class CommandHistory(
    val id: Long = 0,
    val serverId: Long,
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val executedAt: Long
)
