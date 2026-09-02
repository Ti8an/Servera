package com.tivanstudio.servera.di

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigModule {

    /**
     * Null when there is no FirebaseApp -- typically a build without `app/google-services.json`.
     * The built-in preset catalog is a nice-to-have, so an absent Firebase must degrade to an
     * empty catalog instead of failing injection for every screen that shows presets.
     */
    @Provides
    @Singleton
    fun provideRemoteConfig(): FirebaseRemoteConfig? =
        runCatching { Firebase.remoteConfig }
            .onFailure { Log.w("RemoteConfigModule", "Firebase unavailable; built-in presets disabled", it) }
            .getOrNull()
}
