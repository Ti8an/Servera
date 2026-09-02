package com.tivanstudio.servera.data.crypto

import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.tivanstudio.servera.di.AuthPrefs
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The second wrapping of the DEK, this one under a Keystore key (the BEK) that only a strong
 * biometric can use. It sits beside the password wrapping rather than replacing it: the same DEK
 * is stored twice, so a fingerprint is a second door into the vault and never the only one.
 *
 * The BEK never leaves the Keystore, and the hardware releases it only for a Cipher that has been
 * through a BiometricPrompt. That is why this class hands out un-finished ciphers and takes them
 * back once authenticated -- it deliberately cannot wrap or unwrap on its own.
 *
 * The key is invalidated by a new enrollment, so adding a fingerprint drops biometric unlock and
 * sends the user back to their password. That is the intended trade: someone who can enroll their
 * own finger still cannot reach the DEK.
 *
 * Unrelated to [KeystoreManager], whose servera_aes_key is the legacy pre-V2 migration key.
 */
@Singleton
class BiometricKeyManager @Inject constructor(
    @AuthPrefs private val prefs: SharedPreferences
) {

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun loadBek(): SecretKey? {
        val store = keyStore()
        if (!store.containsAlias(KEY_ALIAS)) return null
        return (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    /**
     * @param requireAuth false only for [generateBekForTest]; production keys are always gated.
     * @param strongBox asks for the secure element, which not every device has.
     */
    private fun buildSpec(requireAuth: Boolean, strongBox: Boolean): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .apply {
                if (requireAuth) {
                    setUserAuthenticationRequired(true)
                    setInvalidatedByBiometricEnrollment(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // A timeout of 0 means every single use needs its own authentication, and
                        // STRONG rules out the face/iris sensors the platform ranks as weak.
                        setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                    } else {
                        // The pre-30 spelling of the same thing: -1 is per-use authentication,
                        // which for a key of this shape means a biometric CryptoObject.
                        @Suppress("DEPRECATION")
                        setUserAuthenticationValidityDurationSeconds(-1)
                    }
                }
                if (strongBox) askForStrongBox()
            }
            .build()

    @RequiresApi(Build.VERSION_CODES.P)
    private fun KeyGenParameterSpec.Builder.askForStrongBox() {
        runCatching { setIsStrongBoxBacked(true) }
    }

    private fun generate(spec: KeyGenParameterSpec) {
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(spec)
            generateKey()
        }
    }

    private fun deleteKey() {
        runCatching { keyStore().deleteEntry(KEY_ALIAS) }
    }

    private fun generateKeyOfKind(requireAuth: Boolean) {
        deleteKey()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            generate(buildSpec(requireAuth, strongBox = false))
            return
        }
        try {
            generate(buildSpec(requireAuth, strongBox = true))
        } catch (_: StrongBoxUnavailableException) {
            // No secure element on this device; a TEE-backed key is the honest fallback.
            deleteKey()
            generate(buildSpec(requireAuth, strongBox = false))
        }
    }

    /**
     * Creates the BEK, replacing whatever key sits under the alias. Anything wrapped with the old
     * key is unreadable afterwards, so enabling biometric unlock must follow this with a fresh
     * [wrapDekWithCipher].
     */
    fun generateBek() = generateKeyOfKind(requireAuth = true)

    /**
     * The same key without the authentication gate, so an instrumentation test can exercise the
     * wrap/unwrap round trip -- Base64, IV and GCM tag -- with no BiometricPrompt to drive.
     * Never call this from production code: it produces a key a thief could use unprompted.
     */
    @VisibleForTesting
    internal fun generateBekForTest() = generateKeyOfKind(requireAuth = false)

    /**
     * A cipher to hand to BiometricPrompt.CryptoObject. It is only initialised here; the doFinal
     * belongs to [wrapDekWithCipher], once the prompt has released the key.
     */
    fun getEncryptCipher(): Cipher {
        val bek = checkNotNull(loadBek()) { "No biometric key; call generateBek() first" }
        return Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, bek) }
    }

    /**
     * The decrypting counterpart, initialised with the IV its wrapping was made under.
     *
     * @return null when there is nothing to unwrap, or when the key is gone because a new
     * enrollment invalidated it. Either way the answer for the caller is the same: this unlock
     * has to go through the password.
     */
    fun getDecryptCipher(): Cipher? {
        if (!isBiometricWrapPresent()) return null
        val iv = prefs.getString(PREF_BIO_IV, null)?.fromBase64() ?: return null
        val bek = loadBek() ?: return null
        return try {
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, bek, GCMParameterSpec(TAG_SIZE_BITS, iv))
            }
        } catch (_: KeyPermanentlyInvalidatedException) {
            // A finger was added or the biometrics were reset: the wrapping is dead weight now.
            clearBiometric()
            null
        }
    }

    /**
     * Stores the DEK under the BEK. [cipher] must be one [getEncryptCipher] made, already carried
     * through an authenticated prompt.
     */
    fun wrapDekWithCipher(dek: SecretKey, cipher: Cipher) {
        val encrypted = cipher.doFinal(dek.encoded)
        prefs.edit()
            .putString(PREF_WRAPPED_DEK_BIO, encrypted.toBase64())
            .putString(PREF_BIO_IV, cipher.iv.toBase64())
            .apply()
    }

    /**
     * Reads the DEK back. [cipher] must be one [getDecryptCipher] made, already carried through
     * an authenticated prompt.
     *
     * @return null when no wrapping is stored.
     */
    fun unwrapDekWithCipher(cipher: Cipher): SecretKey? {
        val wrapped = prefs.getString(PREF_WRAPPED_DEK_BIO, null)?.fromBase64() ?: return null
        return SecretKeySpec(cipher.doFinal(wrapped), KeyProperties.KEY_ALGORITHM_AES)
    }

    /** Whether this vault has a biometric door at all. */
    fun isBiometricWrapPresent(): Boolean = prefs.contains(PREF_WRAPPED_DEK_BIO)

    /** Drops the key and the wrapping together; leaving either behind would be a dangling half. */
    fun clearBiometric() {
        deleteKey()
        prefs.edit()
            .remove(PREF_WRAPPED_DEK_BIO)
            .remove(PREF_BIO_IV)
            .apply()
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    companion object {
        const val KEY_ALIAS = "servera_biometric_dek_key"

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREF_WRAPPED_DEK_BIO = "wrapped_dek_bio"
        private const val PREF_BIO_IV = "wrapped_dek_bio_iv"
        private const val KEY_SIZE_BITS = 256
        private const val TAG_SIZE_BITS = 128
    }
}
