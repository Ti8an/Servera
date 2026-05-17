package com.tivanstudio.servera.domain.repository

import com.tivanstudio.servera.domain.entity.CommandResult
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo

interface SshClient {
    suspend fun execute(server: Server, command: String): CommandResult
    suspend fun testConnection(server: Server): Boolean
    suspend fun fetchServerInfo(server: Server): ServerInfo
}
