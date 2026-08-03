package com.tivanstudio.servera.presentation.presets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetSource
import com.tivanstudio.servera.domain.usecase.preset.AddCustomPresetUseCase
import com.tivanstudio.servera.domain.usecase.preset.DeleteCustomPresetUseCase
import com.tivanstudio.servera.domain.usecase.preset.GetPresetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PresetsViewModel @Inject constructor(
    private val getPresets: GetPresetsUseCase,
    private val addCustomPreset: AddCustomPresetUseCase,
    private val deleteCustomPreset: DeleteCustomPresetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PresetsUiState())
    val uiState: StateFlow<PresetsUiState> = _uiState.asStateFlow()

    init {
        observePresets()
    }

    private fun observePresets() {
        viewModelScope.launch {
            getPresets().collect { presets ->
                _uiState.update { it.copy(presets = presets) }
            }
        }
    }

    fun startAdd() {
        _uiState.update {
            it.copy(
                editing = Preset(
                    id = 0,
                    category = "",
                    label = "",
                    command = "",
                    source = PresetSource.CUSTOM,
                    sortOrder = 0
                ),
                isNew = true
            )
        }
    }

    fun startEdit(preset: Preset) {
        if (preset.source != PresetSource.CUSTOM) return
        _uiState.update { it.copy(editing = preset, isNew = false) }
    }

    fun copyBuiltinToCustom(preset: Preset) {
        _uiState.update {
            it.copy(
                editing = preset.copy(id = 0, source = PresetSource.CUSTOM),
                isNew = true
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(editing = null, isNew = false) }
    }

    fun savePreset(category: String, label: String, command: String) {
        val state = _uiState.value
        val editing = state.editing ?: return
        val trimmedCategory = category.trim()
        // New presets go to the end of their category; edits keep their position.
        val sortOrder =
            if (state.isNew) state.presets.count { it.category == trimmedCategory }
            else editing.sortOrder

        viewModelScope.launch {
            addCustomPreset(
                editing.copy(
                    category = trimmedCategory,
                    label = label.trim(),
                    command = command.trim(),
                    source = PresetSource.CUSTOM,
                    sortOrder = sortOrder
                )
            )
            dismissDialog()
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch { deleteCustomPreset(id) }
    }
}
