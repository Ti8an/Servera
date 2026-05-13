package com.tivanstudio.servera.domain.entity

data class QuickCommand(
    val id: Long = 0,
    val label: String,
    val command: String,
    val sortOrder: Int
)
