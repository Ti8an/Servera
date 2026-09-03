package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.db.entity.PresetEntity
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetSource

fun PresetEntity.toDomain(encryption: EncryptionHelper): Preset = Preset(
    id = id,
    groupId = groupId,
    label = label,
    command = encryption.decryptOrEmpty(encryptedCommand),
    sortOrder = sortOrder,
    // Room only ever holds the user's own presets; BUILTIN rows live in Remote Config's cache.
    source = PresetSource.CUSTOM,
    iconKey = iconKey
)

fun Preset.toEntity(encryption: EncryptionHelper): PresetEntity = PresetEntity(
    id = id,
    groupId = groupId,
    label = label,
    // The key is one of a fixed set of identifiers, not user data, so it stays in the clear.
    encryptedCommand = encryption.encrypt(command),
    sortOrder = sortOrder,
    iconKey = iconKey
)
