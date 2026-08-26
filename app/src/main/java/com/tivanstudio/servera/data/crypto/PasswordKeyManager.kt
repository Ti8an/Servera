package com.tivanstudio.servera.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tivanstudio.servera.di.PrefsFileName
import dagger.hilt.android.qualifiers.ApplicationContext
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

/**
 * Two-level key scheme: password -> KEK (PBKDF2) -> wraps the DEK, and the DEK encrypts the data.
 * There is no recovery phrase, so a forgotten password means a wipe.
 */
@Singleton
class PasswordKeyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @PrefsFileName private val prefsFileName: String = DEFAULT_PREFS_FILE
) {

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
            prefsFileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun deriveKek(password: CharArray, salt: ByteArray, iterations: Int): SecretKey {
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
     * Creates a fresh DEK and wraps it with a KEK derived from [password].
     * @throws IllegalStateException if a password has already been set.
     */
    fun initialize(password: CharArray): SecretKey {
        check(!isInitialized()) { "Crypto is already initialized" }

        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val dek = KeyGenerator.getInstance("AES").apply { init(KEY_LENGTH_BITS) }.generateKey()
        val kek = deriveKek(password, salt, PBKDF2_ITERATIONS)
        val wrapped = aesGcmEncrypt(kek, dek.encoded)

        prefs.edit()
            .putString(KEY_WRAPPED_DEK, wrapped.toBase64())
            .putString(KEY_KDF_SALT, salt.toBase64())
            .putInt(KEY_KDF_ITERATIONS, PBKDF2_ITERATIONS)
            .putInt(KEY_CRYPTO_SCHEME, SCHEME_V2)
            .apply()

        return dek
    }

    /**
     * Unwraps the DEK with [password], or returns null when the password is wrong
     * (GCM tag mismatch) or nothing has been initialized yet.
     */
    fun unlock(password: CharArray): SecretKey? {
        val wrapped = prefs.getString(KEY_WRAPPED_DEK, null)?.fromBase64() ?: return null
        val salt = prefs.getString(KEY_KDF_SALT, null)?.fromBase64() ?: return null
        val iterations = prefs.getInt(KEY_KDF_ITERATIONS, PBKDF2_ITERATIONS)

        val kek = deriveKek(password, salt, iterations)
        return runCatching { SecretKeySpec(aesGcmDecrypt(kek, wrapped), "AES") }.getOrNull()
    }

    /**
     * Re-wraps the same DEK under a new password, so the stored data stays readable.
     * Writes the current [PBKDF2_ITERATIONS]: a vault created with an older work factor picks
     * the new one up here, since [unlock] always reads whatever was stored alongside the salt.
     */
    fun rewrap(dek: SecretKey, newPassword: CharArray) {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val kek = deriveKek(newPassword, salt, PBKDF2_ITERATIONS)
        val wrapped = aesGcmEncrypt(kek, dek.encoded)

        prefs.edit()
            .putString(KEY_WRAPPED_DEK, wrapped.toBase64())
            .putString(KEY_KDF_SALT, salt.toBase64())
            .putInt(KEY_KDF_ITERATIONS, PBKDF2_ITERATIONS)
            .apply()
    }

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
        const val DEFAULT_PREFS_FILE = "auth_prefs"
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
