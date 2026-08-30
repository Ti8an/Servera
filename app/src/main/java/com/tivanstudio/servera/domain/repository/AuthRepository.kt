package com.tivanstudio.servera.domain.repository

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

    fun isBiometricEnabled(): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean)
}
