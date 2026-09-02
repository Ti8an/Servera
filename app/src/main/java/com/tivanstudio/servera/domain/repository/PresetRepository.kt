package com.tivanstudio.servera.domain.repository

import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup
import kotlinx.coroutines.flow.Flow

interface PresetRepository {
    /** BUILTIN groups from the Remote Config catalog, followed by the user's CUSTOM groups. */
    fun getGroups(): Flow<List<PresetGroup>>
    suspend fun addGroup(group: PresetGroup): Long
    suspend fun updateGroup(group: PresetGroup)
    suspend fun deleteGroup(id: Long)

    /** BUILTIN presets from the Remote Config catalog, merged with the user's CUSTOM presets. */
    fun getPresets(): Flow<List<Preset>>
    suspend fun addPreset(preset: Preset): Long
    suspend fun deletePreset(id: Long)

    /** Refreshes the built-in catalog. Returns the catalog version now in effect. */
    suspend fun updatePresets(): Result<Int>

    /**
     * Forks a BUILTIN preset into an independent CUSTOM one the user can edit or delete.
     * Returns the id of the new row.
     */
    suspend fun copyBuiltinToCustom(preset: Preset): Long
}
