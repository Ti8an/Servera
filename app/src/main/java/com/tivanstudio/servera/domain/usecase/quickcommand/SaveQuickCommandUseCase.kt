package com.tivanstudio.servera.domain.usecase.quickcommand

import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.repository.QuickCommandRepository
import javax.inject.Inject

class SaveQuickCommandUseCase @Inject constructor(
    private val repository: QuickCommandRepository
) {
    suspend operator fun invoke(cmd: QuickCommand): Long = repository.saveQuickCommand(cmd)
}
