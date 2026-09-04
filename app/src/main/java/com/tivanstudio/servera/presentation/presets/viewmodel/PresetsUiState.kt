package com.tivanstudio.servera.presentation.presets.viewmodel

import androidx.annotation.StringRes
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.domain.entity.PresetSource

data class PresetsUiState(
    val presets: List<Preset> = emptyList(),
    val groups: List<PresetGroup> = emptyList(),
    val editing: Preset? = null,
    val isNew: Boolean = false,
    /** The small "where does the new command come from" dialog is open. */
    val showSourceChooser: Boolean = false,
    /** The full-screen built-in catalog is open. */
    val showLibrary: Boolean = false,
    /** A catalog refresh is in flight; the toolbar action is disabled while it is. */
    val isUpdating: Boolean = false,
    /** One-shot Snackbar text for the last refresh or copy; cleared once shown. */
    @StringRes val updateMessageRes: Int? = null
) {
    /**
     * The grid: the user's own groups with their own presets, empty groups included. The built-in
     * catalog is not part of it any more — it lives behind the library dialog.
     */
    val grouped: List<Pair<PresetGroup, List<Preset>>>
        get() = groupedBy(PresetSource.CUSTOM)

    /** The same shape for the library dialog, over the built-in catalog. */
    val builtinGrouped: List<Pair<PresetGroup, List<Preset>>>
        get() = groupedBy(PresetSource.BUILTIN)

    /** Commands the user already has, so the library can mark the rows that are already in. */
    val customCommandStrings: Set<String>
        get() = presets
            .filter { it.source == PresetSource.CUSTOM }
            .map { it.command }
            .toSet()

    fun groupOf(groupId: Long): PresetGroup? = groups.firstOrNull { it.id == groupId }

    private fun groupedBy(source: PresetSource): List<Pair<PresetGroup, List<Preset>>> =
        groups
            .filter { it.source == source }
            .sortedBy { it.sortOrder }
            .map { group ->
                group to presets
                    .filter { it.groupId == group.id && it.source == source }
                    .sortedBy { it.sortOrder }
            }
}
