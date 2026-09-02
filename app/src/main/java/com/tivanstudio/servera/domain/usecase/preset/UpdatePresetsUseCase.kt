package com.tivanstudio.servera.domain.usecase.preset

import com.tivanstudio.servera.domain.repository.PresetRepository
import javax.inject.Inject

class UpdatePresetsUseCase @Inject constructor(
    private val repository: PresetRepository
) {
    /** Refreshes the built-in catalog. Returns the catalog version now in effect. */
    suspend operator fun invoke(): Result<Int> = repository.updatePresets()
}
