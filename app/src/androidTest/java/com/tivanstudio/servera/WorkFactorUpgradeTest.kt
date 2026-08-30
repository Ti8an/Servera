package com.tivanstudio.servera

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tivanstudio.servera.data.crypto.EncryptionHelper
import com.tivanstudio.servera.data.crypto.KeystoreManager
import com.tivanstudio.servera.data.crypto.MigrationManager
import com.tivanstudio.servera.data.crypto.PasswordKeyManager
import com.tivanstudio.servera.data.db.AppDatabase
import com.tivanstudio.servera.data.db.entity.ServerEntity
import com.tivanstudio.servera.data.preferences.AppPreferences
import com.tivanstudio.servera.data.repository.AuthRepositoryImpl
import com.tivanstudio.servera.di.CommandResultHolder
import com.tivanstudio.servera.di.ServerCache
import com.tivanstudio.servera.di.SessionKeyHolder
import com.tivanstudio.servera.data.mapper.toDomain
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

/**
 * The DEK/KEK scheme shipped at a higher work factor than it now uses. `unlock` honours
 * whatever count the vault was created with, so such a vault would stay slow forever unless
 * something re-wraps it. That upgrade happens on the next successful login, which is the only
 * moment the password is available, and these tests pin that behaviour down.
 */
@RunWith(AndroidJUnit4::class)
class WorkFactorUpgradeTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var session: SessionKeyHolder
    private lateinit var encryption: EncryptionHelper
    private lateinit var passwordKeyManager: PasswordKeyManager
    private lateinit var db: AppDatabase
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        session = SessionKeyHolder()
        encryption = EncryptionHelper(KeystoreManager(), session)

        // A throwaway store: the production "auth_prefs" is never opened by this test.
        context.deleteSharedPreferences(TEST_PREFS)
        prefs = encryptedPrefs(TEST_PREFS)
        passwordKeyManager = PasswordKeyManager(prefs)

        context.deleteDatabase(TEST_DB)
        db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB).build()

        repository = AuthRepositoryImpl(
            prefs = prefs,
            passwordKeyManager = passwordKeyManager,
            migrationManager = MigrationManager(
                db, db.serverDao(), db.presetDao(), db.quickCommandDao(),
                db.commandHistoryDao(), encryption, passwordKeyManager, session
            ),
            session = session,
            serverDao = db.serverDao(),
            historyDao = db.commandHistoryDao(),
            quickCommandDao = db.quickCommandDao(),
            presetDao = db.presetDao(),
            presetGroupDao = db.presetGroupDao(),
            appPreferences = AppPreferences(context),
            serverCache = ServerCache(),
            commandResultHolder = CommandResultHolder()
        )
    }

    @After
    fun tearDown() {
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

    /** A vault as an early adopter's device carries it: wrapped at the superseded work factor. */
    private fun seedLegacyWorkFactorVault(password: String) =
        passwordKeyManager.initialize(password.toCharArray(), SUPERSEDED_ITERATIONS).also {
            session.dek = it
            // Keep the legacy re-encryption pass out of the way: it is a separate concern,
            // covered by LegacyMigrationTest.
            passwordKeyManager.setLegacyMigrated()
        }

    private suspend fun seedServer(name: String, password: String): Long =
        db.serverDao().insert(
            ServerEntity(
                name = name,
                encryptedHost = encryption.encrypt("10.0.0.1"),
                port = 22,
                encryptedLogin = encryption.encrypt("root"),
                encryptedPassword = encryption.encrypt(password),
                encryptedPrivateKey = null,
                timeout = 30,
                createdAt = 1_700_000_000_000
            )
        )

    @Test
    fun loggingIntoAnOldVaultMovesItToTheCurrentWorkFactor() = runBlocking {
        val password = "upgrade-test-pw"
        val dekBefore = seedLegacyWorkFactorVault(password)
        assertEquals(SUPERSEDED_ITERATIONS, passwordKeyManager.storedIterations())

        val serverPassword = "hunter2-äöü-пароль"
        val serverId = seedServer("before upgrade", serverPassword)
        session.clear()

        assertTrue(repository.verifyPassword(password))

        assertEquals(
            "The vault must be re-wrapped at the current work factor",
            PasswordKeyManager.PBKDF2_ITERATIONS,
            passwordKeyManager.storedIterations()
        )
        assertEquals(
            "Re-wrapping must preserve the DEK, not mint a new one",
            dekBefore.encoded.toList(),
            session.dek?.encoded?.toList()
        )
        assertEquals(
            "Data written before the upgrade must still decrypt",
            serverPassword,
            encryption.decrypt(db.serverDao().getAllServersOnce().first().encryptedPassword)
        )
        // The path the app actually uses when it loads a server.
        val server = db.serverDao().getAllServersOnce().first { it.id == serverId }.toDomain(encryption)
        assertFalse(server.isCorrupted)
        assertEquals(serverPassword, server.password)
    }

    @Test
    fun theUpgradedVaultStillOpensWithTheSamePasswordAndRejectsOthers() = runBlocking {
        val password = "upgrade-test-pw"
        val dekBefore = seedLegacyWorkFactorVault(password)
        session.clear()

        assertTrue(repository.verifyPassword(password))
        session.clear()

        // A fresh unlock now runs at the new work factor and must still yield the same DEK.
        assertEquals(
            dekBefore.encoded.toList(),
            passwordKeyManager.unlock(password.toCharArray())?.encoded?.toList()
        )
        assertNull(
            "The upgrade must not weaken the password check",
            passwordKeyManager.unlock("wrong-password".toCharArray())
        )
        assertFalse(repository.verifyPassword("wrong-password"))
    }

    @Test
    fun theUpgradeRunsOnceAndIsANoOpAfterwards() = runBlocking {
        val password = "upgrade-test-pw"
        seedLegacyWorkFactorVault(password)
        session.clear()

        assertTrue(repository.verifyPassword(password))
        val saltAfterUpgrade = prefs.getString(KEY_KDF_SALT, null)
        val wrappedAfterUpgrade = prefs.getString(KEY_WRAPPED_DEK, null)
        session.clear()

        assertTrue(repository.verifyPassword(password))

        assertEquals(PasswordKeyManager.PBKDF2_ITERATIONS, passwordKeyManager.storedIterations())
        assertEquals(
            "A vault already at the current work factor must not be re-wrapped again",
            saltAfterUpgrade,
            prefs.getString(KEY_KDF_SALT, null)
        )
        assertEquals(wrappedAfterUpgrade, prefs.getString(KEY_WRAPPED_DEK, null))
    }

    @Test
    fun aVaultCreatedNowIsLeftAloneOnLogin() = runBlocking {
        val password = "upgrade-test-pw"
        repository.setPassword(password)
        assertEquals(PasswordKeyManager.PBKDF2_ITERATIONS, passwordKeyManager.storedIterations())

        val salt = prefs.getString(KEY_KDF_SALT, null)
        val wrapped = prefs.getString(KEY_WRAPPED_DEK, null)
        session.clear()

        assertTrue(repository.verifyPassword(password))

        assertEquals("Nothing to upgrade, so nothing may be rewritten", salt, prefs.getString(KEY_KDF_SALT, null))
        assertEquals(wrapped, prefs.getString(KEY_WRAPPED_DEK, null))
    }

    @Test
    fun anUpgradeChangesTheSaltAndTheWrappedDek() = runBlocking {
        val password = "upgrade-test-pw"
        seedLegacyWorkFactorVault(password)
        val saltBefore = prefs.getString(KEY_KDF_SALT, null)
        val wrappedBefore = prefs.getString(KEY_WRAPPED_DEK, null)
        session.clear()

        assertTrue(repository.verifyPassword(password))

        assertNotEquals("The re-wrap must draw a fresh salt", saltBefore, prefs.getString(KEY_KDF_SALT, null))
        assertNotEquals(wrappedBefore, prefs.getString(KEY_WRAPPED_DEK, null))
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
        /** What [PasswordKeyManager.PBKDF2_ITERATIONS] was before it was lowered. */
        const val SUPERSEDED_ITERATIONS = 600_000
        const val TEST_DB = "work_factor_test.db"
        const val TEST_PREFS = "auth_prefs_work_factor_test"
        const val KEY_KDF_SALT = "kdf_salt"
        const val KEY_WRAPPED_DEK = "wrapped_dek"
    }
}
