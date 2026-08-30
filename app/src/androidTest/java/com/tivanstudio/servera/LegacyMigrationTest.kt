package com.tivanstudio.servera

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.crypto.KeystoreManager
import com.tivanstudio.servera.data.crypto.MigrationManager
import com.tivanstudio.servera.data.crypto.PasswordKeyManager
import com.tivanstudio.servera.data.db.AppDatabase
import com.tivanstudio.servera.data.db.entity.CommandHistoryEntity
import com.tivanstudio.servera.data.db.entity.PresetEntity
import com.tivanstudio.servera.data.db.entity.PresetGroupEntity
import com.tivanstudio.servera.data.db.entity.QuickCommandEntity
import com.tivanstudio.servera.data.db.entity.ServerEntity
import com.tivanstudio.servera.data.mapper.toDomain
import com.tivanstudio.servera.data.mapper.toEntity
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.di.CryptoModule
import com.tivanstudio.servera.di.SessionKeyHolder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.crypto.Cipher

/**
 * Covers the upgrade path on a real device: rows written by the pre-V2 build (one Android
 * Keystore key) have to come out readable under the password-derived DEK once the user sets
 * a password for the first time in the new scheme.
 *
 * The fixture is seeded here with the real Keystore key rather than read from the app's own
 * database, so the test is self-contained and never touches the user's data.
 */
@RunWith(AndroidJUnit4::class)
class LegacyMigrationTest {

    private lateinit var context: Context
    private lateinit var session: SessionKeyHolder
    private lateinit var keystoreManager: KeystoreManager
    private lateinit var encryption: EncryptionHelper
    private lateinit var passwordKeyManager: PasswordKeyManager
    private lateinit var db: AppDatabase
    private lateinit var migrationManager: MigrationManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        session = SessionKeyHolder()
        keystoreManager = KeystoreManager()
        encryption = EncryptionHelper(keystoreManager, session)
        // A throwaway store: the production "auth_prefs" is never opened by this test.
        context.deleteSharedPreferences(TEST_PREFS)
        passwordKeyManager = PasswordKeyManager(encryptedPrefs(TEST_PREFS))

        context.deleteDatabase(TEST_DB)
        db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB).build()
        migrationManager = MigrationManager(
            db, db.serverDao(), db.presetDao(), db.quickCommandDao(),
            db.commandHistoryDao(), encryption, passwordKeyManager, session
        )
    }

    @After
    fun tearDown() {
        // Guarded so a failure in setUp surfaces its own cause instead of a lateinit error.
        if (::db.isInitialized) db.close()
        if (::session.isInitialized) session.clear()
        if (::passwordKeyManager.isInitialized) passwordKeyManager.clearCrypto()
        // clearCrypto writes with apply(); a synchronous commit on the same file flushes that
        // queue, otherwise the pending write recreates the file right after we delete it.
        context.getSharedPreferences(TEST_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        context.deleteSharedPreferences(TEST_PREFS)
        testPrefsFile().delete()
        context.deleteDatabase(TEST_DB)
    }

    /** Exactly what the pre-V2 [EncryptionHelper.encrypt] wrote: Base64(iv[12] + ciphertext). */
    private fun legacyEncrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreManager.getOrCreateKey())
        return Base64.encodeToString(
            cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
    }

    /**
     * A pre-V2 row as the v8 -> v9 schema migration leaves it: secrets under the legacy Keystore
     * key, host and login still the plaintext the old schema stored in those columns.
     */
    private fun legacyServer(
        name: String,
        password: String,
        privateKey: String?,
        host: String = LEGACY_HOST,
        login: String = LEGACY_LOGIN
    ) = ServerEntity(
        name = name,
        encryptedHost = host,
        port = 22,
        encryptedLogin = login,
        encryptedPassword = legacyEncrypt(password),
        encryptedPrivateKey = privateKey?.let { legacyEncrypt(it) },
        timeout = 30,
        createdAt = 1_700_000_000_000
    )

    @Test
    fun legacyRowsAreReEncryptedUnderTheNewDek() = runBlocking {
        val dao = db.serverDao()
        val plainPassword = "hunter2-äöü-пароль"
        val plainKey = "-----BEGIN OPENSSH PRIVATE KEY-----\nabc\n-----END-----"

        val withKeyId = dao.insert(legacyServer("with key", plainPassword, plainKey))
        val noKeyId = dao.insert(legacyServer("no key", plainPassword, null))
        // A row the legacy key cannot read: it must be skipped, not blow the pass up.
        val corruptId = dao.insert(
            legacyServer("corrupt", plainPassword, null)
                .copy(encryptedPassword = Base64.encodeToString(ByteArray(42), Base64.NO_WRAP))
        )
        val before = dao.getAllServersOnce().associateBy { it.id }

        assertFalse(passwordKeyManager.isInitialized())
        assertFalse(passwordKeyManager.isLegacyMigrated())

        // The user sets a password for the first time under the new scheme.
        session.dek = passwordKeyManager.initialize("upgrade-test-pw".toCharArray())
        migrationManager.migrateIfNeeded()

        assertTrue("Migration did not record that it ran", passwordKeyManager.isLegacyMigrated())

        val after = dao.getAllServersOnce().associateBy { it.id }
        assertEquals("Migration must not add or drop rows", before.keys, after.keys)

        listOf(withKeyId, noKeyId).forEach { id ->
            val row = after.getValue(id)
            assertNotEquals(
                "Server $id was left encrypted with the legacy key",
                before.getValue(id).encryptedPassword,
                row.encryptedPassword
            )
            assertEquals(
                "Password of server $id did not survive the migration",
                plainPassword,
                encryption.decrypt(row.encryptedPassword)
            )
            assertNotEquals(
                "Host of server $id was left in plaintext",
                LEGACY_HOST,
                row.encryptedHost
            )
            assertNotEquals(
                "Login of server $id was left in plaintext",
                LEGACY_LOGIN,
                row.encryptedLogin
            )

            // The path the app actually uses when it loads a server.
            val server = row.toDomain(encryption)
            assertFalse("Server $id must not read back as corrupted", server.isCorrupted)
            assertEquals(plainPassword, server.password)
            assertEquals(LEGACY_HOST, server.host)
            assertEquals(LEGACY_LOGIN, server.login)
            assertEquals(before.getValue(id).name, server.name)
        }

        assertEquals(plainKey, after.getValue(withKeyId).encryptedPrivateKey?.let(encryption::decrypt))
        assertNull("A row without a key must not gain one", after.getValue(noKeyId).encryptedPrivateKey)

        assertEquals(
            "An unreadable row must be left untouched, not corrupted further",
            before.getValue(corruptId),
            after.getValue(corruptId)
        )
    }

    /**
     * The v8 -> v9 columns arrive holding plaintext. The pass has to encrypt them as they stand
     * -- and a later login must not run over them a second time, which would encrypt the
     * ciphertext and lose the row.
     */
    @Test
    fun plaintextHostAndLoginAreEncryptedOnceAndStayReadable() = runBlocking {
        val dao = db.serverDao()
        val host = "srv-01.internal.example.com"
        val login = "deploy-üser"
        val id = dao.insert(legacyServer("plain", "pw", null, host = host, login = login))

        session.dek = passwordKeyManager.initialize("upgrade-test-pw".toCharArray())
        migrationManager.migrateIfNeeded()

        val migrated = dao.getServerById(id)!!
        assertNotEquals("Host must not stay in plaintext", host, migrated.encryptedHost)
        assertNotEquals("Login must not stay in plaintext", login, migrated.encryptedLogin)
        assertEquals(host, encryption.decrypt(migrated.encryptedHost))
        assertEquals(login, encryption.decrypt(migrated.encryptedLogin))

        // Logging in again: the flag has to keep the pass off already-migrated ciphertext.
        session.clear()
        session.dek = passwordKeyManager.unlock("upgrade-test-pw".toCharArray())
        migrationManager.migrateIfNeeded()

        assertEquals("A second login must leave the row alone", migrated, dao.getServerById(id))
        val server = dao.getServerById(id)!!.toDomain(encryption)
        assertFalse(server.isCorrupted)
        assertEquals(host, server.host)
        assertEquals(login, server.login)
    }

    /**
     * Presets, attached commands and history arrive from the v9 -> v10 rename holding plaintext
     * commands. All three tables go through the same pass, and all three have to read back.
     */
    @Test
    fun plaintextCommandsAreEncryptedAcrossAllThreeTables() = runBlocking {
        val serverId = db.serverDao().insert(legacyServer("box", "pw", null))
        val groupId = db.presetGroupDao().insert(PresetGroupEntity(name = "Disk", colorHex = "#4CAF50", sortOrder = 0))
        val presetCommand = "df -h --output=source,pcent"
        val attachedCommand = "systemctl restart nginx"
        val historyCommand = "grep -R 'пароль' /etc"

        val presetId = db.presetDao().insert(
            PresetEntity(groupId = groupId, label = "Disk usage", encryptedCommand = presetCommand, sortOrder = 0)
        )
        val attachedId = db.quickCommandDao().insert(
            QuickCommandEntity(
                serverId = serverId,
                label = "Restart nginx",
                encryptedCommand = attachedCommand,
                sortOrder = 0
            )
        )
        db.commandHistoryDao().insert(
            CommandHistoryEntity(
                serverId = serverId,
                encryptedCommand = historyCommand,
                stdout = "",
                stderr = "",
                exitCode = 0,
                executedAt = 1_700_000_000_000
            )
        )

        session.dek = passwordKeyManager.initialize("upgrade-test-pw".toCharArray())
        migrationManager.migrateIfNeeded()

        val preset = db.presetDao().getAllOnce().single { it.id == presetId }
        val attached = db.quickCommandDao().getAllOnce().single { it.id == attachedId }
        val history = db.commandHistoryDao().getAllOnce().single()

        assertNotEquals("Preset command left in plaintext", presetCommand, preset.encryptedCommand)
        assertNotEquals("Attached command left in plaintext", attachedCommand, attached.encryptedCommand)
        assertNotEquals("History command left in plaintext", historyCommand, history.encryptedCommand)

        // The paths the app actually loads through.
        assertEquals(presetCommand, preset.toDomain(encryption).command)
        assertEquals(attachedCommand, attached.toDomain(encryption).command)
        assertEquals(historyCommand, history.toDomain(encryption).command)

        // Labels and group snapshots were never secret and must come through untouched.
        assertEquals("Disk usage", preset.toDomain(encryption).label)
        assertEquals("Restart nginx", attached.toDomain(encryption).label)

        // A second login must not run over ciphertext, which would encrypt it again.
        session.clear()
        session.dek = passwordKeyManager.unlock("upgrade-test-pw".toCharArray())
        migrationManager.migrateIfNeeded()

        assertEquals(preset, db.presetDao().getAllOnce().single { it.id == presetId })
        assertEquals(attached, db.quickCommandDao().getAllOnce().single { it.id == attachedId })
        assertEquals(history, db.commandHistoryDao().getAllOnce().single())
    }

    /**
     * GCM picks a fresh IV per write, so the same command encrypts to a different string every
     * time. Anything comparing commands -- the attached-command dedup above all -- has to work on
     * the decrypted text, never on what sits in the column.
     */
    @Test
    fun equalCommandsMatchOnlyAfterDecryption() = runBlocking {
        val serverId = db.serverDao().insert(legacyServer("box", "pw", null))
        session.dek = passwordKeyManager.initialize("upgrade-test-pw".toCharArray())
        migrationManager.migrateIfNeeded()

        val command = "systemctl restart nginx"
        listOf("From catalog", "Typed by hand").forEachIndexed { index, label ->
            db.quickCommandDao().insert(
                QuickCommand(
                    serverId = serverId,
                    label = label,
                    command = command,
                    sortOrder = index
                ).toEntity(encryption)
            )
        }

        val rows = db.quickCommandDao().getAllOnce()
        assertNotEquals(
            "Two writes of the same command must not share a ciphertext",
            rows[0].encryptedCommand,
            rows[1].encryptedCommand
        )

        val domain = rows.map { it.toDomain(encryption) }
        assertEquals("Dedup on the domain model must collapse them", 1, domain.map { it.command }.toSet().size)
        assertEquals(setOf(command), domain.map { it.command }.toSet())
    }

    /** A command written under another key must blank out, not take its list down. */
    @Test
    fun anUnreadableCommandComesBackEmpty() = runBlocking {
        val serverId = db.serverDao().insert(legacyServer("box", "pw", null))
        session.dek = passwordKeyManager.initialize("upgrade-test-pw".toCharArray())
        migrationManager.migrateIfNeeded()

        db.commandHistoryDao().insert(
            CommandHistoryEntity(
                serverId = serverId,
                encryptedCommand = Base64.encodeToString(ByteArray(42), Base64.NO_WRAP),
                stdout = "out",
                stderr = "",
                exitCode = 0,
                executedAt = 1_700_000_000_000
            )
        )

        val item = db.commandHistoryDao().getAllOnce().single().toDomain(encryption)
        assertEquals("", item.command)
        assertEquals("Fields that were never encrypted stay readable", "out", item.stdout)
    }

    @Test
    fun secondPassIsANoOp() = runBlocking {
        val dao = db.serverDao()
        dao.insert(legacyServer("only", "pw", null))

        session.dek = passwordKeyManager.initialize("upgrade-test-pw".toCharArray())
        migrationManager.migrateIfNeeded()
        val afterFirst = dao.getAllServersOnce()

        migrationManager.migrateIfNeeded()
        assertEquals(afterFirst, dao.getAllServersOnce())
    }

    @Test
    fun anUnreadableRowLoadsAsCorruptedInsteadOfThrowing() = runBlocking {
        val dao = db.serverDao()
        val goodId = dao.insert(legacyServer("good", "pw", null))
        val badId = dao.insert(
            legacyServer("bad", "pw", null)
                .copy(encryptedPassword = Base64.encodeToString(ByteArray(42), Base64.NO_WRAP))
        )

        session.dek = passwordKeyManager.initialize("upgrade-test-pw".toCharArray())
        migrationManager.migrateIfNeeded()

        // The whole list has to load: one bad row must not take the others down with it.
        val servers = dao.getAllServersOnce().map { it.toDomain(encryption) }.associateBy { it.id }

        val good = servers.getValue(goodId)
        assertFalse(good.isCorrupted)
        assertEquals("pw", good.password)

        val bad = servers.getValue(badId)
        assertTrue("An unreadable row must be flagged", bad.isCorrupted)
        assertEquals("Secrets of a corrupted row must not leak through", "", bad.password)
        assertNull(bad.privateKey)
        // Host and login share the row's single decryption attempt, so they go the same way.
        assertEquals("", bad.host)
        assertEquals("", bad.login)
        assertEquals("Non-secret fields stay readable", "bad", bad.name)
    }

    @Test
    fun migrationIsDeferredWhileTheVaultIsLocked() = runBlocking {
        val dao = db.serverDao()
        dao.insert(legacyServer("only", "pw", null))
        val before = dao.getAllServersOnce()

        passwordKeyManager.initialize("upgrade-test-pw".toCharArray())
        session.clear()
        migrationManager.migrateIfNeeded()

        assertFalse(
            "A locked pass must not claim the migration is done",
            passwordKeyManager.isLegacyMigrated()
        )
        assertEquals("Nothing may be rewritten without the DEK", before, dao.getAllServersOnce())

        // Unlocking later still gets the row across.
        session.dek = passwordKeyManager.unlock("upgrade-test-pw".toCharArray())
        migrationManager.migrateIfNeeded()

        assertTrue(passwordKeyManager.isLegacyMigrated())
        assertEquals("pw", encryption.decrypt(dao.getAllServersOnce().first().encryptedPassword))
    }

    @Test
    fun cleanInstallJustMarksItselfMigrated() = runBlocking {
        session.dek = passwordKeyManager.initialize("upgrade-test-pw".toCharArray())
        migrationManager.migrateIfNeeded()

        assertTrue(passwordKeyManager.isLegacyMigrated())
        assertTrue(db.serverDao().getAllServersOnce().isEmpty())
    }

    @Test
    fun unlockReturnsTheSameDekAndRejectsAWrongPassword() = runBlocking {
        val dek = passwordKeyManager.initialize("upgrade-test-pw".toCharArray())

        assertEquals(
            dek.encoded.toList(),
            passwordKeyManager.unlock("upgrade-test-pw".toCharArray())?.encoded?.toList()
        )
        assertNull(passwordKeyManager.unlock("wrong-password".toCharArray()))
    }

    /** Guards the isolation itself: a regression here would put the user's vault at risk. */
    @Test
    fun testStoreIsSeparateFromTheProductionOne() {
        assertNotEquals(CryptoModule.DEFAULT_PREFS_FILE, TEST_PREFS)

        passwordKeyManager.initialize("upgrade-test-pw".toCharArray())
        assertTrue(passwordKeyManager.isInitialized())

        // The vault landed in the throwaway file; the production one is never even opened,
        // so whatever state the user's vault is in, this test cannot affect or depend on it.
        assertTrue(testPrefsFile().exists())
    }

    /** The same store CryptoModule builds in production, pointed at the test file. */
    private fun encryptedPrefs(fileName: String) = EncryptedSharedPreferences.create(
        context,
        fileName,
        MasterKey.Builder(context)
            .setKeyGenParameterSpec(
                KeyGenParameterSpec.Builder(
                    MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun testPrefsFile() =
        File(File(context.applicationInfo.dataDir, "shared_prefs"), "$TEST_PREFS.xml")

    private companion object {
        const val TEST_DB = "migration_test.db"
        const val TEST_PREFS = "auth_prefs_test"
        const val LEGACY_HOST = "10.0.0.1"
        const val LEGACY_LOGIN = "root"
    }
}
