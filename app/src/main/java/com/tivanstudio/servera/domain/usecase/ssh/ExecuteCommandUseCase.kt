package com.tivanstudio.servera.domain.usecase.ssh

import com.tivanstudio.servera.domain.entity.CommandHistory
import com.tivanstudio.servera.domain.entity.CommandResult
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.repository.CommandHistoryRepository
import com.tivanstudio.servera.domain.repository.SshClient
import javax.inject.Inject

class ExecuteCommandUseCase @Inject constructor(
    private val sshClient: SshClient,
    private val historyRepository: CommandHistoryRepository
) {
    suspend operator fun invoke(server: Server, command: String): Result<CommandResult> =
        runCatching {
            val result = sshClient.execute(server, command)
            historyRepository.saveHistory(
                CommandHistory(
                    serverId = server.id,
                    command = command,
                    stdout = result.stdout,
                    stderr = result.stderr,
                    exitCode = result.exitCode,
                    executedAt = System.currentTimeMillis()
                )
            )
            result
        }
}
