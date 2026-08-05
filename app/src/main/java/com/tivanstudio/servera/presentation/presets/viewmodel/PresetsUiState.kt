package com.tivanstudio.servera.presentation.presets.viewmodel

import com.tivanstudio.servera.domain.entity.Preset

data class PresetsUiState(
    val presets: List<Preset> = emptyList(),
    val editing: Preset? = null,
    val isNew: Boolean = false
) {
    val grouped: Map<String, List<Preset>> get() = presets.groupBy { it.category }
    val categories: List<String> get() = presets.map { it.category }.distinct().sorted()
}
