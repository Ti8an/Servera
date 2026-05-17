package com.tivanstudio.servera.data.repository

import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.db.dao.ServerDao
import com.tivanstudio.servera.data.mapper.toDomain
import com.tivanstudio.servera.data.mapper.toEntity
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ServerRepositoryImpl @Inject constructor(
    private val dao: ServerDao,
    private val encryption: EncryptionHelper
) : ServerRepository {

    override fun getServers(): Flow<List<Server>> =
        dao.getAllServers().map { list -> list.map { it.toDomain(encryption) } }

    override suspend fun getServerById(id: Long): Server? =
        dao.getServerById(id)?.toDomain(encryption)

    override suspend fun addServer(server: Server): Long =
        dao.insert(server.toEntity(encryption))

    override suspend fun updateServer(server: Server) =
        dao.update(server.toEntity(encryption))

    override suspend fun deleteServer(id: Long) =
        dao.deleteById(id)
}
