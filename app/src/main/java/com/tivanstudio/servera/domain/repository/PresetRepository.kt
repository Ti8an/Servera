package com.tivanstudio.servera.domain.repository

import com.tivanstudio.servera.domain.entity.Preset
import kotlinx.coroutines.flow.Flow

interface PresetRepository {
    fun getPresets(): Flow<List<Preset>>
    suspend fun addCustom(preset: Preset): Long
    suspend fun deleteCustom(id: Long)
    suspend fun updatePresets(): Result<Int>
}
