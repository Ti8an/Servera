package com.tivanstudio.servera.data.repository

import android.content.SharedPreferences
import com.tivanstudio.servera.data.crypto.MigrationManager
import com.tivanstudio.servera.data.crypto.PasswordKeyManager
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @AuthPrefs private val prefs: SharedPreferences,
    private val passwordKeyManager: PasswordKeyManager,
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
        session.dek = passwordKeyManager.initialize(password.toCharArray())
        // Upgrading users set their first password here: their existing rows are still
        // encrypted with the legacy Keystore key and have to move onto the fresh DEK now.
        migrationManager.migrateIfNeeded()
    }

    override suspend fun verifyPassword(password: String): Boolean {
        val dek = passwordKeyManager.unlock(password.toCharArray()) ?: return false
        session.dek = dek
        // No-op once the flag is set; retries a pass that died half-way through.
        migrationManager.migrateIfNeeded()
        return true
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Boolean {
        val dek = passwordKeyManager.unlock(oldPassword.toCharArray()) ?: return false
        passwordKeyManager.rewrap(dek, newPassword.toCharArray())
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
        prefs.edit()
            .remove(KEY_BIOMETRIC_ENABLED)
            // Pre-V2 builds stored a password hash here; upgraded installs still carry it.
            .remove(KEY_LEGACY_PASSWORD_HASH)
            .apply()

        serverCache.clear()
        commandResultHolder.clear()
    }

    override fun isBiometricEnabled(): Boolean =
        prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LEGACY_PASSWORD_HASH = "password_hash"
    }
}
