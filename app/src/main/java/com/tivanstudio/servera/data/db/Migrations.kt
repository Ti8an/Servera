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

/** History records whether the command output was kept alongside the run. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE command_history ADD COLUMN resultSaved INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * `host` and `login` become `encryptedHost` / `encryptedLogin`: from here on they are stored
 * as ciphertext like the password and the private key already were.
 *
 * Only the columns are renamed -- the values stay plaintext until MigrationManager's legacy
 * pass encrypts them under the DEK, which happens on the first unlock after the upgrade.
 *
 * `ALTER TABLE ... RENAME COLUMN` needs SQLite 3.25, which is API 29; minSdk here is 26,
 * so the table is rebuilt instead.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `servers_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `encryptedHost` TEXT NOT NULL,
                `port` INTEGER NOT NULL,
                `encryptedLogin` TEXT NOT NULL,
                `encryptedPassword` TEXT NOT NULL,
                `encryptedPrivateKey` TEXT,
                `timeout` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `servers_new` (
                `id`, `name`, `encryptedHost`, `port`, `encryptedLogin`,
                `encryptedPassword`, `encryptedPrivateKey`, `timeout`, `createdAt`
            )
            SELECT `id`, `name`, `host`, `port`, `login`,
                   `encryptedPassword`, `encryptedPrivateKey`, `timeout`, `createdAt`
            FROM `servers`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `servers`")
        db.execSQL("ALTER TABLE `servers_new` RENAME TO `servers`")
    }
}

/**
 * `command` becomes `encryptedCommand` in presets, attached commands and history: the text a
 * user runs against their machines is as telling as the credentials, so it stops being stored
 * in the clear. Labels, group names and captured output are deliberately left alone.
 *
 * As with the v8 -> v9 rename the values stay plaintext here; MigrationManager's pass puts them
 * under the DEK on the first unlock after the upgrade. Same reason for rebuilding rather than
 * `RENAME COLUMN`: that needs SQLite 3.25 (API 29) and minSdk is 26.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `presets_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `groupId` INTEGER NOT NULL,
                `label` TEXT NOT NULL,
                `encryptedCommand` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                FOREIGN KEY(`groupId`) REFERENCES `preset_groups`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `presets_new` (`id`, `groupId`, `label`, `encryptedCommand`, `sortOrder`)
            SELECT `id`, `groupId`, `label`, `command`, `sortOrder` FROM `presets`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `presets`")
        db.execSQL("ALTER TABLE `presets_new` RENAME TO `presets`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_presets_groupId` ON `presets` (`groupId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `quick_commands_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `serverId` INTEGER NOT NULL,
                `label` TEXT NOT NULL,
                `encryptedCommand` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `showOutput` INTEGER NOT NULL,
                `groupName` TEXT,
                `groupColorHex` TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `quick_commands_new` (
                `id`, `serverId`, `label`, `encryptedCommand`, `sortOrder`,
                `showOutput`, `groupName`, `groupColorHex`
            )
            SELECT `id`, `serverId`, `label`, `command`, `sortOrder`,
                   `showOutput`, `groupName`, `groupColorHex`
            FROM `quick_commands`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `quick_commands`")
        db.execSQL("ALTER TABLE `quick_commands_new` RENAME TO `quick_commands`")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `command_history_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `serverId` INTEGER NOT NULL,
                `encryptedCommand` TEXT NOT NULL,
                `stdout` TEXT NOT NULL,
                `stderr` TEXT NOT NULL,
                `exitCode` INTEGER NOT NULL,
                `executedAt` INTEGER NOT NULL,
                `groupName` TEXT,
                `resultSaved` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `command_history_new` (
                `id`, `serverId`, `encryptedCommand`, `stdout`, `stderr`,
                `exitCode`, `executedAt`, `groupName`, `resultSaved`
            )
            SELECT `id`, `serverId`, `command`, `stdout`, `stderr`,
                   `exitCode`, `executedAt`, `groupName`, `resultSaved`
            FROM `command_history`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `command_history`")
        db.execSQL("ALTER TABLE `command_history_new` RENAME TO `command_history`")
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
