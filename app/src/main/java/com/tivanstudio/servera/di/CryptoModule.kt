package com.tivanstudio.servera.di

import com.tivanstudio.servera.data.crypto.PasswordKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    /**
     * Production always gets the real store. Tests build [PasswordKeyManager] by hand with a
     * throwaway file name, so they never open -- let alone clear -- the user's vault.
     */
    @Provides
    @PrefsFileName
    fun providePrefsFileName(): String = PasswordKeyManager.DEFAULT_PREFS_FILE
}
