package com.tivanstudio.servera.domain.usecase.history

import com.tivanstudio.servera.domain.entity.CommandHistory
import com.tivanstudio.servera.domain.repository.CommandHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCommandHistoryUseCase @Inject constructor(
    private val repository: CommandHistoryRepository
) {
    fun forServer(serverId: Long): Flow<List<CommandHistory>> =
        repository.getHistoryForServer(serverId)

    fun getAll(): Flow<List<CommandHistory>> = repository.getAllHistory()
}
