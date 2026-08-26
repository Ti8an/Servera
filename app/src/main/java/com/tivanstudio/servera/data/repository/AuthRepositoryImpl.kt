package com.tivanstudio.servera.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tivanstudio.servera.data.crypto.MigrationManager
import com.tivanstudio.servera.data.crypto.PasswordKeyManager
import com.tivanstudio.servera.data.db.dao.CommandHistoryDao
import com.tivanstudio.servera.data.db.dao.PresetDao
import com.tivanstudio.servera.data.db.dao.PresetGroupDao
import com.tivanstudio.servera.data.db.dao.QuickCommandDao
import com.tivanstudio.servera.data.db.dao.ServerDao
import com.tivanstudio.servera.data.preferences.AppPreferences
import com.tivanstudio.servera.di.CommandResultHolder
import com.tivanstudio.servera.di.ServerCache
import com.tivanstudio.servera.di.SessionKeyHolder
import com.tivanstudio.servera.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
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

    private val prefs by lazy {
        val spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        val masterKey = MasterKey.Builder(context)
            .setKeyGenParameterSpec(spec)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

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
        prefs.edit().remove(KEY_BIOMETRIC_ENABLED).apply()

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
    }
}
