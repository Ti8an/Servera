package com.tivanstudio.servera.di

import javax.inject.Qualifier

/** The single EncryptedSharedPreferences instance holding the vault and auth state. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthPrefs
