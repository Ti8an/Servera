package com.tivanstudio.servera.data.crypto

import android.util.Base64
import com.tivanstudio.servera.di.SessionKeyHolder
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionHelper @Inject constructor(
    private val keystoreManager: KeystoreManager,
    private val session: SessionKeyHolder
) {
    fun encrypt(plain: String): String {
        val dek = session.dek ?: throw IllegalStateException("Vault locked")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, dek)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val dek = session.dek ?: throw IllegalStateException("Vault locked")
        return decryptWith(encoded, dek)
    }

    /**
     * Reads data written by the old Keystore-backed scheme.
     * Only [MigrationManager] should call this. Kept for one more release so a rollback can
     * still read the pre-V2 rows; drop it, and [KeystoreManager] with it, in the release after.
     */
    @Deprecated("Legacy pre-V2 path, used only by MigrationManager")
    fun decryptLegacy(encoded: String): String =
        decryptWith(encoded, keystoreManager.getOrCreateKey())

    private fun decryptWith(encoded: String, key: SecretKey): String {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 12)
        val data = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(data), Charsets.UTF_8)
    }
}
