package com.tivanstudio.servera

import android.content.Context
import android.util.Base64
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
        passwordKeyManager = PasswordKeyManager(context)
        passwordKeyManager.clearCrypto()

        context.deleteDatabase(TEST_DB)
        db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB).build()
        migrationManager = MigrationManager(db, db.serverDao(), encryption, passwordKeyManager)
    }

    @After
    fun tearDown() {
        db.close()
        session.clear()
        passwordKeyManager.clearCrypto()
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

    private companion object {
        const val TEST_DB = "migration_test.db"
    }
}
