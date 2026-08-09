package com.tivanstudio.servera.presentation.console.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.entity.CommandHistory
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.presentation.presets.ui.GroupDot
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo
import com.tivanstudio.servera.presentation.console.viewmodel.CommandRunState
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleEvent
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleUiState
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleViewModel
import com.tivanstudio.servera.presentation.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

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
        onOpenCommandDialog = viewModel::openCommandDialog,
        onDismissCommandDialog = viewModel::dismissCommandDialog,
        onEditCommand      = viewModel::startEdit,
        onPickPreset       = viewModel::attachFromCatalog,
        onSaveOwn          = viewModel::saveOwn,
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
    onOpenCommandDialog: () -> Unit,
    onDismissCommandDialog: () -> Unit,
    onEditCommand: (QuickCommand) -> Unit,
    onPickPreset: (Preset) -> Unit,
    onSaveOwn: (label: String, command: String, showOutput: Boolean, group: PresetGroup) -> Unit,
    onRun: (QuickCommand) -> Unit,
    onRemove: (Long) -> Unit,
    onToggleHistory: () -> Unit
) {
    if (uiState.showCommandDialog) {
        CommandDialog(
            uiState      = uiState,
            initial      = uiState.editingCommand,
            onDismiss    = onDismissCommandDialog,
            onPickPreset = onPickPreset,
            onSaveOwn    = onSaveOwn
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
                    onOpenCommandDialog = onOpenCommandDialog,
                    onEditCommand   = onEditCommand,
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
    onOpenCommandDialog: () -> Unit,
    onEditCommand: (QuickCommand) -> Unit,
    onRun: (QuickCommand) -> Unit,
    onRemove: (Long) -> Unit,
    onToggleHistory: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding      = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        onEdit   = { onEditCommand(cmd) },
                        onRemove = { onRemove(cmd.id) }
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
        }

        FloatingActionButton(
            onClick        = onOpenCommandDialog,
            containerColor = PrimaryGreen,
            modifier       = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add_command)
            )
        }
    }
}

@Composable
private fun AttachedCommandItem(
    cmd: QuickCommand,
    runState: CommandRunState?,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val isRunning = runState is CommandRunState.Running

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val offsetX = remember { Animatable(0f) }
    val scope   = rememberCoroutineScope()
    val density = LocalDensity.current

    val maxDragPx       = with(density) { 96.dp.toPx() }
    val actionThreshold = with(density) { 72.dp.toPx() }

    val offset = offsetX.value

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor   = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.delete_command_title)) },
            text  = { Text(stringResource(R.string.delete_command_message, cmd.label)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onRemove()
                }) {
                    Text(stringResource(R.string.delete_confirm), color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(
                        stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Action layer, uncovered by the card as it follows the finger.
        if (offset != 0f) {
            val draggingRight = offset > 0f
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = if (draggingRight) InfoBlue.copy(alpha = 0.85f)
                                else DangerRed.copy(alpha = 0.85f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = if (draggingRight) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    imageVector        = if (draggingRight) Icons.Default.Edit else Icons.Default.Delete,
                    contentDescription = null,
                    tint               = Color.White
                )
            }
        }

        Card(
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape    = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offsetX.snapTo((offsetX.value + delta).coerceIn(-maxDragPx, maxDragPx))
                        }
                    },
                    onDragStopped = {
                        // Both actions open a dialog, so the row always slides back.
                        when {
                            offsetX.value >= actionThreshold  -> onEdit()
                            offsetX.value <= -actionThreshold -> showDeleteConfirm = true
                        }
                        offsetX.animateTo(0f, tween(durationMillis = 200))
                    }
                )
                .clickable(enabled = !isRunning, onClick = onRun)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = cmd.label,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 14.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    CommandGroupTag(cmd = cmd)
                }
                Text(
                    text       = cmd.command,
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )

                when (runState) {
                    is CommandRunState.Running -> Row(
                        modifier          = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(14.dp),
                            color       = PrimaryGreen,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text     = stringResource(R.string.cmd_running),
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    is CommandRunState.Done -> Text(
                        text     = stringResource(R.string.cmd_exit_code, runState.exitCode),
                        color    = if (runState.exitCode == 0) PrimaryGreen else DangerRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    is CommandRunState.Failure -> Text(
                        text       = runState.message,
                        color      = DangerRed,
                        fontSize   = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier   = Modifier.padding(top = 6.dp)
                    )
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun CommandGroupTag(cmd: QuickCommand) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        cmd.groupColorHex?.let { hex ->
            GroupDot(colorHex = hex, size = 8)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text     = cmd.groupName ?: stringResource(R.string.group_custom),
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Full-screen add/edit sheet. Adding offers both modes — pick a catalog preset or
 * type your own; editing only ever touches an existing command, so it opens straight
 * into the manual form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandDialog(
    uiState: ConsoleUiState,
    initial: QuickCommand?,
    onDismiss: () -> Unit,
    onPickPreset: (Preset) -> Unit,
    onSaveOwn: (label: String, command: String, showOutput: Boolean, group: PresetGroup) -> Unit
) {
    val isEditing = initial != null
    var ownMode by remember(initial) { mutableStateOf(isEditing) }
    // Selection is keyed by group + command rather than by id, so it survives
    // duplicate or unsaved ids in the catalog.
    var selectedKey by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope    = rememberCoroutineScope()
    val addedMsg = stringResource(R.string.command_added)

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost   = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(
                                    if (isEditing) R.string.edit_command_title
                                    else R.string.add_command_title
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel)
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
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (!isEditing) {
                        TabRow(
                            selectedTabIndex = if (ownMode) 1 else 0,
                            containerColor   = MaterialTheme.colorScheme.surface,
                            contentColor     = PrimaryGreen
                        ) {
                            Tab(
                                selected = !ownMode,
                                onClick  = { ownMode = false },
                                text     = { Text(stringResource(R.string.mode_from_catalog)) }
                            )
                            Tab(
                                selected = ownMode,
                                onClick  = { ownMode = true },
                                text     = { Text(stringResource(R.string.mode_own)) }
                            )
                        }
                    }

                    if (ownMode) {
                        OwnCommandForm(
                            uiState  = uiState,
                            initial  = initial,
                            onSave   = onSaveOwn,
                            onCancel = onDismiss
                        )
                    } else {
                        CatalogPicker(
                            uiState     = uiState,
                            selectedKey = selectedKey,
                            onSelect    = { selectedKey = it },
                            modifier    = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val preset = uiState.presets
                                    .firstOrNull { it.selectionKey() == selectedKey }
                                if (preset != null) {
                                    onPickPreset(preset)
                                    scope.launch { snackbarHostState.showSnackbar(addedMsg) }
                                    // Cleared so the next preset can be picked right away.
                                    selectedKey = null
                                }
                            },
                            enabled  = selectedKey != null,
                            colors   = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape    = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(stringResource(R.string.save_button), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** Stable identity of a catalog row: ids may collide, group + command does not. */
private fun Preset.selectionKey(): String = "$groupId|$command"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogPicker(
    uiState: ConsoleUiState,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.grouped.isEmpty()) {
        Box(
            modifier         = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text      = stringResource(R.string.no_common_commands),
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(32.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier       = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        uiState.grouped.forEach { (group, presets) ->
            item(key = "header_${group.id}") {
                Row(
                    modifier          = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GroupDot(colorHex = group.colorHex, size = 10)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text  = group.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(presets, key = { "${it.groupId}_${it.command}" }) { preset ->
                val isSelected = preset.selectionKey() == selectedKey
                // Animated so picking another row reads as a move, not a flicker.
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryGreen.copy(alpha = 0.18f)
                                  else MaterialTheme.colorScheme.surface,
                    label = "presetBackground"
                )
                Card(
                    onClick  = { onSelect(preset.selectionKey()) },
                    colors   = CardDefaults.cardColors(containerColor = containerColor),
                    border   = if (isSelected) BorderStroke(1.5.dp, PrimaryGreen) else null,
                    shape    = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
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
                                color      = MaterialTheme.colorScheme.onSurface,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnCommandForm(
    uiState: ConsoleUiState,
    initial: QuickCommand?,
    onSave: (label: String, command: String, showOutput: Boolean, group: PresetGroup) -> Unit,
    onCancel: () -> Unit
) {
    val groups = uiState.groups.sortedBy { it.sortOrder }

    var label      by remember(initial) { mutableStateOf(initial?.label ?: "") }
    var command    by remember(initial) { mutableStateOf(initial?.command ?: "") }
    var showOutput by remember(initial) { mutableStateOf(initial?.showOutput ?: true) }
    // An edited command only remembers its group by name, so match on that.
    var group      by remember(initial, groups) {
        mutableStateOf(
            groups.firstOrNull { it.name == initial?.groupName } ?: groups.firstOrNull()
        )
    }

    val selected  = group
    val canSubmit = label.isNotBlank() && command.isNotBlank() && selected != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            Text(text = stringResource(R.string.show_output_field), fontSize = 14.sp)
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
            text       = stringResource(R.string.select_group),
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        if (groups.isEmpty()) {
            Text(
                text     = stringResource(R.string.no_groups_hint),
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        } else {
            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                groups.forEach { candidate ->
                    FilterChip(
                        selected = candidate.id == selected?.id,
                        onClick  = { group = candidate },
                        label    = { Text(candidate.name) },
                        leadingIcon = { GroupDot(colorHex = candidate.colorHex, size = 10) }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text(
                    stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { selected?.let { onSave(label, command, showOutput, it) } },
                enabled = canSubmit,
                colors  = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape   = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.save_button), fontWeight = FontWeight.Bold)
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

            uiState.serverInfoErrorRes != null -> Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = stringResource(uiState.serverInfoErrorRes),
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
                    QuickCommand(2, 1, "Disk free", "df -h", 1, showOutput = false),
                    QuickCommand(3, 1, "Restart nginx", "systemctl restart nginx", 2, showOutput = true),
                    QuickCommand(4, 1, "Tail log", "tail -n 50 /var/log/syslog", 3, showOutput = true)
                ),
                presets = listOf(
                    Preset(0, 1, "Running containers", "docker ps", 0),
                    Preset(1, 2, "Disk free", "df -h", 0)
                ),
                groups = listOf(
                    PresetGroup(1, "Docker", "#1565C0", 0),
                    PresetGroup(2, "System", "#2E7D32", 1)
                ),
                runStates = mapOf(
                    1L to CommandRunState.Running,
                    2L to CommandRunState.Done(
                        stdout   = "CONTAINER ID   IMAGE     STATUS\n9f1c2b3a4d5e   nginx     Up 3 hours",
                        stderr   = "",
                        exitCode = 0
                    ),
                    3L to CommandRunState.Done(stdout = "", stderr = "unit not found", exitCode = 5),
                    4L to CommandRunState.Failure("Connection refused")
                ),
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
            onOpenCommandDialog = {},
            onDismissCommandDialog = {},
            onEditCommand      = {},
            onPickPreset       = {},
            onSaveOwn          = { _, _, _, _ -> },
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
            onOpenCommandDialog = {},
            onDismissCommandDialog = {},
            onEditCommand      = {},
            onPickPreset       = {},
            onSaveOwn          = { _, _, _, _ -> },
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
            onOpenCommandDialog = {},
            onDismissCommandDialog = {},
            onEditCommand      = {},
            onPickPreset       = {},
            onSaveOwn          = { _, _, _, _ -> },
            onRun              = {},
            onRemove           = {},
            onToggleHistory    = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun InfoTabErrorPreview() {
    ServeraTheme {
        ConsoleScreenContent(
            uiState = ConsoleUiState(
                server             = Server(1, "Production", "192.168.1.1", 22, "root", ""),
                selectedTab        = 1,
                serverInfoErrorRes = R.string.ssh_error_auth
            ),
            onBack              = {},
            onExecute           = {},
            onSelectTab         = {},
            onInfoTabSelected   = {},
            onRefreshServerInfo = {},
            onOpenCommandDialog = {},
            onDismissCommandDialog = {},
            onEditCommand      = {},
            onPickPreset       = {},
            onSaveOwn          = { _, _, _, _ -> },
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
            onEdit   = {},
            onRemove = {}
        )
    }
}
