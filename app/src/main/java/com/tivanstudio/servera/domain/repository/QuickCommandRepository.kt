package com.tivanstudio.servera.domain.repository

import com.tivanstudio.servera.domain.entity.QuickCommand
import kotlinx.coroutines.flow.Flow

interface QuickCommandRepository {
    fun getQuickCommands(): Flow<List<QuickCommand>>
    suspend fun saveQuickCommand(cmd: QuickCommand)
    suspend fun deleteQuickCommand(id: Long)
}
