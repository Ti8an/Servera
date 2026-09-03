package com.tivanstudio.servera.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_commands")
data class QuickCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: Long,
    val label: String,
    val encryptedCommand: String,
    val sortOrder: Int,
    val showOutput: Boolean = true,
    val groupName: String? = null,
    val groupColorHex: String? = null,
    val iconKey: String? = null
)
