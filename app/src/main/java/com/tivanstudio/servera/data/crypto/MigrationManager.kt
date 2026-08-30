package com.tivanstudio.servera.data.crypto

import android.util.Log
import androidx.room.withTransaction
import com.tivanstudio.servera.data.db.AppDatabase
import com.tivanstudio.servera.data.db.dao.CommandHistoryDao
import com.tivanstudio.servera.data.db.dao.PresetDao
import com.tivanstudio.servera.data.db.dao.QuickCommandDao
import com.tivanstudio.servera.data.db.dao.ServerDao
import com.tivanstudio.servera.data.db.entity.ServerEntity
import com.tivanstudio.servera.di.SessionKeyHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Moves rows written by the pre-V2 scheme (a single Android Keystore key) onto the
 * password-derived DEK, and puts the fields that scheme never protected at all under it.
 *
 * Users upgrading from an older build set a password for the first time under the new scheme:
 * that call creates the DEK, and this pass immediately re-encrypts their existing rows under it.
 * Without it their servers would stay readable only by the legacy key that nothing reads any more.
 *
 * Two kinds of field go through here. Server secrets arrive as legacy ciphertext and have to be
 * decrypted with the old key first. Host, login and every stored command arrive as plaintext --
 * the schema migrations that renamed those columns left the values alone -- so they are simply
 * encrypted where they stand.
 */
@Singleton
class MigrationManager @Inject constructor(
    private val db: AppDatabase,
    private val serverDao: ServerDao,
    private val presetDao: PresetDao,
    private val quickCommandDao: QuickCommandDao,
    private val historyDao: CommandHistoryDao,
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

        var migrated = 0
        var skipped = 0
        var commands = 0

        // One transaction over the whole pass: a half-converted database would leave rows
        // nothing can read, so either all of it lands or none of it does.
        db.withTransaction {
            for (entity in serverDao.getAllServersOnce()) {
                val reencrypted = reencrypt(entity)
                if (reencrypted == null) {
                    skipped++
                } else {
                    serverDao.update(reencrypted)
                    migrated++
                }
            }

            for (preset in presetDao.getAllOnce()) {
                val encrypted = encryptPlainCommand(preset.encryptedCommand, "preset", preset.id)
                    ?: continue
                presetDao.update(preset.copy(encryptedCommand = encrypted))
                commands++
            }

            for (cmd in quickCommandDao.getAllOnce()) {
                val encrypted = encryptPlainCommand(cmd.encryptedCommand, "quick command", cmd.id)
                    ?: continue
                quickCommandDao.update(cmd.copy(encryptedCommand = encrypted))
                commands++
            }

            for (item in historyDao.getAllOnce()) {
                val encrypted = encryptPlainCommand(item.encryptedCommand, "history", item.id)
                    ?: continue
                historyDao.update(item.copy(encryptedCommand = encrypted))
                commands++
            }
        }

        passwordKeyManager.setLegacyMigrated()
        Log.i(
            TAG,
            "Legacy re-encryption done: $migrated servers migrated, $skipped skipped, " +
                "$commands commands encrypted"
        )
    }

    /**
     * Returns the row with every protected field written under the DEK, or null when the legacy
     * key cannot read the secrets -- a row written by another key, or corrupt ciphertext.
     * All fields are converted together so a row never ends up half legacy, half DEK.
     *
     * The secrets arrive as legacy ciphertext and have to be decrypted first. Host and login do
     * not: the pre-V2 schema kept them in plaintext (the v8 -> v9 migration only renamed the
     * columns), so they are encrypted as they stand. Nothing here can meet an already-DEK value:
     * the pass runs once, guarded by the legacy_migrated flag.
     */
    @Suppress("DEPRECATION") // decryptLegacy is deprecated for callers, this is its one user
    private fun reencrypt(entity: ServerEntity): ServerEntity? = runCatching {
        val password = encryption.decryptLegacy(entity.encryptedPassword)
        val privateKey = entity.encryptedPrivateKey?.let { encryption.decryptLegacy(it) }

        entity.copy(
            encryptedHost = encryption.encrypt(entity.encryptedHost),
            encryptedLogin = encryption.encrypt(entity.encryptedLogin),
            encryptedPassword = encryption.encrypt(password),
            encryptedPrivateKey = privateKey?.let { encryption.encrypt(it) }
        )
    }.onFailure {
        Log.w(TAG, "Server ${entity.id} is not readable with the legacy key, left as is", it)
    }.getOrNull()

    /**
     * Puts a command that has always been stored in the clear under the DEK. Returns null when
     * there is nothing to do -- a blank command carries nothing worth protecting -- or when the
     * write fails, which leaves that one row as it was instead of failing the whole pass.
     *
     * Nothing here can meet an already-encrypted command: the pass runs once, guarded by the
     * legacy_migrated flag.
     */
    private fun encryptPlainCommand(plain: String, kind: String, id: Long): String? {
        if (plain.isBlank()) return null
        return runCatching { encryption.encrypt(plain) }
            .onFailure { Log.w(TAG, "Command of $kind $id could not be encrypted, left as is", it) }
            .getOrNull()
    }

    private companion object {
        const val TAG = "MigrationManager"
    }
}
