package com.tivanstudio.servera.domain.usecase.server

import com.tivanstudio.servera.domain.repository.ServerRepository
import com.tivanstudio.servera.domain.repository.SshClient
import javax.inject.Inject

class CheckServerStatusUseCase @Inject constructor(
    private val serverRepository: ServerRepository,
    private val sshClient: SshClient
) {
    suspend operator fun invoke(serverId: Long): Boolean = runCatching {
        val server = serverRepository.getServerById(serverId) ?: return false
        sshClient.testConnection(server)
    }.getOrDefault(false)
}
