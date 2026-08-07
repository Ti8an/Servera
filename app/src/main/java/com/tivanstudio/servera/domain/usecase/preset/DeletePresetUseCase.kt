package com.tivanstudio.servera.domain.usecase.preset

import com.tivanstudio.servera.domain.repository.PresetRepository
import javax.inject.Inject

class DeletePresetUseCase @Inject constructor(
    private val repository: PresetRepository
) {
    suspend operator fun invoke(id: Long) = repository.deletePreset(id)
}
