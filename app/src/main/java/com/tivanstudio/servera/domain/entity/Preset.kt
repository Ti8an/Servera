package com.tivanstudio.servera.domain.entity

data class Preset(
    val id: Long = 0,
    val groupId: Long,
    val label: String,
    val command: String,
    val sortOrder: Int,
    val source: PresetSource = PresetSource.CUSTOM,
    /** Key into `PresetIcons`; null and unknown keys both fall back to the terminal glyph. */
    val iconKey: String? = null
)
