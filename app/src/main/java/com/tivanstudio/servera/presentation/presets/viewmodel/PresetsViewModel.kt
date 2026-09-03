package com.tivanstudio.servera.presentation.presets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.analytics.Analytics
import com.tivanstudio.servera.domain.analytics.AnalyticsEvent
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetSource
import com.tivanstudio.servera.domain.usecase.preset.AddPresetUseCase
import com.tivanstudio.servera.domain.usecase.preset.CopyBuiltinToCustomUseCase
import com.tivanstudio.servera.domain.usecase.preset.DeletePresetUseCase
import com.tivanstudio.servera.domain.usecase.preset.GetGroupsUseCase
import com.tivanstudio.servera.domain.usecase.preset.GetPresetsUseCase
import com.tivanstudio.servera.domain.usecase.preset.UpdatePresetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PresetsViewModel @Inject constructor(
    private val getPresets: GetPresetsUseCase,
    private val getGroups: GetGroupsUseCase,
    private val addPreset: AddPresetUseCase,
    private val deletePreset: DeletePresetUseCase,
    private val updatePresets: UpdatePresetsUseCase,
    private val copyBuiltinToCustom: CopyBuiltinToCustomUseCase,
    private val analytics: Analytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(PresetsUiState())
    val uiState: StateFlow<PresetsUiState> = _uiState.asStateFlow()

    init {
        observeCatalog()
    }

    private fun observeCatalog() {
        viewModelScope.launch {
            combine(getPresets(), getGroups()) { presets, groups -> presets to groups }
                .collect { (presets, groups) ->
                    _uiState.update { it.copy(presets = presets, groups = groups) }
                }
        }
    }

    /** The add tile sits inside a group's own section, so the caller already knows where it goes. */
    fun startAdd(groupId: Long) {
        val state = _uiState.value
        if (state.groups.none { it.id == groupId }) return
        _uiState.update {
            it.copy(
                editing = Preset(
                    id        = 0,
                    groupId   = resolveTargetGroupId(groupId) ?: NO_GROUP,
                    label     = "",
                    command   = "",
                    sortOrder = 0
                ),
                isNew = true
            )
        }
    }

    /** Built-ins are read-only: the UI hides the affordance, this is the backstop. */
    fun startEdit(preset: Preset) {
        if (preset.source == PresetSource.BUILTIN) return
        _uiState.update { it.copy(editing = preset, isNew = false) }
    }

    /**
     * Opens the dialog on a fresh copy of a built-in so it can be adjusted before it becomes one
     * of the user's own presets.
     */
    fun startCopy(preset: Preset) {
        _uiState.update {
            it.copy(
                editing = preset.copy(
                    id        = 0,
                    groupId   = resolveTargetGroupId(preset.groupId) ?: NO_GROUP,
                    sortOrder = 0
                ),
                isNew = true
            )
        }
    }

    /**
     * The group a preset started in, translated into one it can actually be saved to.
     *
     * Built-in groups are catalog entries with no row in `preset_groups`, and `presets.groupId` is
     * a foreign key onto it, so one can never be the target: fall back to the user's group of the
     * same name, the rule the repository copy already follows. Null when there is nothing to fall
     * back to, which leaves the dialog to ask for a group.
     */
    private fun resolveTargetGroupId(sourceGroupId: Long): Long? {
        val groups = _uiState.value.groups
        val source = groups.firstOrNull { it.id == sourceGroupId } ?: return null
        if (source.source == PresetSource.CUSTOM) return source.id
        return groups.firstOrNull {
            it.source == PresetSource.CUSTOM && it.name.equals(source.name, ignoreCase = true)
        }?.id
    }

    fun dismissDialog() {
        _uiState.update { it.copy(editing = null, isNew = false) }
    }

    fun savePreset(groupId: Long, label: String, command: String) {
        val state = _uiState.value
        val editing = state.editing ?: return
        if (label.isBlank() || command.isBlank()) return
        if (state.groups.none { it.id == groupId }) return

        // New presets land at the end of their group; edits keep their position,
        // unless they were moved to another group.
        val movedToOtherGroup = !state.isNew && editing.groupId != groupId
        val sortOrder =
            if (state.isNew || movedToOtherGroup) state.presets.count { it.groupId == groupId }
            else editing.sortOrder

        viewModelScope.launch {
            addPreset(
                editing.copy(
                    groupId   = groupId,
                    label     = label.trim(),
                    command   = command.trim(),
                    sortOrder = sortOrder
                )
            )
            // savePreset also serves edits; only a brand new row is a creation.
            if (state.isNew) analytics.log(AnalyticsEvent.PresetCreated)
            dismissDialog()
        }
    }

    fun deletePreset(id: Long) {
        // Built-in rows carry catalog-derived ids that no Room row owns; deleting one would be a
        // silent no-op, so refuse it outright.
        if (_uiState.value.presets.any { it.id == id && it.source == PresetSource.BUILTIN }) return
        viewModelScope.launch { deletePreset.invoke(id) }
    }

    /**
     * Pulls a fresh built-in catalog from Remote Config. The list itself refreshes through the
     * repository's catalogRevision flow, so this only has to report the outcome.
     */
    fun refresh() {
        if (_uiState.value.isUpdating) return
        _uiState.update { it.copy(isUpdating = true, updateMessageRes = null) }
        viewModelScope.launch {
            val messageRes = updatePresets().fold(
                {
                    analytics.log(AnalyticsEvent.PresetsUpdated)
                    R.string.presets_updated
                },
                { R.string.presets_update_failed }
            )
            _uiState.update { it.copy(isUpdating = false, updateMessageRes = messageRes) }
        }
    }

    /** Forks a built-in preset into an editable copy of the user's own. */
    fun copyToCustom(preset: Preset) {
        viewModelScope.launch {
            copyBuiltinToCustom(preset)
            analytics.log(AnalyticsEvent.PresetCopiedToCustom)
            _uiState.update { it.copy(updateMessageRes = R.string.preset_copied) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(updateMessageRes = null) }
    }

    private companion object {
        /** Room ids start at 1, so zero reads as "no group chosen yet" and matches nothing. */
        const val NO_GROUP = 0L
    }
}
