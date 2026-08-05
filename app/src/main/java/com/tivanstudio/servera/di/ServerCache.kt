package com.tivanstudio.servera.di

import com.tivanstudio.servera.domain.entity.ServerInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerCache @Inject constructor() {

    private val statuses = mutableMapOf<Long, Boolean>()
    private val infos    = mutableMapOf<Long, ServerInfo>()

    fun statusOf(serverId: Long): Boolean? = statuses[serverId]

    fun putStatus(serverId: Long, isOnline: Boolean) {
        statuses[serverId] = isOnline
    }

    fun infoOf(serverId: Long): ServerInfo? = infos[serverId]

    fun putInfo(serverId: Long, info: ServerInfo) {
        infos[serverId] = info
    }
}
