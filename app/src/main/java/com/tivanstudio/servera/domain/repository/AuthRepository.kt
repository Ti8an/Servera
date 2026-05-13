package com.tivanstudio.servera.domain.repository

interface AuthRepository {
    suspend fun setPassword(password: String)
    suspend fun verifyPassword(password: String): Boolean
    suspend fun isPasswordSet(): Boolean
    fun isBiometricEnabled(): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean)
}
