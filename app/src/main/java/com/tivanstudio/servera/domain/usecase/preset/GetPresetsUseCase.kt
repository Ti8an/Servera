package com.tivanstudio.servera.domain.usecase.preset

import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.repository.PresetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPresetsUseCase @Inject constructor(
    private val repository: PresetRepository
) {
    operator fun invoke(): Flow<List<Preset>> = repository.getPresets()
}
