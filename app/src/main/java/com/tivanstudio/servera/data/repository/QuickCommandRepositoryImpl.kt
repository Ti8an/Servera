package com.tivanstudio.servera.data.repository

import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.db.dao.QuickCommandDao
import com.tivanstudio.servera.data.mapper.toDomain
import com.tivanstudio.servera.data.mapper.toEntity
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.repository.QuickCommandRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuickCommandRepositoryImpl @Inject constructor(
    private val dao: QuickCommandDao,
    private val encryption: EncryptionHelper
) : QuickCommandRepository {

    override fun getQuickCommands(serverId: Long): Flow<List<QuickCommand>> =
        dao.getForServer(serverId).map { list -> list.map { it.toDomain(encryption) } }

    override suspend fun saveQuickCommand(cmd: QuickCommand) =
        dao.insert(cmd.toEntity(encryption))

    override suspend fun deleteQuickCommand(id: Long) =
        dao.deleteById(id)
}
