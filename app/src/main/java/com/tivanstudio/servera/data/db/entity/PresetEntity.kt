package com.tivanstudio.servera.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "presets",
    foreignKeys = [
        ForeignKey(
            entity        = PresetGroupEntity::class,
            parentColumns = ["id"],
            childColumns  = ["groupId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val label: String,
    val encryptedCommand: String,
    val sortOrder: Int
)
