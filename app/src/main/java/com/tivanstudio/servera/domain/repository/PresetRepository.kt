package com.tivanstudio.servera.domain.repository

import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup
import kotlinx.coroutines.flow.Flow

interface PresetRepository {
    fun getGroups(): Flow<List<PresetGroup>>
    suspend fun addGroup(group: PresetGroup): Long
    suspend fun updateGroup(group: PresetGroup)
    suspend fun deleteGroup(id: Long)

    fun getPresets(): Flow<List<Preset>>
    suspend fun addPreset(preset: Preset): Long
    suspend fun deletePreset(id: Long)

    suspend fun updatePresets(): Result<Int>
}
