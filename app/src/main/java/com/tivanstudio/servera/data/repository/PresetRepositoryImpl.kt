package com.tivanstudio.servera.data.repository

import com.tivanstudio.servera.data.db.dao.PresetDao
import com.tivanstudio.servera.data.db.dao.PresetGroupDao
import com.tivanstudio.servera.data.mapper.toDomain
import com.tivanstudio.servera.data.mapper.toEntity
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.domain.repository.PresetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PresetRepositoryImpl @Inject constructor(
    private val dao: PresetDao,
    private val groupDao: PresetGroupDao
) : PresetRepository {

    override fun getGroups(): Flow<List<PresetGroup>> =
        groupDao.getAll().map { groups -> groups.map { it.toDomain() } }

    override suspend fun addGroup(group: PresetGroup): Long =
        groupDao.insert(group.toEntity())

    override suspend fun updateGroup(group: PresetGroup) =
        groupDao.update(group.toEntity())

    override suspend fun deleteGroup(id: Long) =
        groupDao.deleteById(id)

    override fun getPresets(): Flow<List<Preset>> =
        dao.getAll().map { presets -> presets.map { it.toDomain() } }

    override suspend fun addPreset(preset: Preset): Long =
        dao.insert(preset.toEntity())

    override suspend fun deletePreset(id: Long) =
        dao.deleteById(id)

    // TODO Firebase Remote Config
    override suspend fun updatePresets(): Result<Int> = Result.success(0)
}
