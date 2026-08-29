package com.tivanstudio.servera.data.mapper

import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.db.entity.ServerEntity
import com.tivanstudio.servera.domain.entity.Server

/** Everything a row keeps under the DEK, decrypted in one go. */
private class Plaintext(
    val host: String,
    val login: String,
    val password: String,
    val privateKey: String?
)

/**
 * A row whose encrypted fields cannot be decrypted must not take the whole list down with it, so
 * the failure is confined to that server: it comes back with those fields empty and
 * [Server.isCorrupted] set, and the UI offers only to delete it.
 *
 * Host and login are decrypted in the same attempt as the secrets on purpose. Splitting them
 * would let a locked vault surface as a half-read row instead of the [IllegalStateException]
 * the caller is waiting for.
 */
fun ServerEntity.toDomain(encryption: EncryptionHelper): Server {
    val decrypted = runCatching {
        Plaintext(
            host = encryption.decrypt(encryptedHost),
            login = encryption.decrypt(encryptedLogin),
            password = encryption.decrypt(encryptedPassword),
            privateKey = encryptedPrivateKey?.let { encryption.decrypt(it) }
        )
    }.onFailure {
        // A locked vault is not corruption -- every row would fail. Let that one through.
        if (it is IllegalStateException) throw it
    }
    val plain = decrypted.getOrNull()
    return Server(
        id = id,
        name = name,
        host = plain?.host.orEmpty(),
        port = port,
        login = plain?.login.orEmpty(),
        password = plain?.password.orEmpty(),
        privateKey = plain?.privateKey,
        timeout = timeout,
        createdAt = createdAt,
        isCorrupted = decrypted.isFailure
    )
}

fun Server.toEntity(encryption: EncryptionHelper): ServerEntity = ServerEntity(
    id = id,
    name = name,
    encryptedHost = encryption.encrypt(host),
    port = port,
    encryptedLogin = encryption.encrypt(login),
    encryptedPassword = encryption.encrypt(password),
    encryptedPrivateKey = privateKey?.let { encryption.encrypt(it) },
    timeout = timeout,
    createdAt = createdAt
)
