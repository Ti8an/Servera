package com.tivanstudio.servera.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: Long,
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val executedAt: Long
)
