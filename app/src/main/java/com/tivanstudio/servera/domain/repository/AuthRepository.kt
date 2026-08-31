package com.tivanstudio.servera.domain.repository

import com.tivanstudio.servera.data.crypto.SecurityLevel
import javax.crypto.Cipher

interface AuthRepository {
    /** Initializes the vault: creates the DEK, wraps it with [password] and unlocks the session. */
    suspend fun setPassword(password: String)

    /** Unwraps the DEK with [password]. Returns true (and unlocks the session) only on success. */
    suspend fun verifyPassword(password: String): Boolean

    /** Re-wraps the DEK under [newPassword]. Returns false when [oldPassword] is wrong. */
    suspend fun changePassword(oldPassword: String, newPassword: String): Boolean

    suspend fun isPasswordSet(): Boolean

    /** Drops the DEK from memory. */
    fun lock()

    /**
     * Forgotten-password path: wipes the wrapped DEK together with every piece of user data.
     * There is no recovery -- the old data is unreadable once its DEK is gone. After this the
     * app is back to first-launch state and a new password creates a brand new DEK.
     */
    suspend fun resetAll()

    /**
     * True only when the flag and the biometric wrapping of the DEK agree. A new enrollment can
     * wipe the wrapping behind the app's back, and a switch that still reads "on" then would be
     * a lie.
     */
    fun isBiometricEnabled(): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean)

    /**
     * Makes a fresh BEK and hands back the cipher to put in a BiometricPrompt.CryptoObject. The
     * wrapping itself is [finishEnableBiometric]'s job, once the prompt has released the key.
     *
     * @return null when the session is locked -- there is no DEK to wrap.
     */
    fun getBiometricEncryptCipher(): Cipher?

    /**
     * Wraps the session DEK under the BEK and turns the flag on. [cipher] must be the one the
     * prompt returned in its result, not the one [getBiometricEncryptCipher] handed out.
     */
    suspend fun finishEnableBiometric(cipher: Cipher): Result<Unit>

    /** Drops the BEK, the wrapping and the flag together. */
    suspend fun disableBiometric()

    /**
     * The cipher to put in a BiometricPrompt.CryptoObject for unlocking.
     *
     * @return null when there is no wrapping, or when a new enrollment invalidated the BEK --
     * the stale wrapping is cleaned up on the way out. Either answer means the same to the
     * caller: this unlock has to go through the password.
     */
    fun getBiometricDecryptCipher(): Cipher?

    /**
     * Unwraps the DEK with [cipher] and unlocks the session. No PBKDF2 anywhere -- the work
     * factor was paid when the password wrapped the same DEK. [cipher] must be the one the
     * prompt returned in its result.
     *
     * @return false when the unwrap fails; the session is left locked.
     */
    suspend fun unlockWithBiometricCipher(cipher: Cipher): Boolean

    /** The work factor the vault is currently wrapped at. */
    fun getSecurityLevel(): SecurityLevel

    /** True while the vault sits above the default level. */
    fun isEnhancedEnabled(): Boolean

    /**
     * Re-wraps the DEK at [level]. [password] is needed to unwrap it first, so a wrong one
     * fails and leaves the vault exactly as it was. Runs two PBKDF2 derivations back to back.
     */
    suspend fun changeSecurityLevel(level: SecurityLevel, password: String): Result<Unit>
}
