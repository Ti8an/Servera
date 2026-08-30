package com.tivanstudio.servera.di

import javax.inject.Qualifier

/** Names the EncryptedSharedPreferences file the crypto state lives in. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PrefsFileName
