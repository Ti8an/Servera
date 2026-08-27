package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.db.entity.ServerEntity
import com.tivanstudio.servera.domain.entity.Server

/**
 * A row whose secrets cannot be decrypted must not take the whole list down with it, so the
 * failure is confined to that server: it comes back with empty secrets and [Server.isCorrupted]
 * set, and the UI offers only to delete it.
 */
fun ServerEntity.toDomain(encryption: EncryptionHelper): Server {
    val secrets = runCatching {
        encryption.decrypt(encryptedPassword) to encryptedPrivateKey?.let { encryption.decrypt(it) }
    }.onFailure {
        // A locked vault is not corruption -- every row would fail. Let that one through.
        if (it is IllegalStateException) throw it
    }
    return Server(
        id = id,
        name = name,
        host = host,
        port = port,
        login = login,
        password = secrets.getOrNull()?.first.orEmpty(),
        privateKey = secrets.getOrNull()?.second,
        timeout = timeout,
        createdAt = createdAt,
        isCorrupted = secrets.isFailure
    )
}

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
