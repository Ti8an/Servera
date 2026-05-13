package com.tivanstudio.servera.domain.usecase.ssh

import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.repository.SshClient
import javax.inject.Inject

class TestConnectionUseCase @Inject constructor(
    private val sshClient: SshClient
) {
    suspend operator fun invoke(server: Server): Result<Boolean> =
        runCatching { sshClient.testConnection(server) }
}
