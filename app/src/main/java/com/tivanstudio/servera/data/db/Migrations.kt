package com.tivanstudio.servera.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE quick_commands ADD COLUMN serverId INTEGER NOT NULL DEFAULT -1")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE quick_commands ADD COLUMN showOutput INTEGER NOT NULL DEFAULT 1")
    }
}

/**
 * Preset categories (free-form strings) become first-class groups with a colour.
 * Existing presets keep their data: every distinct category turns into a group
 * and the presets are re-pointed at it via groupId.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `preset_groups` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `preset_groups` (`name`, `colorHex`, `sortOrder`)
            SELECT DISTINCT `category`, '$DEFAULT_GROUP_COLOR', 0 FROM `presets`
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `presets_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `groupId` INTEGER NOT NULL,
                `label` TEXT NOT NULL,
                `command` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                FOREIGN KEY(`groupId`) REFERENCES `preset_groups`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `presets_new` (`id`, `groupId`, `label`, `command`, `sortOrder`)
            SELECT p.`id`, g.`id`, p.`label`, p.`command`, p.`sortOrder`
            FROM `presets` p JOIN `preset_groups` g ON g.`name` = p.`category`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `presets`")
        db.execSQL("ALTER TABLE `presets_new` RENAME TO `presets`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_presets_groupId` ON `presets` (`groupId`)")
    }
}

/** Attached commands carry a snapshot of their catalog group (name + colour). */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE quick_commands ADD COLUMN groupName TEXT")
        db.execSQL("ALTER TABLE quick_commands ADD COLUMN groupColorHex TEXT")
    }
}

/** History keeps a snapshot of the group it ran under, so it can be filtered by group. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE command_history ADD COLUMN groupName TEXT")
    }
}

private const val DEFAULT_GROUP_COLOR = "#4CAF50"

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
