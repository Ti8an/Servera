package com.tivanstudio.servera.data.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.tivanstudio.servera.domain.entity.CommandResult
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo
import com.tivanstudio.servera.domain.entity.SshErrorType
import com.tivanstudio.servera.domain.entity.SshException
import com.tivanstudio.servera.domain.repository.SshClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class SshClientImpl @Inject constructor() : SshClient {

    private data class ExecResult(val stdout: String, val stderr: String, val exitCode: Int)

    /**
     * JSch reports nearly everything as [JSchException] with the real reason nested or
     * only spelled out in the message, so both the cause chain and the text are inspected.
     */
    private fun mapSshError(e: Throwable): SshException {
        if (e is SshException) return e

        // Bounded so a self-referencing cause cannot spin forever.
        val chain = generateSequence(e) { it.cause }.take(8).toList()

        val typed = chain.firstNotNullOfOrNull { cause ->
            when (cause) {
                is UnknownHostException   -> SshErrorType.HOST_NOT_FOUND
                is SocketTimeoutException -> SshErrorType.TIMEOUT
                is ConnectException       -> SshErrorType.UNREACHABLE
                else                      -> null
            }
        }
        if (typed != null) return SshException(typed, e)

        val text = chain.mapNotNull { it.message }.joinToString(" ").lowercase()
        val type = when {
            "unknownhostexception" in text || "unknown host" in text -> SshErrorType.HOST_NOT_FOUND
            "timeout" in text || "timed out" in text                 -> SshErrorType.TIMEOUT
            "connection refused" in text                             -> SshErrorType.UNREACHABLE
            "auth" in text || "password" in text                     -> SshErrorType.AUTH
            else                                                     -> SshErrorType.UNKNOWN
        }
        return SshException(type, e)
    }

    private fun createSession(server: Server): Session {
        val jsch = JSch()
        server.privateKey?.let { jsch.addIdentity("key", it.toByteArray(), null, null) }
        return jsch.getSession(server.login, server.host, server.port).apply {
            if (server.privateKey == null) setPassword(server.password)
            setConfig("StrictHostKeyChecking", "no")
            connect(server.timeout * 1000)
        }
    }

    private fun runOnSession(session: Session, command: String): ExecResult {
        val channel = (session.openChannel("exec") as ChannelExec).apply {
            setCommand(command)
            connect()
        }
        val stdout = channel.inputStream.bufferedReader().readText()
        val stderr = channel.errStream.bufferedReader().readText()
        while (!channel.isClosed) Thread.sleep(50)
        val exitCode = channel.exitStatus
        channel.disconnect()
        return ExecResult(stdout, stderr, exitCode)
    }

    override suspend fun execute(server: Server, command: String): CommandResult =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            val session = createSession(server)
            try {
                val result = runOnSession(session, command)
                CommandResult(
                    command = command,
                    stdout = result.stdout,
                    stderr = result.stderr,
                    exitCode = result.exitCode,
                    durationMs = System.currentTimeMillis() - start
                )
            } finally {
                session.disconnect()
            }
        }

    override suspend fun testConnection(server: Server): Boolean =
        runCatching { execute(server, "echo ok") }.isSuccess

    override suspend fun checkConnection(server: Server): Result<Unit> =
        runCatching { execute(server, "echo ok"); Unit }
            .recoverCatching { throw mapSshError(it) }

    override suspend fun fetchServerInfo(server: Server): ServerInfo =
        withContext(Dispatchers.IO) {
            var session: Session? = null
            try {
                val open = createSession(server).also { session = it }
                fun run(cmd: String) = runOnSession(open, cmd).stdout.trim()
                ServerInfo(
                    hostname  = run("hostname"),
                    os        = run("cat /etc/os-release | grep PRETTY_NAME | cut -d= -f2 | tr -d '\"'"),
                    cpuInfo   = run("grep 'model name' /proc/cpuinfo | head -1 | cut -d: -f2 | xargs"),
                    ramTotal  = run("free -h | awk '/^Mem:/{print \$2}'"),
                    ramFree   = run("free -h | awk '/^Mem:/{print \$4}'"),
                    diskUsage = run("df -h / | awk 'NR==2{print \$3\"/\"\$2\" (\"\$5\")\"}'" ),
                    uptime    = run("uptime -p")
                )
            } catch (e: Exception) {
                throw mapSshError(e)
            } finally {
                session?.disconnect()
            }
        }
}
