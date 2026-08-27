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
import com.tivanstudio.servera.data.db.entity.ServerEntity
import com.tivanstudio.servera.data.mapper.toDomain
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
        migrationManager =
            MigrationManager(db, db.serverDao(), encryption, passwordKeyManager, session)
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

    private fun legacyServer(name: String, password: String, privateKey: String?) = ServerEntity(
        name = name,
        host = "10.0.0.1",
        port = 22,
        login = "root",
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
            // The path the app actually uses when it loads a server.
            val server = row.toDomain(encryption)
            assertEquals(plainPassword, server.password)
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
    }
}
