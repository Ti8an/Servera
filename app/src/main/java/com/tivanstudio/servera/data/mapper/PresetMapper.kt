package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.db.entity.PresetEntity
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetSource
import kotlinx.serialization.Serializable

fun PresetEntity.toDomain(): Preset = Preset(
    id = id,
    category = category,
    label = label,
    command = command,
    source = PresetSource.CUSTOM,
    sortOrder = sortOrder
)

fun Preset.toEntity(): PresetEntity = PresetEntity(
    id = id,
    category = category,
    label = label,
    command = command,
    sortOrder = sortOrder
)

@Serializable
data class PresetFile(
    val version: Int,
    val presets: List<PresetDto>
)

@Serializable
data class PresetDto(
    val category: String,
    val label: String,
    val command: String,
    val sortOrder: Int
)

fun PresetDto.toBuiltinDomain(): Preset = Preset(
    category = category,
    label = label,
    command = command,
    source = PresetSource.BUILTIN,
    sortOrder = sortOrder
)
