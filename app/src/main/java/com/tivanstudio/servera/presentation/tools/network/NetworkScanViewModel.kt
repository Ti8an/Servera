package com.tivanstudio.servera.presentation.tools.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivanstudio.servera.R
import com.tivanstudio.servera.data.network.NetworkScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkScanViewModel @Inject constructor(
    private val scanner: NetworkScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkScanUiState())
    val uiState: StateFlow<NetworkScanUiState> = _uiState.asStateFlow()

    fun startScan() {
        if (_uiState.value.isScanning) return

        val subnet = scanner.currentSubnet()
        if (subnet == null) {
            _uiState.update { it.copy(error = R.string.net_no_wifi, isScanning = false) }
            return
        }

        _uiState.update {
            it.copy(
                isScanning = true,
                error      = null,
                progress   = 0f,
                scanned    = 0,
                devices    = emptyList(),
                subnet     = subnet.base + "0/24"
            )
        }

        viewModelScope.launch {
            val devices = scanner.scan { scanned, total ->
                _uiState.update {
                    it.copy(
                        scanned  = scanned,
                        total    = total,
                        progress = scanned.toFloat() / total
                    )
                }
            }
            _uiState.update {
                it.copy(
                    devices    = devices,
                    isScanning = false,
                    progress   = 1f,
                    hasScanned = true
                )
            }
        }
    }
}
