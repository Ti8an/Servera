package com.tivanstudio.servera.domain.entity

data class QuickCommand(
    val id: Long = 0,
    val serverId: Long,
    val label: String,
    val command: String,
    val sortOrder: Int,
    val showOutput: Boolean = true,
    /** Snapshot of the catalog group this command came from; null for ungrouped ones. */
    val groupName: String? = null,
    val groupColorHex: String? = null
)
