package com.tivanstudio.servera.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val login: String,
    val encryptedPassword: String,
    val encryptedPrivateKey: String?,
    val timeout: Int,
    val createdAt: Long
)
