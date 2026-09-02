package com.tivanstudio.servera.data.repository

import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.db.dao.PresetDao
import com.tivanstudio.servera.data.db.dao.PresetGroupDao
import com.tivanstudio.servera.data.db.entity.PresetGroupEntity
import com.tivanstudio.servera.data.mapper.toDomain
import com.tivanstudio.servera.data.mapper.toEntity
import com.tivanstudio.servera.data.remote.PresetCatalogRemote
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.domain.entity.PresetSource
import com.tivanstudio.servera.domain.repository.PresetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PresetRepositoryImpl @Inject constructor(
    private val dao: PresetDao,
    private val groupDao: PresetGroupDao,
    private val remote: PresetCatalogRemote,
    private val encryption: EncryptionHelper
) : PresetRepository {

    /** Re-reads the activated catalog on every refresh so a manual update reaches the UI. */
    private val builtinGroups: Flow<List<PresetGroup>> =
        remote.catalogRevision.map { remote.getBuiltinGroups() }

    private val builtinPresets: Flow<List<Preset>> =
        remote.catalogRevision.map { remote.getBuiltinPresets() }

    override fun getGroups(): Flow<List<PresetGroup>> = combine(
        builtinGroups,
        groupDao.getAll().map { groups -> groups.map { it.toDomain() } }
    ) { builtin, custom ->
        // Built-in groups lead, the user's own follow; each block keeps its own sortOrder.
        builtin.sortedBy { it.sortOrder } + custom.sortedBy { it.sortOrder }
    }

    override suspend fun addGroup(group: PresetGroup): Long =
        groupDao.insert(group.toEntity())

    override suspend fun updateGroup(group: PresetGroup) =
        groupDao.update(group.toEntity())

    override suspend fun deleteGroup(id: Long) =
        groupDao.deleteById(id)

    override fun getPresets(): Flow<List<Preset>> = combine(
        builtinPresets,
        dao.getAll().map { presets -> presets.map { it.toDomain(encryption) } }
    ) { builtin, custom ->
        (builtin + custom).sortedWith(compareBy({ it.groupId }, { it.sortOrder }))
    }

    override suspend fun addPreset(preset: Preset): Long =
        dao.insert(preset.toEntity(encryption))

    override suspend fun deletePreset(id: Long) =
        dao.deleteById(id)

    override suspend fun updatePresets(): Result<Int> = remote.fetchAndActivate()

    override suspend fun copyBuiltinToCustom(preset: Preset): Long {
        val origin = remote.getBuiltinGroups().firstOrNull { it.id == preset.groupId }
        val customGroups = groupDao.getAll().first()

        // Reuse the user's group of the same name if they already have one, so repeated copies
        // from one built-in group do not pile up duplicates.
        val targetId = customGroups.firstOrNull { it.name.equals(origin?.name ?: FALLBACK_GROUP_NAME, ignoreCase = true) }?.id
            ?: groupDao.insert(
                PresetGroupEntity(
                    name = origin?.name ?: FALLBACK_GROUP_NAME,
                    colorHex = origin?.colorHex ?: FALLBACK_GROUP_COLOR,
                    sortOrder = customGroups.size
                )
            )

        val sortOrder = dao.getAllOnce().count { it.groupId == targetId }
        // id = 0 so Room assigns a fresh one: the copy is fully independent of the built-in.
        return dao.insert(
            Preset(
                id = 0,
                groupId = targetId,
                label = preset.label,
                command = preset.command,
                sortOrder = sortOrder,
                source = PresetSource.CUSTOM
            ).toEntity(encryption)
        )
    }

    private companion object {
        const val FALLBACK_GROUP_NAME = "My presets"
        const val FALLBACK_GROUP_COLOR = "#455A64"
    }
}
