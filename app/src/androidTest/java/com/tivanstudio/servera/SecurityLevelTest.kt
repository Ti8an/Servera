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
import com.tivanstudio.servera.data.crypto.SecurityLevel
import com.tivanstudio.servera.data.db.AppDatabase
import com.tivanstudio.servera.data.db.entity.ServerEntity
import com.tivanstudio.servera.data.mapper.toDomain
import com.tivanstudio.servera.data.preferences.AppPreferences
import com.tivanstudio.servera.data.repository.AuthRepositoryImpl
import com.tivanstudio.servera.di.CommandResultHolder
import com.tivanstudio.servera.di.ServerCache
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

/**
 * The vault's work factor is whatever `kdf_iterations` says, and only an explicit level change
 * moves it. These tests pin down both halves of that: a re-wrap swaps the KEK while keeping the
 * DEK (so the data survives), and nothing else -- a login above all -- touches the count.
 */
@RunWith(AndroidJUnit4::class)
class SecurityLevelTest {

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

    /** A vault as a fresh install carries it, with the legacy pass kept out of the way. */
    private suspend fun seedVault(password: String) {
        repository.setPassword(password)
        // The legacy re-encryption pass is a separate concern, covered by LegacyMigrationTest.
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

    /** Reads a stored server back the way the app does when it loads one. */
    private suspend fun readBackServer(id: Long) =
        db.serverDao().getAllServersOnce().first { it.id == id }.toDomain(encryption)

    @Test
    fun aNewVaultStartsAtTheDefaultLevel() = runBlocking {
        seedVault(PASSWORD)

        assertEquals(SecurityLevel.MIN, passwordKeyManager.currentLevel())
        assertEquals(SecurityLevel.MIN.iterations, passwordKeyManager.storedIterations())
        assertFalse(repository.isEnhancedEnabled())
    }

    @Test
    fun raisingTheLevelRewrapsTheSameDekAndLeavesTheDataReadable() = runBlocking {
        seedVault(PASSWORD)
        val dekBefore = session.dek!!
        val serverPassword = "hunter2-äöü-пароль"
        val serverId = seedServer("before the change", serverPassword)
        val saltBefore = prefs.getString(KEY_KDF_SALT, null)

        assertTrue(repository.changeSecurityLevel(SecurityLevel.MID, PASSWORD).isSuccess)

        assertEquals(SecurityLevel.MID.iterations, passwordKeyManager.storedIterations())
        assertEquals(SecurityLevel.MID, repository.getSecurityLevel())
        assertTrue(repository.isEnhancedEnabled())
        assertEquals(
            "The re-wrap must preserve the DEK, not mint a new one",
            dekBefore.encoded.toList(),
            session.dek?.encoded?.toList()
        )
        assertNotEquals(
            "The re-wrap must draw a fresh salt",
            saltBefore,
            prefs.getString(KEY_KDF_SALT, null)
        )
        assertEquals(serverPassword, readBackServer(serverId).password)

        // And on to the top level, from a level that is already raised.
        assertTrue(repository.changeSecurityLevel(SecurityLevel.HIGH, PASSWORD).isSuccess)

        assertEquals(SecurityLevel.HIGH.iterations, passwordKeyManager.storedIterations())
        assertEquals(SecurityLevel.HIGH, repository.getSecurityLevel())
        assertEquals(dekBefore.encoded.toList(), session.dek?.encoded?.toList())
        assertEquals(serverPassword, readBackServer(serverId).password)
    }

    @Test
    fun theRewrappedVaultOpensWithTheSamePasswordAndRejectsOthers() = runBlocking {
        seedVault(PASSWORD)
        val dekBefore = session.dek!!

        assertTrue(repository.changeSecurityLevel(SecurityLevel.HIGH, PASSWORD).isSuccess)
        session.clear()

        assertEquals(
            dekBefore.encoded.toList(),
            passwordKeyManager.unlock(PASSWORD.toCharArray())?.encoded?.toList()
        )
        assertNull(
            "Changing the level must not weaken the password check",
            passwordKeyManager.unlock(WRONG_PASSWORD.toCharArray())
        )
    }

    @Test
    fun turningEnhancedOffGoesBackToTheDefaultLevel() = runBlocking {
        seedVault(PASSWORD)
        val dekBefore = session.dek!!
        val serverPassword = "hunter2-äöü-пароль"
        val serverId = seedServer("before turning it off", serverPassword)

        assertTrue(repository.changeSecurityLevel(SecurityLevel.HIGH, PASSWORD).isSuccess)
        assertTrue(repository.changeSecurityLevel(SecurityLevel.MIN, PASSWORD).isSuccess)

        assertEquals(SecurityLevel.MIN.iterations, passwordKeyManager.storedIterations())
        assertFalse(repository.isEnhancedEnabled())
        assertEquals(dekBefore.encoded.toList(), session.dek?.encoded?.toList())
        assertEquals(serverPassword, readBackServer(serverId).password)
    }

    @Test
    fun aWrongPasswordLeavesTheLevelAndTheDataAlone() = runBlocking {
        seedVault(PASSWORD)
        val dekBefore = session.dek!!
        val serverPassword = "hunter2-äöü-пароль"
        val serverId = seedServer("untouched", serverPassword)

        assertTrue(repository.changeSecurityLevel(SecurityLevel.MID, PASSWORD).isSuccess)
        val saltAfter = prefs.getString(KEY_KDF_SALT, null)
        val wrappedAfter = prefs.getString(KEY_WRAPPED_DEK, null)

        assertTrue(repository.changeSecurityLevel(SecurityLevel.HIGH, WRONG_PASSWORD).isFailure)

        assertEquals(SecurityLevel.MID.iterations, passwordKeyManager.storedIterations())
        assertEquals(
            "A failed attempt must not rewrite the wrapping",
            saltAfter,
            prefs.getString(KEY_KDF_SALT, null)
        )
        assertEquals(wrappedAfter, prefs.getString(KEY_WRAPPED_DEK, null))
        assertEquals(dekBefore.encoded.toList(), session.dek?.encoded?.toList())

        val server = readBackServer(serverId)
        assertFalse(server.isCorrupted)
        assertEquals(serverPassword, server.password)
        assertTrue(
            "The vault must still open with the real password",
            repository.verifyPassword(PASSWORD)
        )
    }

    @Test
    fun loggingInDoesNotMoveTheLevel() = runBlocking {
        seedVault(PASSWORD)
        assertTrue(repository.changeSecurityLevel(SecurityLevel.HIGH, PASSWORD).isSuccess)
        val salt = prefs.getString(KEY_KDF_SALT, null)
        val wrapped = prefs.getString(KEY_WRAPPED_DEK, null)
        session.clear()

        assertTrue(repository.verifyPassword(PASSWORD))

        assertEquals(
            "A login must leave the chosen work factor exactly as it is",
            SecurityLevel.HIGH.iterations,
            passwordKeyManager.storedIterations()
        )
        assertEquals("A login must not re-wrap anything", salt, prefs.getString(KEY_KDF_SALT, null))
        assertEquals(wrapped, prefs.getString(KEY_WRAPPED_DEK, null))
    }

    @Test
    fun changingThePasswordKeepsTheChosenLevel() = runBlocking {
        seedVault(PASSWORD)
        assertTrue(repository.changeSecurityLevel(SecurityLevel.HIGH, PASSWORD).isSuccess)

        assertTrue(repository.changePassword(PASSWORD, "a-brand-new-password"))

        assertEquals(SecurityLevel.HIGH.iterations, passwordKeyManager.storedIterations())
        assertTrue(repository.verifyPassword("a-brand-new-password"))
    }

    @Test
    fun currentLevelMapsTheStoredWorkFactor() = runBlocking {
        seedVault(PASSWORD)

        assertEquals(SecurityLevel.MIN, SecurityLevel.fromIterations(100_000))
        assertEquals(SecurityLevel.MID, SecurityLevel.fromIterations(210_000))
        assertEquals(SecurityLevel.HIGH, SecurityLevel.fromIterations(600_000))
        // A count no level uses resolves to the nearest one rather than blowing up.
        assertEquals(SecurityLevel.MID, SecurityLevel.fromIterations(200_000))
        assertEquals(SecurityLevel.MIN, SecurityLevel.fromIterations(1_000))

        for (level in SecurityLevel.entries) {
            assertTrue(repository.changeSecurityLevel(level, PASSWORD).isSuccess)
            assertEquals(level, passwordKeyManager.currentLevel())
            assertEquals(level.iterations, passwordKeyManager.storedIterations())
        }
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
        const val PASSWORD = "level-test-pw"
        const val WRONG_PASSWORD = "not-the-password"
        const val TEST_DB = "security_level_test.db"
        const val TEST_PREFS = "auth_prefs_security_level_test"
        const val KEY_KDF_SALT = "kdf_salt"
        const val KEY_WRAPPED_DEK = "wrapped_dek"
    }
}
