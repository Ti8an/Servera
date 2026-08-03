package com.tivanstudio.servera.domain.entity

enum class PresetSource { BUILTIN, CUSTOM }

data class Preset(
    val id: Long = 0,
    val category: String,
    val label: String,
    val command: String,
    val source: PresetSource,
    val sortOrder: Int
)
