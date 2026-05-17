package com.tivanstudio.servera.data.repository

import com.tivanstudio.servera.data.db.dao.CommandHistoryDao
import com.tivanstudio.servera.data.mapper.toDomain
import com.tivanstudio.servera.data.mapper.toEntity
import com.tivanstudio.servera.domain.entity.CommandHistory
import com.tivanstudio.servera.domain.repository.CommandHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CommandHistoryRepositoryImpl @Inject constructor(
    private val dao: CommandHistoryDao
) : CommandHistoryRepository {

    override fun getHistoryForServer(serverId: Long): Flow<List<CommandHistory>> =
        dao.getHistoryForServer(serverId).map { list -> list.map { it.toDomain() } }

    override fun getAllHistory(): Flow<List<CommandHistory>> =
        dao.getAllHistory().map { list -> list.map { it.toDomain() } }

    override suspend fun saveHistory(history: CommandHistory) =
        dao.insert(history.toEntity())

    override suspend fun clearHistory(serverId: Long) =
        dao.clearByServer(serverId)
}
