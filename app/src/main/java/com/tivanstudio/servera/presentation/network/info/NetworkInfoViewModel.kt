package com.tivanstudio.servera.presentation.network.info

import androidx.lifecycle.ViewModel
import com.tivanstudio.servera.data.network.NetworkScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class NetworkInfoViewModel @Inject constructor(
    private val scanner: NetworkScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkInfoUiState(scanner.currentDetails()))
    val uiState: StateFlow<NetworkInfoUiState> = _uiState.asStateFlow()

    /**
     * These are a snapshot, not a subscription: the phone can hop networks while the screen
     * is open, and nothing here would notice until asked again.
     */
    fun refresh() {
        _uiState.update { it.copy(details = scanner.currentDetails()) }
    }
}
