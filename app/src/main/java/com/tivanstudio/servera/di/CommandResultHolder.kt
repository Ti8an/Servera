package com.tivanstudio.servera.di

import com.tivanstudio.servera.domain.entity.CommandResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hand-off between the screen that ran (or recalled) a command and the result screen.
 * [result] is null when the output was never stored — the metadata below still describes
 * the run, so the result screen can show everything except the output itself.
 */
@Singleton
class CommandResultHolder @Inject constructor() {
    var result: CommandResult? = null
    var serverId: Long = -1L
    var serverName: String? = null
    var serverHost: String? = null
    var groupName: String? = null
    var command: String? = null
    var exitCode: Int? = null
    var outputSaved: Boolean = true

    fun clear() {
        result = null
        serverId = -1L
        serverName = null
        serverHost = null
        groupName = null
        command = null
        exitCode = null
        outputSaved = true
    }
}
