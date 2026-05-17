package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.db.entity.CommandHistoryEntity
import com.tivanstudio.servera.domain.entity.CommandHistory

fun CommandHistoryEntity.toDomain(): CommandHistory = CommandHistory(
    id = id,
    serverId = serverId,
    command = command,
    stdout = stdout,
    stderr = stderr,
    exitCode = exitCode,
    executedAt = executedAt
)

fun CommandHistory.toEntity(): CommandHistoryEntity = CommandHistoryEntity(
    id = id,
    serverId = serverId,
    command = command,
    stdout = stdout,
    stderr = stderr,
    exitCode = exitCode,
    executedAt = executedAt
)
