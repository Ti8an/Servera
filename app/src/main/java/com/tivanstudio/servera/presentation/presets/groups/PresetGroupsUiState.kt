package com.tivanstudio.servera.presentation.presets.groups

import com.tivanstudio.servera.domain.entity.PresetGroup

data class PresetGroupsUiState(
    val groups: List<PresetGroup> = emptyList(),
    val editing: PresetGroup? = null,
    val isNew: Boolean = false,
    /** How many presets each group holds — drives the "presets will be deleted" warning. */
    val presetCounts: Map<Long, Int> = emptyMap()
) {
    fun presetCountOf(groupId: Long): Int = presetCounts[groupId] ?: 0
}
