package com.tivanstudio.servera.domain.usecase.preset

import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.repository.PresetRepository
import javax.inject.Inject

class AddCustomPresetUseCase @Inject constructor(
    private val repository: PresetRepository
) {
    suspend operator fun invoke(preset: Preset): Long = repository.addCustom(preset)
}
