package com.tivanstudio.servera.data.crypto

import android.content.SharedPreferences
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.tivanstudio.servera.di.AuthPrefs
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * How hard the vault makes an offline password guess. The whole cost is the PBKDF2 work factor,
 * so a level is nothing but an iteration count -- the wrapped DEK and the data underneath it
 * stay as they are when the user moves between levels.
 */
enum class SecurityLevel(val iterations: Int) {
    MIN(100_000),
    MID(210_000),
    HIGH(600_000);

    companion object {
        /**
         * Maps a stored work factor back onto a level. A count no level uses (a vault from an
         * older build, or one hand-edited) resolves to the nearest one, so the UI always has
         * something honest to show.
         */
        fun fromIterations(iterations: Int): SecurityLevel =
            entries.firstOrNull { it.iterations == iterations }
                ?: entries.minByOrNull { abs(it.iterations - iterations) }
                ?: MIN
    }
}

/**
 * Two-level key scheme: password -> KEK (PBKDF2) -> wraps the DEK, and the DEK encrypts the data.
 * There is no recovery phrase, so a forgotten password means a wipe.
 *
 * The work factor lives in one place only: `kdf_iterations` in prefs. Every unlock reads it as it
 * stands and nothing rewrites it on its own -- it moves only when the user picks a different
 * [SecurityLevel] through [changeSecurityLevel].
 */
@Singleton
class PasswordKeyManager @Inject constructor(
    @AuthPrefs private val prefs: SharedPreferences
) {

    /**
     * @throws IllegalArgumentException on an empty password: the PBKDF2 provider rejects one
     * anyway, and it does so with an exception no caller here was written to expect.
     */
    private fun deriveKek(password: CharArray, salt: ByteArray, iterations: Int): SecretKey {
        require(password.isNotEmpty()) { "Password must not be empty" }

        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun aesGcmEncrypt(key: SecretKey, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.iv + cipher.doFinal(data)
    }

    private fun aesGcmDecrypt(key: SecretKey, blob: ByteArray): ByteArray {
        val iv = blob.copyOfRange(0, IV_SIZE)
        val data = blob.copyOfRange(IV_SIZE, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, iv))
        return cipher.doFinal(data)
    }

    /**
     * Creates a fresh DEK and wraps it with a KEK derived from [password], at [DEFAULT_LEVEL].
     * @throws IllegalStateException if a password has already been set.
     */
    fun initialize(password: CharArray): SecretKey = initialize(password, DEFAULT_LEVEL.iterations)

    /**
     * Exposed so a test can build a vault at an arbitrary work factor; production always goes
     * through [initialize] or [changeSecurityLevel].
     */
    @VisibleForTesting
    fun initialize(password: CharArray, iterations: Int): SecretKey {
        check(!isInitialized()) { "Crypto is already initialized" }

        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val dek = KeyGenerator.getInstance("AES").apply { init(KEY_LENGTH_BITS) }.generateKey()
        val kek = deriveKek(password, salt, iterations)
        val wrapped = aesGcmEncrypt(kek, dek.encoded)

        prefs.edit()
            .putString(KEY_WRAPPED_DEK, wrapped.toBase64())
            .putString(KEY_KDF_SALT, salt.toBase64())
            .putInt(KEY_KDF_ITERATIONS, iterations)
            .putInt(KEY_CRYPTO_SCHEME, SCHEME_V2)
            .apply()

        return dek
    }

    /**
     * Unwraps the DEK with [password], or returns null when the password is wrong
     * (GCM tag mismatch or empty) or nothing has been initialized yet.
     */
    fun unlock(password: CharArray): SecretKey? {
        // No vault can be created with an empty password, so there is nothing for one to open:
        // it is a wrong password like any other, not a case worth crashing the caller over.
        if (password.isEmpty()) return null

        val wrapped = prefs.getString(KEY_WRAPPED_DEK, null)?.fromBase64() ?: return null
        val salt = prefs.getString(KEY_KDF_SALT, null)?.fromBase64() ?: return null
        val iterations = storedIterations()

        val kek = deriveKek(password, salt, iterations)
        return runCatching { SecretKeySpec(aesGcmDecrypt(kek, wrapped), "AES") }.getOrNull()
    }

    /**
     * Re-wraps the same DEK under a new password, so the stored data stays readable. The work
     * factor is deliberately carried over: changing a password is not a place to silently move
     * the vault to another [SecurityLevel].
     */
    fun rewrap(dek: SecretKey, newPassword: CharArray) {
        wrap(dek, newPassword, storedIterations())
    }

    /**
     * Re-wraps the same DEK at the work factor of [level]. The DEK -- and therefore every
     * encrypted row -- is untouched; only the KEK, the salt and the stored iteration count move.
     */
    fun changeSecurityLevel(dek: SecretKey, password: CharArray, level: SecurityLevel) {
        wrap(dek, password, level.iterations)
    }

    private fun wrap(dek: SecretKey, password: CharArray, iterations: Int) {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val kek = deriveKek(password, salt, iterations)
        val wrapped = aesGcmEncrypt(kek, dek.encoded)

        prefs.edit()
            .putString(KEY_WRAPPED_DEK, wrapped.toBase64())
            .putString(KEY_KDF_SALT, salt.toBase64())
            .putInt(KEY_KDF_ITERATIONS, iterations)
            .apply()
    }

    /** The work factor this vault was wrapped with, which is what [unlock] honours. */
    fun storedIterations(): Int = prefs.getInt(KEY_KDF_ITERATIONS, PBKDF2_ITERATIONS)

    /** The level [storedIterations] corresponds to, for the settings screen. */
    fun currentLevel(): SecurityLevel = SecurityLevel.fromIterations(storedIterations())

    fun isInitialized(): Boolean =
        prefs.getString(KEY_WRAPPED_DEK, null) != null &&
            prefs.getInt(KEY_CRYPTO_SCHEME, 0) == SCHEME_V2

    /** True once the pre-V2 Keystore-encrypted rows have been re-encrypted under the DEK. */
    fun isLegacyMigrated(): Boolean = prefs.getBoolean(KEY_LEGACY_MIGRATED, false)

    fun setLegacyMigrated() {
        prefs.edit().putBoolean(KEY_LEGACY_MIGRATED, true).apply()
    }

    /** Drops the wrapped DEK and its KDF parameters, used by the wipe flow. */
    fun clearCrypto() {
        prefs.edit()
            .remove(KEY_WRAPPED_DEK)
            .remove(KEY_KDF_SALT)
            .remove(KEY_KDF_ITERATIONS)
            .remove(KEY_CRYPTO_SCHEME)
            .remove(KEY_LEGACY_MIGRATED)
            .apply()
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    companion object {
        /** What a brand new vault is created at. */
        val DEFAULT_LEVEL = SecurityLevel.MIN

        /**
         * Historical work factor, kept only as the fallback for reading `kdf_iterations`: every
         * build that wrote a V2 vault used exactly this count, so guessing anything else for a
         * vault whose count somehow went missing would lock it out.
         */
        const val PBKDF2_ITERATIONS = 210_000
        const val SCHEME_V2 = 2

        private const val KEY_WRAPPED_DEK = "wrapped_dek"
        private const val KEY_KDF_SALT = "kdf_salt"
        private const val KEY_KDF_ITERATIONS = "kdf_iterations"
        private const val KEY_CRYPTO_SCHEME = "crypto_scheme"
        private const val KEY_LEGACY_MIGRATED = "legacy_migrated"

        private const val KEY_LENGTH_BITS = 256
        private const val SALT_SIZE = 16
        private const val IV_SIZE = 12
        private const val TAG_SIZE_BITS = 128
    }
}
