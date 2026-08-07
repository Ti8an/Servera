package com.tivanstudio.servera.domain.entity

data class PresetGroup(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val sortOrder: Int
)
