package com.tivanstudio.servera.data.repository

import android.content.Context
import com.tivanstudio.servera.data.db.dao.PresetDao
import com.tivanstudio.servera.data.mapper.PresetFile
import com.tivanstudio.servera.data.mapper.toBuiltinDomain
import com.tivanstudio.servera.data.mapper.toDomain
import com.tivanstudio.servera.data.mapper.toEntity
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.repository.PresetRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

class PresetRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PresetDao
) : PresetRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val builtinFlow: Flow<List<Preset>> = flow { emit(loadBuiltinFromAssets()) }

    override fun getPresets(): Flow<List<Preset>> =
        combine(builtinFlow, dao.getAll()) { builtin, custom ->
            (builtin + custom.map { it.toDomain() })
                .sortedWith(compareBy({ it.category }, { it.sortOrder }))
        }

    override suspend fun addCustom(preset: Preset): Long =
        dao.insert(preset.toEntity())

    override suspend fun deleteCustom(id: Long) =
        dao.deleteById(id)

    // TODO Firebase Remote Config
    override suspend fun updatePresets(): Result<Int> = Result.success(0)

    private suspend fun loadBuiltinFromAssets(): List<Preset> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.assets.open(ASSET_FILE_NAME).bufferedReader().use { it.readText() }
            json.decodeFromString<PresetFile>(raw).presets.map { it.toBuiltinDomain() }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val ASSET_FILE_NAME = "presets.json"
    }
}
