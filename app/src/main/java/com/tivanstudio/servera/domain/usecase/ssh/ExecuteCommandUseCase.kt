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
        groupName: String? = null,
        /** Keep stdout/stderr in history; when false only the run itself is recorded. */
        saveResult: Boolean = false
    ): Result<CommandResult> {
        val result = runCatching { sshClient.execute(server, command) }

        if (result.isSuccess) {
            val cmdResult = result.getOrThrow()
            historyRepository.saveHistory(
                CommandHistory(
                    serverId    = server.id,
                    command     = command,
                    stdout      = if (saveResult) cmdResult.stdout else "",
                    stderr      = if (saveResult) cmdResult.stderr else "",
                    exitCode    = cmdResult.exitCode,
                    executedAt  = System.currentTimeMillis(),
                    groupName   = groupName,
                    resultSaved = saveResult
                )
            )
        } else if (saveOnFailure) {
            historyRepository.saveHistory(
                CommandHistory(
                    serverId    = server.id,
                    command     = command,
                    stdout      = "",
                    stderr      = if (saveResult) result.exceptionOrNull()?.message ?: "Unknown error" else "",
                    exitCode    = -1,
                    executedAt  = System.currentTimeMillis(),
                    groupName   = groupName,
                    resultSaved = saveResult
                )
            )
        }

        return result
    }
}
