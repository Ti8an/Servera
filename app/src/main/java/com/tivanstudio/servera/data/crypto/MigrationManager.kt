package com.tivanstudio.servera.data.crypto

import android.util.Log
import androidx.room.withTransaction
import com.tivanstudio.servera.data.db.AppDatabase
import com.tivanstudio.servera.data.db.dao.ServerDao
import com.tivanstudio.servera.data.db.entity.ServerEntity
import com.tivanstudio.servera.di.SessionKeyHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Moves rows written by the pre-V2 scheme (a single Android Keystore key) onto the
 * password-derived DEK.
 *
 * Users upgrading from an older build set a password for the first time under the new scheme:
 * that call creates the DEK, and this pass immediately re-encrypts their existing rows under it.
 * Without it their servers would stay readable only by the legacy key that nothing reads any more.
 */
@Singleton
class MigrationManager @Inject constructor(
    private val db: AppDatabase,
    private val serverDao: ServerDao,
    private val encryption: EncryptionHelper,
    private val passwordKeyManager: PasswordKeyManager,
    private val session: SessionKeyHolder
) {

    /**
     * Re-encrypts every legacy row under the session DEK, then records that it is done.
     * Safe to call repeatedly: after the first successful pass it returns immediately.
     * Requires an unlocked session (the DEK is needed to write), so call it right after
     * a password has been set or verified.
     */
    suspend fun migrateIfNeeded() = withContext(Dispatchers.IO) {
        if (!passwordKeyManager.isInitialized()) return@withContext
        if (passwordKeyManager.isLegacyMigrated()) return@withContext
        // Without the DEK every re-encryption would fail and be counted as an unreadable row,
        // and the pass would then mark itself done -- stranding legacy data for good.
        if (!session.isUnlocked()) {
            Log.w(TAG, "Migration asked for while the vault is locked, deferred")
            return@withContext
        }

        val servers = serverDao.getAllServersOnce()
        if (servers.isEmpty()) {
            // Clean install: nothing was ever written with the legacy key.
            passwordKeyManager.setLegacyMigrated()
            return@withContext
        }

        var migrated = 0
        var skipped = 0

        db.withTransaction {
            for (entity in servers) {
                val reencrypted = reencrypt(entity)
                if (reencrypted == null) {
                    skipped++
                } else {
                    serverDao.update(reencrypted)
                    migrated++
                }
            }
        }

        passwordKeyManager.setLegacyMigrated()
        Log.i(TAG, "Legacy re-encryption done: $migrated migrated, $skipped skipped")
    }

    /**
     * Returns the row with both secret fields re-encrypted under the DEK, or null when the
     * legacy key cannot read it -- a row written by another key, or corrupt ciphertext.
     * Both fields are converted together so a row never ends up half legacy, half DEK.
     */
    @Suppress("DEPRECATION") // decryptLegacy is deprecated for callers, this is its one user
    private fun reencrypt(entity: ServerEntity): ServerEntity? = runCatching {
        val password = encryption.decryptLegacy(entity.encryptedPassword)
        val privateKey = entity.encryptedPrivateKey?.let { encryption.decryptLegacy(it) }

        entity.copy(
            encryptedPassword = encryption.encrypt(password),
            encryptedPrivateKey = privateKey?.let { encryption.encrypt(it) }
        )
    }.onFailure {
        Log.w(TAG, "Server ${entity.id} is not readable with the legacy key, left as is", it)
    }.getOrNull()

    private companion object {
        const val TAG = "MigrationManager"
    }
}
