package com.tivanstudio.servera.presentation.console.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.tivanstudio.servera.domain.entity.CommandHistory
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleEvent
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleUiState
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleViewModel
import com.tivanstudio.servera.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConsoleScreen(
    viewModel: ConsoleViewModel = hiltViewModel(),
    onNavigateToExecute: (Long) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ConsoleEvent.NavigateToExecute -> onNavigateToExecute(event.serverId)
            }
        }
    }

    ConsoleScreenContent(
        uiState = uiState,
        onBack = onBack,
        onExecute = viewModel::navigateToExecute,
        onSelectTab = viewModel::selectTab
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsoleScreenContent(
    uiState: ConsoleUiState,
    onBack: () -> Unit,
    onExecute: () -> Unit,
    onSelectTab: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.server?.name ?: stringResource(R.string.console_tab), fontWeight = FontWeight.Bold)
                        Text(
                            uiState.server?.host ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onExecute) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = PrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor   = MaterialTheme.colorScheme.surface,
                contentColor     = PrimaryGreen
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick  = { onSelectTab(0) },
                    text = { Text(stringResource(R.string.console_tab)) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick  = { onSelectTab(1) },
                    text = { Text(stringResource(R.string.info_tab)) }
                )
            }

            when (uiState.selectedTab) {
                0 -> ConsoleTab(uiState = uiState, onExecute = onExecute)
                1 -> InfoTab(uiState = uiState)
            }
        }
    }
}

@Composable
private fun ConsoleTab(uiState: ConsoleUiState, onExecute: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.quick_commands), style = MaterialTheme.typography.titleMedium)
        }

        if (uiState.quickCommands.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.quickCommands) { cmd ->
                        SuggestionChip(
                            onClick = onExecute,
                            label = { Text(cmd.label, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onExecute,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.new_command), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        if (uiState.recentHistory.isNotEmpty()) {
            item { Text(stringResource(R.string.recent_commands), style = MaterialTheme.typography.titleMedium) }
            items(uiState.recentHistory) { history ->
                HistoryItem(history = history, onRepeat = onExecute)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun HistoryItem(history: CommandHistory, onRepeat: () -> Unit) {
    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = history.command,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = fmt.format(Date(history.executedAt)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Badge(
                containerColor = if (history.exitCode == 0) PrimaryGreen else DangerRed,
                contentColor   = MaterialTheme.colorScheme.onSurface
            ) {
                Text("${history.exitCode}", modifier = Modifier.padding(4.dp))
            }
        }
    }
}

@Composable
private fun InfoTab(uiState: ConsoleUiState) {
    Box(Modifier.fillMaxSize()) {
        when {
            uiState.isLoadingServerInfo -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryGreen)
            }
            uiState.serverInfoError != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = DangerRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.serverInfoError, color = DangerRed)
                }
            }
            uiState.serverInfo != null -> {
                ServerInfoContent(info = uiState.serverInfo)
            }
            else -> {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun ServerInfoContent(info: ServerInfo) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { InfoRow(stringResource(R.string.info_hostname), info.hostname) }
        item { InfoRow(stringResource(R.string.info_os), info.os) }
        item { InfoRow(stringResource(R.string.info_cpu), info.cpuInfo) }
        item { InfoRow(stringResource(R.string.info_ram_total), info.ramTotal) }
        item { InfoRow(stringResource(R.string.info_ram_free), info.ramFree) }
        item { InfoRow(stringResource(R.string.info_disk), info.diskUsage) }
        item { InfoRow(stringResource(R.string.info_uptime), info.uptime) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                value.ifBlank { "—" },
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(2f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsoleScreenContentPreview() {
    ServeraTheme {
        ConsoleScreenContent(
            uiState = ConsoleUiState(
                server = Server(1, "Production", "192.168.1.1", 22, "root", ""),
                selectedTab = 0,
                quickCommands = listOf(
                    QuickCommand(1, "uptime", "uptime", 0),
                    QuickCommand(2, "df -h", "df -h", 1)
                ),
                recentHistory = listOf(
                    CommandHistory(1, 1, "ls -la /etc", "file1\nfile2", "", 0, System.currentTimeMillis()),
                    CommandHistory(2, 1, "df -h", "Filesystem 100%", "", 1, System.currentTimeMillis())
                )
            ),
            onBack = {},
            onExecute = {},
            onSelectTab = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsoleTabPreview() {
    ServeraTheme {
        ConsoleTab(
            uiState = ConsoleUiState(
                quickCommands = listOf(
                    QuickCommand(1, "uptime", "uptime", 0),
                    QuickCommand(2, "df -h", "df -h", 1)
                ),
                recentHistory = listOf(
                    CommandHistory(1, 1, "ls -la", "output", "", 0, System.currentTimeMillis()),
                    CommandHistory(2, 1, "df -h", "", "error", 1, System.currentTimeMillis())
                )
            ),
            onExecute = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryItemPreview() {
    ServeraTheme {
        HistoryItem(
            history = CommandHistory(1, 1, "ls -la /etc", "output", "", 0, System.currentTimeMillis()),
            onRepeat = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoTabPreview() {
    ServeraTheme {
        InfoTab(
            uiState = ConsoleUiState(
                serverInfo = ServerInfo(
                    hostname = "prod-server-01",
                    os = "Ubuntu 22.04",
                    cpuInfo = "Intel Core i7",
                    ramTotal = "16 GB",
                    ramFree = "8 GB",
                    diskUsage = "50%",
                    uptime = "10 days"
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ServerInfoContentPreview() {
    ServeraTheme {
        ServerInfoContent(
            info = ServerInfo(
                hostname = "prod-server-01",
                os = "Ubuntu 22.04 LTS",
                cpuInfo = "4x Intel Core i7",
                ramTotal = "16 GB",
                ramFree = "8 GB",
                diskUsage = "45% used",
                uptime = "10 days, 3:22"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoRowPreview() {
    ServeraTheme {
        InfoRow(label = "Hostname", value = "prod-server-01")
    }
}
