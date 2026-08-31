package com.tivanstudio.servera.data.repository

import android.content.SharedPreferences
import com.tivanstudio.servera.data.crypto.BiometricKeyManager
import com.tivanstudio.servera.data.crypto.MigrationManager
import com.tivanstudio.servera.data.crypto.PasswordKeyManager
import com.tivanstudio.servera.data.crypto.SecurityLevel
import com.tivanstudio.servera.data.db.dao.CommandHistoryDao
import com.tivanstudio.servera.data.db.dao.PresetDao
import com.tivanstudio.servera.data.db.dao.PresetGroupDao
import com.tivanstudio.servera.data.db.dao.QuickCommandDao
import com.tivanstudio.servera.data.db.dao.ServerDao
import com.tivanstudio.servera.data.preferences.AppPreferences
import com.tivanstudio.servera.di.AuthPrefs
import com.tivanstudio.servera.di.CommandResultHolder
import com.tivanstudio.servera.di.ServerCache
import com.tivanstudio.servera.di.SessionKeyHolder
import com.tivanstudio.servera.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @AuthPrefs private val prefs: SharedPreferences,
    private val passwordKeyManager: PasswordKeyManager,
    private val biometricKeyManager: BiometricKeyManager,
    private val migrationManager: MigrationManager,
    private val session: SessionKeyHolder,
    private val serverDao: ServerDao,
    private val historyDao: CommandHistoryDao,
    private val quickCommandDao: QuickCommandDao,
    private val presetDao: PresetDao,
    private val presetGroupDao: PresetGroupDao,
    private val appPreferences: AppPreferences,
    private val serverCache: ServerCache,
    private val commandResultHolder: CommandResultHolder
) : AuthRepository {

    override suspend fun setPassword(password: String) {
        // PBKDF2 is deliberately expensive, so it must never run on the caller's thread --
        // this is reached from viewModelScope, which is Main.
        session.dek = withContext(Dispatchers.Default) {
            passwordKeyManager.initialize(password.toCharArray())
        }
        // Upgrading users set their first password here: their existing rows are still
        // encrypted with the legacy Keystore key and have to move onto the fresh DEK now.
        migrationManager.migrateIfNeeded()
    }

    override suspend fun verifyPassword(password: String): Boolean {
        // Login reads the stored work factor and leaves it alone: the level is the user's
        // choice, made in settings, not something a login quietly rewrites underneath them.
        val dek = withContext(Dispatchers.Default) {
            passwordKeyManager.unlock(password.toCharArray())
        } ?: return false

        session.dek = dek
        // No-op once the flag is set; retries a pass that died half-way through.
        migrationManager.migrateIfNeeded()
        return true
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Boolean {
        // Two PBKDF2 derivations back to back (unwrap, then re-wrap), so off the caller's thread.
        val dek = withContext(Dispatchers.Default) {
            passwordKeyManager.unlock(oldPassword.toCharArray())?.also { unlocked ->
                passwordKeyManager.rewrap(unlocked, newPassword.toCharArray())
            }
        } ?: return false

        session.dek = dek
        return true
    }

    override suspend fun isPasswordSet(): Boolean = passwordKeyManager.isInitialized()

    override fun lock() = session.clear()

    override suspend fun resetAll() {
        session.clear()
        passwordKeyManager.clearCrypto()

        historyDao.clearAll()
        quickCommandDao.clearAll()
        serverDao.clearAll()
        presetDao.clearAll()
        presetGroupDao.clearAll()

        appPreferences.clear()
        biometricKeyManager.clearBiometric()
        prefs.edit()
            .remove(KEY_BIOMETRIC_ENABLED)
            // Pre-V2 builds stored a password hash here; upgraded installs still carry it.
            .remove(KEY_LEGACY_PASSWORD_HASH)
            .apply()

        serverCache.clear()
        commandResultHolder.clear()
    }

    // The flag alone is not enough: a new fingerprint enrollment invalidates the BEK and takes
    // the wrapping with it, leaving the flag pointing at a door that no longer exists.
    override fun isBiometricEnabled(): Boolean =
        prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false) &&
                biometricKeyManager.isBiometricWrapPresent()

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    override fun getBiometricEncryptCipher(): Cipher? {
        // Nothing to wrap while locked, and generating a BEK here would only orphan it.
        if (session.dek == null) return null
        biometricKeyManager.generateBek()
        return biometricKeyManager.getEncryptCipher()
    }

    override suspend fun finishEnableBiometric(cipher: Cipher): Result<Unit> = runCatching {
        val dek = session.dek ?: error("Session is locked")
        biometricKeyManager.wrapDekWithCipher(dek, cipher)
        setBiometricEnabled(true)
    }

    override suspend fun disableBiometric() {
        biometricKeyManager.clearBiometric()
        setBiometricEnabled(false)
    }

    override fun getBiometricDecryptCipher(): Cipher? = biometricKeyManager.getDecryptCipher()

    override suspend fun unlockWithBiometricCipher(cipher: Cipher): Boolean {
        // One AES-GCM unwrap, not a derivation -- but it is still Keystore work, and the caller
        // is on Main.
        val dek = withContext(Dispatchers.Default) {
            runCatching { biometricKeyManager.unwrapDekWithCipher(cipher) }.getOrNull()
        } ?: return false

        session.dek = dek
        // The password path runs this too; biometric unlock is no less of a way in.
        migrationManager.migrateIfNeeded()
        return true
    }

    override fun getSecurityLevel(): SecurityLevel = passwordKeyManager.currentLevel()

    override fun isEnhancedEnabled(): Boolean = getSecurityLevel() != PasswordKeyManager.DEFAULT_LEVEL

    override suspend fun changeSecurityLevel(level: SecurityLevel, password: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            val dek = passwordKeyManager.unlock(password.toCharArray())
                ?: return@withContext Result.failure(IllegalArgumentException("Wrong password"))

            passwordKeyManager.changeSecurityLevel(dek, password.toCharArray(), level)
            // Same DEK as before, but the caller may be re-wrapping while locked out of session
            // state (e.g. after a background kill), so put it back in hand either way.
            session.dek = dek
            Result.success(Unit)
        }

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LEGACY_PASSWORD_HASH = "password_hash"
    }
}
