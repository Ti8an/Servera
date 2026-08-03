package com.tivanstudio.servera.domain.usecase.preset

import com.tivanstudio.servera.domain.repository.PresetRepository
import javax.inject.Inject

class DeleteCustomPresetUseCase @Inject constructor(
    private val repository: PresetRepository
) {
    suspend operator fun invoke(id: Long) = repository.deleteCustom(id)
}
