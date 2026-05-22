package com.tivanstudio.servera.presentation.console.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleEvent
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleUiState
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleViewModel
import com.tivanstudio.servera.presentation.console.viewmodel.QuickCommandStatus
import com.tivanstudio.servera.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val QUICK_COMMAND_EXAMPLES = listOf(
    "uname -a", "df -h", "free -h", "uptime", "whoami", "ls -la", "ps aux", "top -bn1"
)

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
        uiState               = uiState,
        onBack                = onBack,
        onExecute             = viewModel::navigateToExecute,
        onSelectTab           = viewModel::selectTab,
        onAddCommand          = viewModel::startAddCommand,
        onEditCommand         = viewModel::startEditCommand,
        onDeleteCommand       = viewModel::deleteCommand,
        onExecuteQuickCommand = viewModel::executeQuickCommand,
        onDismissDialog       = viewModel::dismissEditDialog,
        onSaveCommand         = viewModel::saveEditedCommand,
        onRunCommand          = viewModel::saveAndRunEditedCommand
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsoleScreenContent(
    uiState: ConsoleUiState,
    onBack: () -> Unit,
    onExecute: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onAddCommand: () -> Unit,
    onEditCommand: (QuickCommand) -> Unit,
    onDeleteCommand: (Long) -> Unit,
    onExecuteQuickCommand: (QuickCommand) -> Unit,
    onDismissDialog: () -> Unit,
    onSaveCommand: (String, String) -> Unit,
    onRunCommand: (String, String) -> Unit
) {
    if (uiState.editingCommand != null) {
        QuickCommandDialog(
            cmd       = uiState.editingCommand,
            onDismiss = onDismissDialog,
            onSave    = onSaveCommand,
            onRun     = onRunCommand
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
                    uiState               = uiState,
                    onExecute             = onExecute,
                    onAddCommand          = onAddCommand,
                    onEditCommand         = onEditCommand,
                    onDeleteCommand       = onDeleteCommand,
                    onExecuteQuickCommand = onExecuteQuickCommand
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
    onAddCommand: () -> Unit,
    onEditCommand: (QuickCommand) -> Unit,
    onDeleteCommand: (Long) -> Unit,
    onExecuteQuickCommand: (QuickCommand) -> Unit
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
                IconButton(onClick = onAddCommand, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_quick_command),
                        tint = PrimaryGreen
                    )
                }
            }
        }

        if (uiState.quickCommands.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_quick_commands),
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            items(uiState.quickCommands, key = { it.id }) { cmd ->
                QuickCommandItem(
                    cmd      = cmd,
                    status   = uiState.commandStatuses[cmd.id],
                    onTap    = { onExecuteQuickCommand(cmd) },
                    onEdit   = { onEditCommand(cmd) },
                    onDelete = { onDeleteCommand(cmd.id) }
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
            item { Text(stringResource(R.string.recent_commands), style = MaterialTheme.typography.titleMedium) }
            items(uiState.recentHistory) { history ->
                HistoryItem(history = history, onRepeat = onExecute)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCommandItem(
    cmd: QuickCommand,
    status: QuickCommandStatus?,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onDelete(); true  }
                SwipeToDismissBoxValue.EndToStart -> { onEdit();  false }
                else                              -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> DangerRed.copy(alpha = 0.85f)
                    SwipeToDismissBoxValue.EndToStart -> InfoBlue.copy(alpha = 0.85f)
                    else                             -> Color.Transparent
                },
                label = "swipe_bg"
            )
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else                             -> Alignment.CenterEnd
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                else                             -> Icons.Default.Edit
            }
            Box(
                modifier          = Modifier
                    .fillMaxSize()
                    .background(color, shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp),
                contentAlignment  = alignment
            ) {
                if (direction != SwipeToDismissBoxValue.Settled) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }
            }
        }
    ) {
        Card(
            onClick = onTap,
            colors  = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape   = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Column {
                Row(
                    modifier          = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint     = InfoBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
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
                            fontSize   = 11.sp,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                    }
                }

                if (status != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    QuickCommandStatusRow(status = status)
                }
            }
        }
    }
}

@Composable
private fun QuickCommandStatusRow(status: QuickCommandStatus) {
    val (icon, label, tint) = when (status) {
        is QuickCommandStatus.Running -> Triple(null,                         stringResource(R.string.cmd_status_running), MaterialTheme.colorScheme.onSurfaceVariant)
        is QuickCommandStatus.Success -> Triple(Icons.Default.CheckCircle,    stringResource(R.string.cmd_status_success), PrimaryGreen)
        is QuickCommandStatus.Failure -> Triple(Icons.Default.ErrorOutline,   stringResource(R.string.cmd_status_failed),  DangerRed)
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (status is QuickCommandStatus.Running) {
            CircularProgressIndicator(
                modifier  = Modifier.size(12.dp),
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                strokeWidth = 1.5.dp
            )
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        }
        Text(
            text     = if (status is QuickCommandStatus.Failure) "$label: ${status.message}" else label,
            fontSize = 11.sp,
            color    = tint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCommandDialog(
    cmd: QuickCommand,
    onDismiss: () -> Unit,
    onSave: (label: String, command: String) -> Unit,
    onRun: (label: String, command: String) -> Unit
) {
    var label     by remember(cmd.id) { mutableStateOf(cmd.label) }
    var command   by remember(cmd.id) { mutableStateOf(cmd.command) }
    val isNew      = cmd.id == 0L
    val canSubmit  = label.isNotBlank() && command.isNotBlank()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text      = if (isNew) stringResource(R.string.add_quick_command)
                            else       stringResource(R.string.edit_quick_command),
                style     = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value         = label,
                onValueChange = { label = it },
                label         = { Text(stringResource(R.string.quick_command_label_hint)) },
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = PrimaryGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value         = command,
                onValueChange = { command = it },
                label         = { Text(stringResource(R.string.quick_command_command_hint)) },
                minLines      = 3,
                maxLines      = 6,
                textStyle     = MaterialTheme.typography.bodyMedium.copy(
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

            Text(
                stringResource(R.string.example_commands),
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QUICK_COMMAND_EXAMPLES) { example ->
                    SuggestionChip(
                        onClick = { command = example },
                        label   = { Text(example, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        colors  = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick  = { onSave(label, command) },
                    enabled  = canSubmit,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape    = MaterialTheme.shapes.medium
                ) {
                    Text(
                        stringResource(R.string.save_button),
                        fontWeight = FontWeight.Medium,
                        color      = if (canSubmit) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick  = { onRun(label, command) },
                    enabled  = canSubmit,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape    = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.run_button), fontWeight = FontWeight.Bold)
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
                quickCommands = listOf(
                    QuickCommand(1, "Uptime", "uptime", 0),
                    QuickCommand(2, "Disk usage", "df -h", 1)
                ),
                commandStatuses = mapOf(
                    1L to QuickCommandStatus.Success,
                    2L to QuickCommandStatus.Running
                ),
                recentHistory = listOf(
                    CommandHistory(1, 1, "ls -la /etc", "output", "", 0, System.currentTimeMillis())
                )
            ),
            onBack                = {},
            onExecute             = {},
            onSelectTab           = {},
            onAddCommand          = {},
            onEditCommand         = {},
            onDeleteCommand       = {},
            onExecuteQuickCommand = {},
            onDismissDialog       = {},
            onSaveCommand         = { _, _ -> },
            onRunCommand          = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickCommandItemRunningPreview() {
    ServeraTheme {
        QuickCommandItem(
            cmd    = QuickCommand(1, "Uptime", "uptime", 0),
            status = QuickCommandStatus.Running,
            onTap  = {}, onEdit = {}, onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickCommandItemSuccessPreview() {
    ServeraTheme {
        QuickCommandItem(
            cmd    = QuickCommand(1, "Uptime", "uptime", 0),
            status = QuickCommandStatus.Success,
            onTap  = {}, onEdit = {}, onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickCommandItemFailurePreview() {
    ServeraTheme {
        QuickCommandItem(
            cmd    = QuickCommand(1, "Bad cmd", "invalid", 0),
            status = QuickCommandStatus.Failure("Connection refused"),
            onTap  = {}, onEdit = {}, onDelete = {}
        )
    }
}
