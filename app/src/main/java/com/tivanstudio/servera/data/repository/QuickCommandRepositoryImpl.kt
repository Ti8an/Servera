package com.tivanstudio.servera.data.repository

import com.tivanstudio.servera.data.db.dao.QuickCommandDao
import com.tivanstudio.servera.data.mapper.toDomain
import com.tivanstudio.servera.data.mapper.toEntity
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.repository.QuickCommandRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuickCommandRepositoryImpl @Inject constructor(
    private val dao: QuickCommandDao
) : QuickCommandRepository {

    override fun getQuickCommands(): Flow<List<QuickCommand>> =
        dao.getAllQuickCommands().map { list -> list.map { it.toDomain() } }

    override suspend fun saveQuickCommand(cmd: QuickCommand) =
        dao.insert(cmd.toEntity())

    override suspend fun deleteQuickCommand(id: Long) =
        dao.deleteById(id)
}
