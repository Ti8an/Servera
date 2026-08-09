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
    suspend operator fun invoke(
        server: Server,
        command: String,
        saveOnFailure: Boolean = false,
        /** Group the command was attached to; null for a command typed by hand. */
        groupName: String? = null
    ): Result<CommandResult> {
        val result = runCatching { sshClient.execute(server, command) }

        if (result.isSuccess) {
            val cmdResult = result.getOrThrow()
            historyRepository.saveHistory(
                CommandHistory(
                    serverId    = server.id,
                    command     = command,
                    stdout      = cmdResult.stdout,
                    stderr      = cmdResult.stderr,
                    exitCode    = cmdResult.exitCode,
                    executedAt  = System.currentTimeMillis(),
                    groupName   = groupName
                )
            )
        } else if (saveOnFailure) {
            historyRepository.saveHistory(
                CommandHistory(
                    serverId    = server.id,
                    command     = command,
                    stdout      = "",
                    stderr      = result.exceptionOrNull()?.message ?: "Unknown error",
                    exitCode    = -1,
                    executedAt  = System.currentTimeMillis(),
                    groupName   = groupName
                )
            )
        }

        return result
    }
}
