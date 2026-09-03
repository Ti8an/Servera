package com.tivanstudio.servera.di

import android.content.Context
import androidx.room.Room
import com.tivanstudio.servera.data.db.AppDatabase
import com.tivanstudio.servera.data.db.MIGRATION_1_2
import com.tivanstudio.servera.data.db.MIGRATION_2_3
import com.tivanstudio.servera.data.db.MIGRATION_3_4
import com.tivanstudio.servera.data.db.MIGRATION_4_5
import com.tivanstudio.servera.data.db.MIGRATION_5_6
import com.tivanstudio.servera.data.db.MIGRATION_6_7
import com.tivanstudio.servera.data.db.MIGRATION_7_8
import com.tivanstudio.servera.data.db.MIGRATION_8_9
import com.tivanstudio.servera.data.db.MIGRATION_9_10
import com.tivanstudio.servera.data.db.MIGRATION_10_11
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
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                MIGRATION_10_11
            )
            .build()

    @Provides
    fun provideServerDao(db: AppDatabase) = db.serverDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase) = db.commandHistoryDao()

    @Provides
    fun provideQuickCommandDao(db: AppDatabase) = db.quickCommandDao()

    @Provides
    fun providePresetDao(db: AppDatabase) = db.presetDao()

    @Provides
    fun providePresetGroupDao(db: AppDatabase) = db.presetGroupDao()
}
