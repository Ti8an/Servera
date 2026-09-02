package com.tivanstudio.servera.domain.entity

data class Preset(
    val id: Long = 0,
    val groupId: Long,
    val label: String,
    val command: String,
    val sortOrder: Int
)
