package com.tivanstudio.servera.domain.usecase.history

import com.tivanstudio.servera.domain.repository.CommandHistoryRepository
import javax.inject.Inject

class ClearHistoryUseCase @Inject constructor(
    private val repository: CommandHistoryRepository
) {
    suspend operator fun invoke(serverId: Long) = repository.clearHistory(serverId)
}
