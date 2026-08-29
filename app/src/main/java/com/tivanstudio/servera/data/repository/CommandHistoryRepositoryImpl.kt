package com.tivanstudio.servera.data.repository

import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.db.dao.CommandHistoryDao
import com.tivanstudio.servera.data.mapper.toDomain
import com.tivanstudio.servera.data.mapper.toEntity
import com.tivanstudio.servera.domain.entity.CommandHistory
import com.tivanstudio.servera.domain.repository.CommandHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CommandHistoryRepositoryImpl @Inject constructor(
    private val dao: CommandHistoryDao,
    private val encryption: EncryptionHelper
) : CommandHistoryRepository {

    override fun getHistoryForServer(serverId: Long): Flow<List<CommandHistory>> =
        dao.getHistoryForServer(serverId).map { list -> list.map { it.toDomain(encryption) } }

    override fun getAllHistory(): Flow<List<CommandHistory>> =
        dao.getAllHistory().map { list -> list.map { it.toDomain(encryption) } }

    override suspend fun saveHistory(history: CommandHistory) =
        dao.insert(history.toEntity(encryption))

    override suspend fun clearHistory(serverId: Long) =
        dao.clearByServer(serverId)
}
