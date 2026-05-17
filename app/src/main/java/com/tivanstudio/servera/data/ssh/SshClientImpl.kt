package com.tivanstudio.servera.data.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.tivanstudio.servera.domain.entity.CommandResult
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo
import com.tivanstudio.servera.domain.repository.SshClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SshClientImpl @Inject constructor() : SshClient {

    private data class ExecResult(val stdout: String, val stderr: String, val exitCode: Int)

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

    override suspend fun fetchServerInfo(server: Server): ServerInfo =
        withContext(Dispatchers.IO) {
            val session = createSession(server)
            try {
                fun run(cmd: String) = runOnSession(session, cmd).stdout.trim()
                ServerInfo(
                    hostname  = run("hostname"),
                    os        = run("cat /etc/os-release | grep PRETTY_NAME | cut -d= -f2 | tr -d '\"'"),
                    cpuInfo   = run("grep 'model name' /proc/cpuinfo | head -1 | cut -d: -f2 | xargs"),
                    ramTotal  = run("free -h | awk '/^Mem:/{print \$2}'"),
                    ramFree   = run("free -h | awk '/^Mem:/{print \$4}'"),
                    diskUsage = run("df -h / | awk 'NR==2{print \$3\"/\"\$2\" (\"\$5\")\"}'" ),
                    uptime    = run("uptime -p")
                )
            } finally {
                session.disconnect()
            }
        }
}
