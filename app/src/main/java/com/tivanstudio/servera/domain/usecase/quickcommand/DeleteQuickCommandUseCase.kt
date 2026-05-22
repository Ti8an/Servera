package com.tivanstudio.servera.domain.usecase.quickcommand

import com.tivanstudio.servera.domain.repository.QuickCommandRepository
import javax.inject.Inject

class DeleteQuickCommandUseCase @Inject constructor(
    private val repository: QuickCommandRepository
) {
    suspend operator fun invoke(id: Long) = repository.deleteQuickCommand(id)
}
