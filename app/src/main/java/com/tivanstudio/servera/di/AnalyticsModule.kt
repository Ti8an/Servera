package com.tivanstudio.servera.di

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.tivanstudio.servera.data.analytics.FirebaseAnalyticsImpl
import com.tivanstudio.servera.domain.analytics.Analytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalytics(impl: FirebaseAnalyticsImpl): Analytics

    companion object {

        /**
         * Null when there is no FirebaseApp -- typically a build without `app/google-services.json`.
         * Usage reporting is a nice-to-have, so an absent Firebase must degrade to a no-op instead
         * of failing injection for every screen.
         */
        @Provides
        @Singleton
        fun provideFirebaseAnalytics(): FirebaseAnalytics? =
            runCatching {
                Firebase.analytics.apply { setAnalyticsCollectionEnabled(true) }
            }
                .onFailure { Log.w("AnalyticsModule", "Firebase unavailable; analytics disabled", it) }
                .getOrNull()
    }
}
