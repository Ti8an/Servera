package com.tivanstudio.servera.di

import com.tivanstudio.servera.data.ssh.SshClientImpl
import com.tivanstudio.servera.domain.repository.SshClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SshModule {

    @Binds
    @Singleton
    abstract fun bindSshClient(impl: SshClientImpl): SshClient
}
