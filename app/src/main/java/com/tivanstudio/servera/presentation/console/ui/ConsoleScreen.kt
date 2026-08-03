package com.tivanstudio.servera.presentation.console.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        uiState         = uiState,
        onBack          = onBack,
        onExecute       = viewModel::navigateToExecute,
        onSelectTab     = viewModel::selectTab,
        onOpenPicker    = viewModel::openPicker,
        onDismissPicker = viewModel::dismissPicker,
        onAttach        = viewModel::attachPreset,
        onRun           = viewModel::runAttached,
        onRemove        = viewModel::removeAttached,
        onToggleHistory = viewModel::toggleHistory
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsoleScreenContent(
    uiState: ConsoleUiState,
    onBack: () -> Unit,
    onExecute: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onOpenPicker: () -> Unit,
    onDismissPicker: () -> Unit,
    onAttach: (Preset) -> Unit,
    onRun: (QuickCommand) -> Unit,
    onRemove: (Long) -> Unit,
    onToggleHistory: () -> Unit
) {
    if (uiState.showPicker) {
        PresetPickerSheet(
            uiState   = uiState,
            onDismiss = onDismissPicker,
            onAttach  = onAttach
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
                    uiState         = uiState,
                    onExecute       = onExecute,
                    onOpenPicker    = onOpenPicker,
                    onRun           = onRun,
                    onRemove        = onRemove,
                    onToggleHistory = onToggleHistory
                )
                1 -> InfoTab(uiState = uiState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsoleTab(
    uiState: ConsoleUiState,
    onExecute: () -> Unit,
    onOpenPicker: () -> Unit,
    onRun: (QuickCommand) -> Unit,
    onRemove: (Long) -> Unit,
    onToggleHistory: () -> Unit
) {
    val isBusy = uiState.runningId != null

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
                IconButton(onClick = onOpenPicker, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.presets_picker_title),
                        tint = PrimaryGreen
                    )
                }
            }
        }

        if (uiState.runError != null) {
            item {
                Text(
                    text     = uiState.runError,
                    color    = DangerRed,
                    fontSize = 12.sp
                )
            }
        }

        if (uiState.attachedCommands.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_attached_commands),
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            items(uiState.attachedCommands, key = { it.id }) { cmd ->
                AttachedCommandItem(
                    cmd       = cmd,
                    isRunning = uiState.runningId == cmd.id,
                    enabled   = !isBusy,
                    onRun     = { onRun(cmd) },
                    onRemove  = { onRemove(cmd.id) }
                )
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleHistory)
                        .padding(vertical = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.executed_commands),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Icon(
                        if (uiState.showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.showHistory) {
                items(uiState.recentHistory) { history ->
                    HistoryItem(history = history, onRepeat = onExecute)
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachedCommandItem(
    cmd: QuickCommand,
    isRunning: Boolean,
    enabled: Boolean,
    onRun: () -> Unit,
    onRemove: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> DangerRed.copy(alpha = 0.85f)
                    else                              -> Color.Transparent
                },
                label = "attached_swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                }
            }
        }
    ) {
        Card(
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
                        text       = cmd.label,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 14.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text       = cmd.command,
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
                    IconButton(
                        onClick  = onRun,
                        enabled  = enabled,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint     = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick  = onRemove,
                    enabled  = enabled,
                    modifier = Modifier.size(32.dp)
                ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetPickerSheet(
    uiState: ConsoleUiState,
    onDismiss: () -> Unit,
    onAttach: (Preset) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text       = stringResource(R.string.presets_picker_title),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (uiState.presets.isEmpty()) {
                Text(
                    stringResource(R.string.presets_picker_empty),
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            } else {
                val attached = uiState.attachedCommandStrings

                LazyColumn(
                    modifier            = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.groupedPresets.forEach { (category, presets) ->
                        item(key = "header_$category") {
                            Text(
                                text     = category,
                                style    = MaterialTheme.typography.titleSmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
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
                            PresetPickerRow(
                                preset     = preset,
                                isAttached = preset.command in attached,
                                onClick    = { onAttach(preset) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetPickerRow(
    preset: Preset,
    isAttached: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isAttached, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = preset.label,
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp,
                color      = if (isAttached) MaterialTheme.colorScheme.onSurfaceVariant
                             else MaterialTheme.colorScheme.onSurface,
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
        if (isAttached) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint     = PrimaryGreen,
                modifier = Modifier.size(20.dp)
            )
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
                attachedCommands = listOf(
                    QuickCommand(1, 1, "Running containers", "docker ps", 0),
                    QuickCommand(2, 1, "Disk free", "df -h", 1)
                ),
                presets = listOf(
                    Preset(0, "Docker", "Running containers", "docker ps", PresetSource.BUILTIN, 0),
                    Preset(1, "System", "Disk free", "df -h", PresetSource.CUSTOM, 0)
                ),
                showPicker  = false,
                showHistory = false,
                recentHistory = listOf(
                    CommandHistory(1, 1, "ls -la /etc", "output", "", 0, System.currentTimeMillis())
                )
            ),
            onBack          = {},
            onExecute       = {},
            onSelectTab     = {},
            onOpenPicker    = {},
            onDismissPicker = {},
            onAttach        = {},
            onRun           = {},
            onRemove        = {},
            onToggleHistory = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ConsoleTabEmptyPreview() {
    ServeraTheme {
        ConsoleScreenContent(
            uiState = ConsoleUiState(
                server = Server(1, "Staging", "10.0.0.1", 22, "deploy", ""),
                attachedCommands = emptyList()
            ),
            onBack          = {},
            onExecute       = {},
            onSelectTab     = {},
            onOpenPicker    = {},
            onDismissPicker = {},
            onAttach        = {},
            onRun           = {},
            onRemove        = {},
            onToggleHistory = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun AttachedCommandItemRunningPreview() {
    ServeraTheme {
        AttachedCommandItem(
            cmd       = QuickCommand(1, 1, "Disk free", "df -h", 0),
            isRunning = true,
            enabled   = false,
            onRun     = {},
            onRemove  = {}
        )
    }
}
