package com.tivanstudio.servera.presentation.presets.viewmodel

import androidx.annotation.StringRes
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup

data class PresetsUiState(
    val presets: List<Preset> = emptyList(),
    val groups: List<PresetGroup> = emptyList(),
    val editing: Preset? = null,
    val isNew: Boolean = false,
    /** A catalog refresh is in flight; the toolbar action is disabled while it is. */
    val isUpdating: Boolean = false,
    /** One-shot Snackbar text for the last refresh or copy; cleared once shown. */
    @StringRes val updateMessageRes: Int? = null
) {
    /** Every group in sortOrder, with its own presets — empty groups included. */
    val grouped: List<Pair<PresetGroup, List<Preset>>>
        get() = groups
            .sortedBy { it.sortOrder }
            .map { group ->
                group to presets
                    .filter { it.groupId == group.id }
                    .sortedBy { it.sortOrder }
            }

    fun groupOf(groupId: Long): PresetGroup? = groups.firstOrNull { it.id == groupId }
}
