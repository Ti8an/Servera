package com.tivanstudio.servera.presentation.console.result

import androidx.lifecycle.ViewModel
import com.tivanstudio.servera.di.CommandResultHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CommandResultViewModel @Inject constructor(
    resultHolder: CommandResultHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CommandResultUiState(
            result   = resultHolder.result,
            serverId = resultHolder.serverId
        )
    )
    val uiState: StateFlow<CommandResultUiState> = _uiState.asStateFlow()
}
