package com.tivanstudio.servera.presentation.console.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetSource
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleEvent
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleUiState
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleViewModel
import com.tivanstudio.servera.presentation.presets.ui.PresetDialog
import com.tivanstudio.servera.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConsoleScreen(
    viewModel: ConsoleViewModel = hiltViewModel(),
    onNavigateToExecute: (Long) -> Unit,
    onNavigateToResult: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ConsoleEvent.NavigateToExecute -> onNavigateToExecute(event.serverId)
                is ConsoleEvent.NavigateToResult  -> onNavigateToResult()
            }
        }
    }

    ConsoleScreenContent(
        uiState                = uiState,
        onBack                 = onBack,
        onExecute              = viewModel::navigateToExecute,
        onSelectTab            = viewModel::selectTab,
        onRunPreset            = viewModel::runPreset,
        onAddPreset            = viewModel::startAddPreset,
        onEditPreset           = viewModel::startEditPreset,
        onDeletePreset         = viewModel::deletePreset,
        onDismissPresetDialog  = viewModel::dismissPresetDialog,
        onSavePreset           = viewModel::savePreset
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsoleScreenContent(
    uiState: ConsoleUiState,
    onBack: () -> Unit,
    onExecute: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onRunPreset: (Preset) -> Unit,
    onAddPreset: () -> Unit,
    onEditPreset: (Preset) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onDismissPresetDialog: () -> Unit,
    onSavePreset: (String, String, String) -> Unit
) {
    if (uiState.editingPreset != null) {
        PresetDialog(
            initial    = uiState.editingPreset,
            categories = uiState.categories,
            isNew      = uiState.editingPreset.id == 0L,
            onDismiss  = onDismissPresetDialog,
            onSave     = onSavePreset
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.server?.name ?: stringResource(R.string.console_tab),
                            fontWeight = FontWeight.Bold
                        )
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
                    text     = { Text(stringResource(R.string.console_tab)) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick  = { onSelectTab(1) },
                    text     = { Text(stringResource(R.string.info_tab)) }
                )
            }

            when (uiState.selectedTab) {
                0 -> ConsoleTab(
                    uiState        = uiState,
                    onExecute      = onExecute,
                    onRunPreset    = onRunPreset,
                    onAddPreset    = onAddPreset,
                    onEditPreset   = onEditPreset,
                    onDeletePreset = onDeletePreset
                )
                1 -> InfoTab(uiState = uiState)
            }
        }
    }
}

@Composable
private fun ConsoleTab(
    uiState: ConsoleUiState,
    onExecute: () -> Unit,
    onRunPreset: (Preset) -> Unit,
    onAddPreset: () -> Unit,
    onEditPreset: (Preset) -> Unit,
    onDeletePreset: (Long) -> Unit
) {
    val isBusy = uiState.runningPresetId != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.quick_commands), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAddPreset, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_quick_command),
                        tint = PrimaryGreen
                    )
                }
            }
        }

        if (uiState.presetError != null) {
            item {
                Text(
                    text     = uiState.presetError,
                    color    = DangerRed,
                    fontSize = 12.sp
                )
            }
        }

        if (uiState.presets.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.presets_picker_empty),
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            uiState.groupedPresets.forEach { (category, presets) ->
                item(key = "header_$category") {
                    Text(
                        text     = category,
                        style    = MaterialTheme.typography.titleSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(
                    items = presets,
                    key   = { preset ->
                        when (preset.source) {
                            PresetSource.BUILTIN -> "builtin_${preset.category}_${preset.label}"
                            PresetSource.CUSTOM  -> "custom_${preset.id}"
                        }
                    }
                ) { preset ->
                    PresetRunItem(
                        preset    = preset,
                        isRunning = uiState.runningPresetId == preset.id,
                        enabled   = !isBusy,
                        onRun     = { onRunPreset(preset) },
                        onEdit    = { onEditPreset(preset) },
                        onDelete  = { onDeletePreset(preset.id) }
                    )
                }
            }
        }

        item {
            Button(
                onClick = onExecute,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape    = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.new_command),
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimary
                )
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
private fun PresetRunItem(
    preset: Preset,
    isRunning: Boolean,
    enabled: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick  = onRun,
        enabled  = enabled,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = preset.label,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text       = preset.command,
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            if (isRunning) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(18.dp),
                    color       = PrimaryGreen,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint     = PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (preset.source == PresetSource.CUSTOM) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint     = InfoBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint     = DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(history: CommandHistory, onRepeat: () -> Unit) {
    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = history.command,
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 13.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = fmt.format(Date(history.executedAt)),
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    modifier              = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = DangerRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.serverInfoError, color = DangerRed)
                }
            }
            uiState.serverInfo != null -> ServerInfoContent(info = uiState.serverInfo)
            else -> Box(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ServerInfoContent(info: ServerInfo) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(value.ifBlank { "—" }, fontSize = 13.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(2f))
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ConsoleTabPreview() {
    ServeraTheme {
        ConsoleScreenContent(
            uiState = ConsoleUiState(
                server = Server(1, "Production", "192.168.1.1", 22, "root", ""),
                presets = listOf(
                    Preset(0, "Docker", "Running containers", "docker ps", PresetSource.BUILTIN, 0),
                    Preset(1, "Docker", "Compose logs", "docker compose logs --tail=100", PresetSource.CUSTOM, 1),
                    Preset(2, "System", "Disk free", "df -h", PresetSource.CUSTOM, 0)
                ),
                runningPresetId = 1L,
                recentHistory = listOf(
                    CommandHistory(1, 1, "ls -la /etc", "output", "", 0, System.currentTimeMillis())
                )
            ),
            onBack                = {},
            onExecute             = {},
            onSelectTab           = {},
            onRunPreset           = {},
            onAddPreset           = {},
            onEditPreset          = {},
            onDeletePreset        = {},
            onDismissPresetDialog = {},
            onSavePreset          = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PresetRunItemBuiltinPreview() {
    ServeraTheme {
        PresetRunItem(
            preset    = Preset(0, "Docker", "Running containers", "docker ps", PresetSource.BUILTIN, 0),
            isRunning = false,
            enabled   = true,
            onRun     = {}, onEdit = {}, onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PresetRunItemCustomRunningPreview() {
    ServeraTheme {
        PresetRunItem(
            preset    = Preset(1, "System", "Disk free", "df -h", PresetSource.CUSTOM, 0),
            isRunning = true,
            enabled   = false,
            onRun     = {}, onEdit = {}, onDelete = {}
        )
    }
}
