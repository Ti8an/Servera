package com.tivanstudio.servera.domain.repository

import com.tivanstudio.servera.domain.entity.Server
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun getServers(): Flow<List<Server>>
    suspend fun getServerById(id: Long): Server?
    suspend fun addServer(server: Server): Long
    suspend fun updateServer(server: Server)
    suspend fun deleteServer(id: Long)
}
