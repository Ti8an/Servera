package com.tivanstudio.servera.data.remote

import kotlinx.serialization.Serializable

/** Wire format of the `presets_catalog` Remote Config value. */
@Serializable
internal data class PresetCatalogDto(
    val version: Int = 0,
    val groups: List<GroupDto> = emptyList(),
    val presets: List<PresetDto> = emptyList()
)

@Serializable
internal data class GroupDto(
    val id: String,
    val name: String,
    val colorHex: String = DEFAULT_GROUP_COLOR,
    val sortOrder: Int = 0
)

@Serializable
internal data class PresetDto(
    val groupId: String,
    val label: String,
    val command: String,
    val sortOrder: Int = 0
)

internal const val DEFAULT_GROUP_COLOR = "#455A64"
