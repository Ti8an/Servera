package com.tivanstudio.servera.domain.usecase.ssh

import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo
import com.tivanstudio.servera.domain.repository.SshClient
import javax.inject.Inject

class FetchServerInfoUseCase @Inject constructor(
    private val sshClient: SshClient
) {
    suspend operator fun invoke(server: Server): Result<ServerInfo> =
        runCatching { sshClient.fetchServerInfo(server) }
}
