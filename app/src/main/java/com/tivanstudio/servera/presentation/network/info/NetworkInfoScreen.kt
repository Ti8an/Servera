package com.tivanstudio.servera.presentation.network.info

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
import com.tivanstudio.servera.data.network.NetworkScanner
import com.tivanstudio.servera.presentation.theme.*

/** Shown in place of any value the system did not hand over. */
private const val NoValue = "—"

@Composable
fun NetworkInfoScreen(
    viewModel: NetworkInfoViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NetworkInfoContent(
        uiState   = uiState,
        onBack    = onBack,
        onRefresh = viewModel::refresh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkInfoContent(
    uiState: NetworkInfoUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.network_info_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.network_refresh)
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Above the rows on purpose: it is why the numbers below look the way they do,
            // not a footnote to them.
            if (uiState.details.isVpnActive) {
                VpnWarning()
                Spacer(Modifier.height(8.dp))
            }

            if (!uiState.hasConnection) {
                Text(
                    text     = stringResource(R.string.network_no_connection),
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                DetailRow(
                    label = stringResource(R.string.network_transport),
                    value = stringResource(uiState.details.transport.labelRes())
                )
                DetailRow(
                    label = stringResource(R.string.network_local_ip),
                    value = uiState.details.localIp
                )
                DetailRow(
                    label = stringResource(R.string.network_subnet),
                    value = uiState.subnet
                )
                DetailRow(
                    label = stringResource(R.string.network_gateway),
                    value = uiState.details.gatewayIp
                )

                // One row per resolver: two addresses sharing a line wrap into an unreadable
                // run of digits on a narrow screen.
                if (uiState.details.dnsServers.isEmpty()) {
                    DetailRow(label = stringResource(R.string.network_dns), value = null)
                } else {
                    uiState.details.dnsServers.forEach { dns ->
                        DetailRow(label = stringResource(R.string.network_dns), value = dns)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    // Absent on some builds, and an ActivityNotFoundException here would take
                    // the app down with it.
                    runCatching {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                shape    = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.open_system_network_settings))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VpnWarning() {
    Card(
        colors   = CardDefaults.cardColors(containerColor = InfoBlue.copy(alpha = 0.15f)),
        shape    = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.VpnKey,
                contentDescription = null,
                tint     = InfoBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text     = stringResource(R.string.network_vpn_warning),
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Caption on the left, value on the right; the value is selectable so an IP can be copied. */
@Composable
private fun DetailRow(label: String, value: String?) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text     = label,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        SelectionContainer {
            Text(
                text       = value ?: NoValue,
                fontFamily = FontFamily.Monospace,
                fontSize   = 13.sp,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun NetworkScanner.Transport.labelRes(): Int = when (this) {
    NetworkScanner.Transport.WIFI     -> R.string.network_wifi
    NetworkScanner.Transport.CELLULAR -> R.string.network_cellular
    NetworkScanner.Transport.ETHERNET -> R.string.network_ethernet
    NetworkScanner.Transport.OTHER    -> R.string.network_other
    NetworkScanner.Transport.NONE     -> R.string.network_no_connection
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val WifiDetails = NetworkScanner.NetworkDetails(
    transport    = NetworkScanner.Transport.WIFI,
    isVpnActive  = false,
    localIp      = "192.168.1.42",
    subnetBase   = "192.168.1.",
    prefixLength = 24,
    gatewayIp    = "192.168.1.1",
    dnsServers   = listOf("192.168.1.1", "1.1.1.1")
)

@Preview(showBackground = true)
@Composable
private fun NetworkInfoWifiPreview() {
    ServeraTheme {
        NetworkInfoContent(
            uiState   = NetworkInfoUiState(WifiDetails),
            onBack    = {},
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkInfoVpnPreview() {
    ServeraTheme {
        NetworkInfoContent(
            uiState   = NetworkInfoUiState(WifiDetails.copy(isVpnActive = true)),
            onBack    = {},
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkInfoNoConnectionPreview() {
    ServeraTheme {
        NetworkInfoContent(
            uiState   = NetworkInfoUiState(),
            onBack    = {},
            onRefresh = {}
        )
    }
}
