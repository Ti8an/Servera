package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.db.entity.PresetEntity
import com.tivanstudio.servera.domain.entity.Preset

fun PresetEntity.toDomain(encryption: EncryptionHelper): Preset = Preset(
    id = id,
    groupId = groupId,
    label = label,
    command = encryption.decryptOrEmpty(encryptedCommand),
    sortOrder = sortOrder
)

fun Preset.toEntity(encryption: EncryptionHelper): PresetEntity = PresetEntity(
    id = id,
    groupId = groupId,
    label = label,
    encryptedCommand = encryption.encrypt(command),
    sortOrder = sortOrder
)
