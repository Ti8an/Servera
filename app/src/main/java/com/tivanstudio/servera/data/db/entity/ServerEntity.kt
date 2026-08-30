package com.tivanstudio.servera.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A stored server. Everything that identifies the machine or the account on it is held as
 * ciphertext under the session DEK -- reading a row without the vault unlocked yields nothing
 * but the display name, the port and the timestamps.
 */
@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val encryptedHost: String,
    val port: Int,
    val encryptedLogin: String,
    val encryptedPassword: String,
    val encryptedPrivateKey: String?,
    val timeout: Int,
    val createdAt: Long
)
