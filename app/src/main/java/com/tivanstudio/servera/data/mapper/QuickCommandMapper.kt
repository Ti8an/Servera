package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.db.entity.QuickCommandEntity
import com.tivanstudio.servera.domain.entity.QuickCommand

fun QuickCommandEntity.toDomain(encryption: EncryptionHelper): QuickCommand = QuickCommand(
    id = id,
    serverId = serverId,
    label = label,
    command = encryption.decryptOrEmpty(encryptedCommand),
    sortOrder = sortOrder,
    showOutput = showOutput,
    groupName = groupName,
    groupColorHex = groupColorHex,
    iconKey = iconKey
)

fun QuickCommand.toEntity(encryption: EncryptionHelper): QuickCommandEntity = QuickCommandEntity(
    id = id,
    serverId = serverId,
    label = label,
    // The key is one of a fixed set of identifiers, not user data, so it stays in the clear.
    encryptedCommand = encryption.encrypt(command),
    sortOrder = sortOrder,
    showOutput = showOutput,
    groupName = groupName,
    groupColorHex = groupColorHex,
    iconKey = iconKey
)
