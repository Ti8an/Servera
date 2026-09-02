package com.tivanstudio.servera.domain.usecase.preset

import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.domain.repository.PresetRepository
import javax.inject.Inject

class UpdateGroupUseCase @Inject constructor(
    private val repository: PresetRepository
) {
    suspend operator fun invoke(group: PresetGroup) = repository.updateGroup(group)
}
