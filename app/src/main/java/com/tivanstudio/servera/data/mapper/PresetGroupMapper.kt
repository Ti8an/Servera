package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.db.entity.PresetGroupEntity
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.domain.entity.PresetSource

fun PresetGroupEntity.toDomain(): PresetGroup = PresetGroup(
    id = id,
    name = name,
    colorHex = colorHex,
    sortOrder = sortOrder,
    // Room only ever holds the user's own groups; BUILTIN rows live in Remote Config's cache.
    source = PresetSource.CUSTOM
)

fun PresetGroup.toEntity(): PresetGroupEntity = PresetGroupEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    sortOrder = sortOrder
)
