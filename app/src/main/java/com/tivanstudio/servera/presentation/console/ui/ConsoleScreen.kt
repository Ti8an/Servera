package com.tivanstudio.servera.presentation.console.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
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
import com.tivanstudio.servera.presentation.console.viewmodel.CommandRunState
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
        uiState            = uiState,
        onBack             = onBack,
        onExecute          = viewModel::navigateToExecute,
        onSelectTab        = viewModel::selectTab,
        onInfoTabSelected  = viewModel::onInfoTabSelected,
        onRefreshServerInfo = viewModel::refreshServerInfo,
        onOpenAddDialog    = viewModel::openAddDialog,
        onDismissAddDialog = viewModel::dismissAddDialog,
        onSaveTyped        = viewModel::attachTyped,
        onRun              = viewModel::runAttached,
        onRemove           = viewModel::removeAttached,
        onToggleHistory    = viewModel::toggleHistory
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsoleScreenContent(
    uiState: ConsoleUiState,
    onBack: () -> Unit,
    onExecute: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onInfoTabSelected: () -> Unit,
    onRefreshServerInfo: () -> Unit,
    onOpenAddDialog: () -> Unit,
    onDismissAddDialog: () -> Unit,
    onSaveTyped: (label: String, command: String, showOutput: Boolean) -> Unit,
    onRun: (QuickCommand) -> Unit,
    onRemove: (Long) -> Unit,
    onToggleHistory: () -> Unit
) {
    if (uiState.showAddDialog) {
        AddCommandDialog(
            uiState   = uiState,
            onDismiss = onDismissAddDialog,
            onSave    = onSaveTyped
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
                    onClick  = {
                        onSelectTab(1)
                        onInfoTabSelected()
                    },
                    text     = { Text(stringResource(R.string.info_tab)) }
                )
            }

            when (uiState.selectedTab) {
                0 -> ConsoleTab(
                    uiState         = uiState,
                    onExecute       = onExecute,
                    onOpenAddDialog = onOpenAddDialog,
                    onRun           = onRun,
                    onRemove        = onRemove,
                    onToggleHistory = onToggleHistory
                )
                1 -> InfoTab(uiState = uiState, onRefresh = onRefreshServerInfo)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsoleTab(
    uiState: ConsoleUiState,
    onExecute: () -> Unit,
    onOpenAddDialog: () -> Unit,
    onRun: (QuickCommand) -> Unit,
    onRemove: (Long) -> Unit,
    onToggleHistory: () -> Unit
) {
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
                IconButton(onClick = onOpenAddDialog, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_command_title),
                        tint = PrimaryGreen
                    )
                }
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
                    cmd      = cmd,
                    runState = uiState.runStates[cmd.id],
                    onRun    = { onRun(cmd) },
                    onRemove = { onRemove(cmd.id) }
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
    runState: CommandRunState?,
    onRun: () -> Unit,
    onRemove: () -> Unit
) {
    val isRunning = runState is CommandRunState.Running

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
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isRunning, onClick = onRun)
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

                when (runState) {
                    is CommandRunState.Running -> CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        color       = PrimaryGreen,
                        strokeWidth = 2.dp
                    )
                    is CommandRunState.Done -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint     = if (runState.exitCode == 0) PrimaryGreen else DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                    is CommandRunState.Failure -> Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint     = DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun AddCommandDialog(
    uiState: ConsoleUiState,
    onDismiss: () -> Unit,
    onSave: (label: String, command: String, showOutput: Boolean) -> Unit
) {
    var label      by remember { mutableStateOf("") }
    var command    by remember { mutableStateOf("") }
    var showOutput by remember { mutableStateOf(true) }

    val canSubmit = label.isNotBlank() && command.isNotBlank()
    val attached  = uiState.attachedCommandStrings

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text       = stringResource(R.string.add_command_title),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = label,
                    onValueChange = { label = it },
                    label         = { Text(stringResource(R.string.command_label_field)) },
                    singleLine    = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value         = command,
                    onValueChange = { command = it },
                    label         = { Text(stringResource(R.string.command_label)) },
                    singleLine    = false,
                    minLines      = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization     = KeyboardCapitalization.None,
                        autoCorrectEnabled = false
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 14.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor      = PrimaryGreen,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text     = stringResource(R.string.show_output_field),
                        fontSize = 14.sp
                    )
                    Switch(
                        checked         = showOutput,
                        onCheckedChange = { showOutput = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = PrimaryGreen
                        )
                    )
                }

                Text(
                    text       = stringResource(R.string.common_commands),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                if (uiState.presets.isEmpty()) {
                    Text(
                        stringResource(R.string.no_common_commands),
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        uiState.groupedPresets.forEach { (category, presets) ->
                            item(key = "header_$category") {
                                Text(
                                    text     = category,
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
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
                                PresetSuggestionRow(
                                    preset     = preset,
                                    isAttached = preset.command in attached,
                                    onClick    = {
                                        label   = preset.label
                                        command = preset.command
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(label, command, showOutput) },
                enabled = canSubmit
            ) {
                Text(
                    stringResource(R.string.save_button),
                    fontWeight = FontWeight.Medium,
                    color = if (canSubmit) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun PresetSuggestionRow(
    preset: Preset,
    isAttached: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
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
        if (isAttached) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint     = PrimaryGreen,
                modifier = Modifier.size(16.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoTab(uiState: ConsoleUiState, onRefresh: () -> Unit) {
    PullToRefreshBox(
        isRefreshing = uiState.isLoadingServerInfo,
        onRefresh    = onRefresh,
        modifier     = Modifier.fillMaxSize()
    ) {
        when {
            uiState.serverInfo != null -> ServerInfoContent(info = uiState.serverInfo)

            uiState.serverInfoError != null -> Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = uiState.serverInfoError,
                    color     = DangerRed,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(32.dp)
                )
            }

            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = stringResource(R.string.info_swipe_hint),
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ServerInfoContent(info: ServerInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        InfoRow(stringResource(R.string.info_hostname), info.hostname)
        InfoRow(stringResource(R.string.info_os), info.os)
        InfoRow(stringResource(R.string.info_cpu), info.cpuInfo)
        InfoRow(stringResource(R.string.info_ram_total), info.ramTotal)
        InfoRow(stringResource(R.string.info_ram_free), info.ramFree)
        InfoRow(stringResource(R.string.info_disk), info.diskUsage)
        InfoRow(stringResource(R.string.info_uptime), info.uptime)
        Spacer(Modifier.height(16.dp))
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
                    QuickCommand(1, 1, "Running containers", "docker ps", 0, showOutput = true),
                    QuickCommand(2, 1, "Disk free", "df -h", 1, showOutput = false)
                ),
                presets = listOf(
                    Preset(0, "Docker", "Running containers", "docker ps", PresetSource.BUILTIN, 0),
                    Preset(1, "System", "Disk free", "df -h", PresetSource.CUSTOM, 0)
                ),
                runStates = mapOf(
                    1L to CommandRunState.Done(
                        stdout   = "CONTAINER ID   IMAGE     STATUS\n9f1c2b3a4d5e   nginx     Up 3 hours",
                        stderr   = "",
                        exitCode = 0
                    ),
                    2L to CommandRunState.Failure("Connection refused")
                ),
                showAddDialog = false,
                showHistory   = false,
                recentHistory = listOf(
                    CommandHistory(1, 1, "ls -la /etc", "output", "", 0, System.currentTimeMillis())
                )
            ),
            onBack              = {},
            onExecute           = {},
            onSelectTab         = {},
            onInfoTabSelected   = {},
            onRefreshServerInfo = {},
            onOpenAddDialog    = {},
            onDismissAddDialog = {},
            onSaveTyped        = { _, _, _ -> },
            onRun              = {},
            onRemove           = {},
            onToggleHistory    = {}
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
            onBack              = {},
            onExecute           = {},
            onSelectTab         = {},
            onInfoTabSelected   = {},
            onRefreshServerInfo = {},
            onOpenAddDialog    = {},
            onDismissAddDialog = {},
            onSaveTyped        = { _, _, _ -> },
            onRun              = {},
            onRemove           = {},
            onToggleHistory    = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun InfoTabEmptyPreview() {
    ServeraTheme {
        ConsoleScreenContent(
            uiState = ConsoleUiState(
                server      = Server(1, "Production", "192.168.1.1", 22, "root", ""),
                selectedTab = 1,
                serverInfo  = null
            ),
            onBack              = {},
            onExecute           = {},
            onSelectTab         = {},
            onInfoTabSelected   = {},
            onRefreshServerInfo = {},
            onOpenAddDialog    = {},
            onDismissAddDialog = {},
            onSaveTyped        = { _, _, _ -> },
            onRun              = {},
            onRemove           = {},
            onToggleHistory    = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun AttachedCommandItemRunningPreview() {
    ServeraTheme {
        AttachedCommandItem(
            cmd      = QuickCommand(1, 1, "Disk free", "df -h", 0),
            runState = CommandRunState.Running,
            onRun    = {},
            onRemove = {}
        )
    }
}
