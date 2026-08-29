package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.db.entity.CommandHistoryEntity
import com.tivanstudio.servera.domain.entity.CommandHistory

fun CommandHistoryEntity.toDomain(encryption: EncryptionHelper): CommandHistory = CommandHistory(
    id = id,
    serverId = serverId,
    command = encryption.decryptOrEmpty(encryptedCommand),
    stdout = stdout,
    stderr = stderr,
    exitCode = exitCode,
    executedAt = executedAt,
    groupName = groupName,
    resultSaved = resultSaved
)

fun CommandHistory.toEntity(encryption: EncryptionHelper): CommandHistoryEntity =
    CommandHistoryEntity(
        id = id,
        serverId = serverId,
        encryptedCommand = encryption.encrypt(command),
        stdout = stdout,
        stderr = stderr,
        exitCode = exitCode,
        executedAt = executedAt,
        groupName = groupName,
        resultSaved = resultSaved
    )
