package com.tivanstudio.servera.di

import com.tivanstudio.servera.data.repository.AuthRepositoryImpl
import com.tivanstudio.servera.data.repository.CommandHistoryRepositoryImpl
import com.tivanstudio.servera.data.repository.QuickCommandRepositoryImpl
import com.tivanstudio.servera.data.repository.ServerRepositoryImpl
import com.tivanstudio.servera.domain.repository.AuthRepository
import com.tivanstudio.servera.domain.repository.CommandHistoryRepository
import com.tivanstudio.servera.domain.repository.QuickCommandRepository
import com.tivanstudio.servera.domain.repository.ServerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindServerRepo(impl: ServerRepositoryImpl): ServerRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepo(impl: CommandHistoryRepositoryImpl): CommandHistoryRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepo(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindQuickCmdRepo(impl: QuickCommandRepositoryImpl): QuickCommandRepository
}
