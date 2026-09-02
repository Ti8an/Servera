package com.tivanstudio.servera.di

import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the Data Encryption Key (DEK) of the currently unlocked session.
 * The DEK lives only in memory and is dropped on lock or process death.
 */
@Singleton
class SessionKeyHolder @Inject constructor() {

    @Volatile
    var dek: SecretKey? = null

    fun isUnlocked(): Boolean = dek != null

    fun clear() {
        dek = null
    }
}
