package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.db.entity.PresetGroupEntity
import com.tivanstudio.servera.domain.entity.PresetGroup

fun PresetGroupEntity.toDomain(): PresetGroup = PresetGroup(
    id = id,
    name = name,
    colorHex = colorHex,
    sortOrder = sortOrder
)

fun PresetGroup.toEntity(): PresetGroupEntity = PresetGroupEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    sortOrder = sortOrder
)
