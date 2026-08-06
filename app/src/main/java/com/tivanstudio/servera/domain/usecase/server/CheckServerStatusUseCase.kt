package com.tivanstudio.servera.domain.usecase.server

import com.tivanstudio.servera.domain.repository.ServerRepository
import com.tivanstudio.servera.domain.repository.SshClient
import javax.inject.Inject

class CheckServerStatusUseCase @Inject constructor(
    private val serverRepository: ServerRepository,
    private val sshClient: SshClient
) {
    suspend operator fun invoke(serverId: Long): Result<Unit> {
        val server = runCatching { serverRepository.getServerById(serverId) }
            .getOrElse { return Result.failure(it) }
            ?: return Result.failure(IllegalStateException("Server not found"))
        return sshClient.checkConnection(server)
    }
}
