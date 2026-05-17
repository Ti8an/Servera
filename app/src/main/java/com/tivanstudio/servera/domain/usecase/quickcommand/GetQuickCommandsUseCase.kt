package com.tivanstudio.servera.domain.usecase.quickcommand

import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.repository.QuickCommandRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetQuickCommandsUseCase @Inject constructor(
    private val repository: QuickCommandRepository
) {
    operator fun invoke(): Flow<List<QuickCommand>> = repository.getQuickCommands()
}
