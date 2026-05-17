package com.tivanstudio.servera.di

import com.tivanstudio.servera.domain.entity.CommandResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandResultHolder @Inject constructor() {
    var result: CommandResult? = null
    var serverId: Long = -1L
}
