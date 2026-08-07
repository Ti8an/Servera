package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.db.entity.PresetEntity
import com.tivanstudio.servera.domain.entity.Preset

fun PresetEntity.toDomain(): Preset = Preset(
    id = id,
    groupId = groupId,
    label = label,
    command = command,
    sortOrder = sortOrder
)

fun Preset.toEntity(): PresetEntity = PresetEntity(
    id = id,
    groupId = groupId,
    label = label,
    command = command,
    sortOrder = sortOrder
)
