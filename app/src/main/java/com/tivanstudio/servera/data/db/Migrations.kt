package com.tivanstudio.servera.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE quick_commands ADD COLUMN serverId INTEGER NOT NULL DEFAULT -1")
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `presets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `category` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `command` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
