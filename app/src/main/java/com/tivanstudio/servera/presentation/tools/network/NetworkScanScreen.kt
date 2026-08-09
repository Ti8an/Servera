package com.tivanstudio.servera.presentation.tools.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.entity.NetworkDevice
import com.tivanstudio.servera.presentation.theme.*

@Composable
fun NetworkScanScreen(
    viewModel: NetworkScanViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NetworkScanContent(
        uiState = uiState,
        onBack  = onBack,
        onScan  = viewModel::startScan
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkScanContent(
    uiState: NetworkScanUiState,
    onBack: () -> Unit,
    onScan: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.net_scan_title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = onScan,
                enabled  = !uiState.isScanning,
                colors   = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape    = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.net_scan_button), fontWeight = FontWeight.Bold)
            }

            uiState.subnet?.let { subnet ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = stringResource(R.string.net_subnet, subnet),
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            if (uiState.isScanning) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    color    = PrimaryGreen,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = "${uiState.scanned}/${uiState.total}",
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            uiState.error?.let { res ->
                Spacer(Modifier.height(8.dp))
                Text(text = stringResource(res), color = DangerRed, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

            if (uiState.devices.isEmpty() && uiState.hasScanned && !uiState.isScanning) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.net_nothing_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding      = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.devices.filter { it.ip.isNotBlank() }, key = { it.ip }) { device ->
                        DeviceCard(device = device)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(device: NetworkDevice) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = device.ip,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                    modifier   = Modifier.weight(1f)
                )
                if (device.isSelf) {
                    DeviceTag(stringResource(R.string.net_self))
                }
                if (device.isGateway) {
                    if (device.isSelf) Spacer(Modifier.width(4.dp))
                    DeviceTag(stringResource(R.string.net_gateway))
                }
            }
            Text(
                text     = device.hostname ?: "—",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text  = device.mac ?: "—",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceTag(text: String) {
    Surface(
        color = PrimaryGreen.copy(alpha = 0.18f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.labelSmall,
            color    = PrimaryGreen,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkScanContentPreview() {
    ServeraTheme {
        NetworkScanContent(
            uiState = NetworkScanUiState(
                subnet     = "192.168.1.0/24",
                hasScanned = true,
                devices = listOf(
                    NetworkDevice("192.168.1.1", "router.lan", "a4:2b:8c:00:11:22", isGateway = true),
                    NetworkDevice("192.168.1.42", "pixel-7", null, isSelf = true),
                    NetworkDevice("192.168.1.100", null, null)
                )
            ),
            onBack = {},
            onScan = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkScanScanningPreview() {
    ServeraTheme {
        NetworkScanContent(
            uiState = NetworkScanUiState(
                isScanning = true,
                progress   = 0.4f,
                scanned    = 102,
                subnet     = "192.168.1.0/24"
            ),
            onBack = {},
            onScan = {}
        )
    }
}
