package com.tivanstudio.servera.domain.repository

import com.tivanstudio.servera.domain.entity.CommandHistory
import kotlinx.coroutines.flow.Flow

interface CommandHistoryRepository {
    fun getHistoryForServer(serverId: Long): Flow<List<CommandHistory>>
    fun getAllHistory(): Flow<List<CommandHistory>>
    suspend fun saveHistory(history: CommandHistory)
    suspend fun clearHistory(serverId: Long)
}
