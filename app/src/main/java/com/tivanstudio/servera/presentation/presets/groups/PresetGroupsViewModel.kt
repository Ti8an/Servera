package com.tivanstudio.servera.presentation.presets.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.domain.usecase.preset.AddGroupUseCase
import com.tivanstudio.servera.domain.usecase.preset.DeleteGroupUseCase
import com.tivanstudio.servera.domain.usecase.preset.GetGroupsUseCase
import com.tivanstudio.servera.domain.usecase.preset.GetPresetsUseCase
import com.tivanstudio.servera.domain.usecase.preset.UpdateGroupUseCase
import com.tivanstudio.servera.presentation.theme.PresetColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PresetGroupsViewModel @Inject constructor(
    private val getGroups: GetGroupsUseCase,
    private val getPresets: GetPresetsUseCase,
    private val addGroup: AddGroupUseCase,
    private val updateGroup: UpdateGroupUseCase,
    private val deleteGroup: DeleteGroupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PresetGroupsUiState())
    val uiState: StateFlow<PresetGroupsUiState> = _uiState.asStateFlow()

    init {
        observeGroups()
        observePresetCounts()
    }

    private fun observeGroups() {
        viewModelScope.launch {
            getGroups().collect { groups ->
                _uiState.update { it.copy(groups = groups) }
            }
        }
    }

    private fun observePresetCounts() {
        viewModelScope.launch {
            getPresets().collect { presets ->
                val counts = presets.groupingBy { it.groupId }.eachCount()
                _uiState.update { it.copy(presetCounts = counts) }
            }
        }
    }

    fun startAdd() {
        _uiState.update { state ->
            state.copy(
                editing = PresetGroup(
                    id        = 0,
                    name      = "",
                    colorHex  = PresetColors.first(),
                    sortOrder = state.groups.size
                ),
                isNew = true
            )
        }
    }

    fun startEdit(group: PresetGroup) {
        _uiState.update { it.copy(editing = group, isNew = false) }
    }

    fun dismiss() {
        _uiState.update { it.copy(editing = null, isNew = false) }
    }

    fun save(name: String, colorHex: String) {
        val editing = _uiState.value.editing ?: return
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        val group = editing.copy(name = trimmed, colorHex = colorHex)
        viewModelScope.launch {
            if (group.id == 0L) addGroup(group) else updateGroup(group)
            dismiss()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { deleteGroup(id) }
    }
}
