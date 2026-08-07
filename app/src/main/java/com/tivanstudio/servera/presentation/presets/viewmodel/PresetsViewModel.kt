package com.tivanstudio.servera.presentation.presets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.usecase.preset.AddPresetUseCase
import com.tivanstudio.servera.domain.usecase.preset.DeletePresetUseCase
import com.tivanstudio.servera.domain.usecase.preset.GetGroupsUseCase
import com.tivanstudio.servera.domain.usecase.preset.GetPresetsUseCase
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
    private val deletePreset: DeletePresetUseCase
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

    /** Adding needs a group to put the preset in; defaults to the first one. */
    fun startAdd() {
        val state = _uiState.value
        val group = state.groups.minByOrNull { it.sortOrder } ?: return
        _uiState.update {
            it.copy(
                editing = Preset(
                    id        = 0,
                    groupId   = group.id,
                    label     = "",
                    command   = "",
                    sortOrder = 0
                ),
                isNew = true
            )
        }
    }

    fun startEdit(preset: Preset) {
        _uiState.update { it.copy(editing = preset, isNew = false) }
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
            dismissDialog()
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch { deletePreset.invoke(id) }
    }
}
