package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.db.entity.QuickCommandEntity
import com.tivanstudio.servera.domain.entity.QuickCommand

fun QuickCommandEntity.toDomain(): QuickCommand = QuickCommand(
    id = id,
    serverId = serverId,
    label = label,
    command = command,
    sortOrder = sortOrder,
    showOutput = showOutput,
    groupName = groupName,
    groupColorHex = groupColorHex
)

fun QuickCommand.toEntity(): QuickCommandEntity = QuickCommandEntity(
    id = id,
    serverId = serverId,
    label = label,
    command = command,
    sortOrder = sortOrder,
    showOutput = showOutput,
    groupName = groupName,
    groupColorHex = groupColorHex
)
