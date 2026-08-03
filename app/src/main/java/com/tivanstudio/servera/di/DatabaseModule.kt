package com.tivanstudio.servera.di

import android.content.Context
import androidx.room.Room
import com.tivanstudio.servera.data.db.AppDatabase
import com.tivanstudio.servera.data.db.MIGRATION_1_2
import com.tivanstudio.servera.data.db.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "servera.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideServerDao(db: AppDatabase) = db.serverDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase) = db.commandHistoryDao()

    @Provides
    fun provideQuickCommandDao(db: AppDatabase) = db.quickCommandDao()

    @Provides
    fun providePresetDao(db: AppDatabase) = db.presetDao()
}
