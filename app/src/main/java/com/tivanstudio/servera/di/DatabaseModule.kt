package com.tivanstudio.servera.di

import android.content.Context
import androidx.room.Room
import com.tivanstudio.servera.data.db.AppDatabase
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
        Room.databaseBuilder(ctx, AppDatabase::class.java, "servera.db").build()

    @Provides
    fun provideServerDao(db: AppDatabase) = db.serverDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase) = db.commandHistoryDao()

    @Provides
    fun provideQuickCommandDao(db: AppDatabase) = db.quickCommandDao()
}
