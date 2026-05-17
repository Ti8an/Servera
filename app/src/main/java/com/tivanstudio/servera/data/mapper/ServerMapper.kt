package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.db.entity.ServerEntity
import com.tivanstudio.servera.domain.entity.Server

fun ServerEntity.toDomain(encryption: EncryptionHelper): Server = Server(
    id = id,
    name = name,
    host = host,
    port = port,
    login = login,
    password = encryption.decrypt(encryptedPassword),
    privateKey = encryptedPrivateKey?.let { encryption.decrypt(it) },
    timeout = timeout,
    createdAt = createdAt
)

fun Server.toEntity(encryption: EncryptionHelper): ServerEntity = ServerEntity(
    id = id,
    name = name,
    host = host,
    port = port,
    login = login,
    encryptedPassword = encryption.encrypt(password),
    encryptedPrivateKey = privateKey?.let { encryption.encrypt(it) },
    timeout = timeout,
    createdAt = createdAt
)
