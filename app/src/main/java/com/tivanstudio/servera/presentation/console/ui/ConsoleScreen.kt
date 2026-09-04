package com.tivanstudio.servera.presentation.console.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.presentation.presets.ui.GroupDot
import com.tivanstudio.servera.domain.entity.QuickCommand
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.entity.ServerInfo
import com.tivanstudio.servera.presentation.common.CommandGridPadding
import com.tivanstudio.servera.presentation.common.CommandTile
import com.tivanstudio.servera.presentation.common.CommandTileColumns
import com.tivanstudio.servera.presentation.common.CommandTileSpacing
import com.tivanstudio.servera.presentation.common.rememberCommandTileWidth
import com.tivanstudio.servera.presentation.console.viewmodel.CommandRunState
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleEvent
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleUiState
import com.tivanstudio.servera.presentation.console.viewmodel.ConsoleViewModel
import com.tivanstudio.servera.presentation.theme.*
import kotlinx.coroutines.launch

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
        onRemove           = viewModel::removeAttached
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
    onSaveOwn: (
        label: String,
        command: String,
        showOutput: Boolean,
        group: PresetGroup,
        iconKey: String?
    ) -> Unit,
    onRun: (QuickCommand) -> Unit,
    onRemove: (Long) -> Unit
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
                    onOpenCommandDialog = onOpenCommandDialog,
                    onEditCommand   = onEditCommand,
                    onRun           = onRun,
                    onRemove        = onRemove
                )
                1 -> InfoTab(uiState = uiState, onRefresh = onRefreshServerInfo)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ConsoleTab(
    uiState: ConsoleUiState,
    onOpenCommandDialog: () -> Unit,
    onEditCommand: (QuickCommand) -> Unit,
    onRun: (QuickCommand) -> Unit,
    onRemove: (Long) -> Unit
) {
    val tileWidth = rememberCommandTileWidth()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = CommandGridPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
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
                // One flat grid in sortOrder: a server holds few enough commands that splitting
                // them into group sections would cost more room than it buys.
                item {
                    FlowRow(
                        maxItemsInEachRow     = CommandTileColumns,
                        horizontalArrangement = Arrangement.spacedBy(CommandTileSpacing),
                        verticalArrangement   = Arrangement.spacedBy(CommandTileSpacing)
                    ) {
                        uiState.attachedCommands.forEach { cmd ->
                            AttachedCommandTile(
                                cmd       = cmd,
                                runState  = uiState.runStates[cmd.id],
                                tileWidth = tileWidth,
                                onRun     = { onRun(cmd) },
                                onEdit    = { onEditCommand(cmd) },
                                onRemove  = { onRemove(cmd.id) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick        = onOpenCommandDialog,
            containerColor = PrimaryGreen,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add_command)
            )
        }
    }
}

/**
 * An attached command as a tile, matching the presets grid. The group snapshot colours the border
 * and the watermark; commands typed by hand have no snapshot and fall back to a neutral outline.
 *
 * A tap runs the command — by far the common case — so edit and delete sit behind a long press,
 * and delete still asks first: re-attaching a command to a server is not a one-tap undo.
 */
@Composable
private fun AttachedCommandTile(
    cmd: QuickCommand,
    runState: CommandRunState?,
    tileWidth: Dp,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded      by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val tileColor = cmd.groupColorHex?.toComposeColor()
        ?: MaterialTheme.colorScheme.onSurfaceVariant

    // The badge is 12 dp of colour with no label of its own, so the state is spoken here instead.
    val statusText = when (runState) {
        is CommandRunState.Running -> stringResource(R.string.cmd_state_running)
        is CommandRunState.Done    ->
            if (runState.exitCode == 0) stringResource(R.string.cmd_state_success)
            else stringResource(R.string.cmd_state_failed)
        is CommandRunState.Failure -> stringResource(R.string.cmd_state_failed)
        null                       -> null
    }
    val description = buildString {
        append("${cmd.label}: ${cmd.command}")
        statusText?.let { append(", $it") }
    }

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

    Box {
        CommandTile(
            label       = cmd.label,
            command     = cmd.command,
            iconKey     = cmd.iconKey,
            accentColor = tileColor,
            width       = tileWidth,
            onClick     = onRun,
            onLongClick = { menuExpanded = true },
            // A running command has to read as running from across the grid, not from a 12 dp
            // spinner alone.
            highlighted = runState is CommandRunState.Running,
            badge       = { RunStateBadge(runState = runState, color = tileColor) },
            contentDescription = description
        )

        DropdownMenu(
            expanded         = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text        = { Text(stringResource(R.string.preset_edit)) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.preset_delete)) },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                },
                onClick = {
                    menuExpanded = false
                    showDeleteConfirm = true
                }
            )
        }
    }
}

/** Always 12 dp wide, so the command beside it does not reflow as a run starts and finishes. */
@Composable
private fun RunStateBadge(runState: CommandRunState?, color: Color) {
    when (runState) {
        is CommandRunState.Running -> CircularProgressIndicator(
            modifier    = Modifier.size(12.dp),
            strokeWidth = 1.5.dp,
            color       = color
        )
        is CommandRunState.Done -> Icon(
            imageVector        = if (runState.exitCode == 0) Icons.Default.CheckCircle
                                 else Icons.Default.Error,
            contentDescription = null,
            tint     = if (runState.exitCode == 0) PrimaryGreen else DangerRed,
            modifier = Modifier.size(12.dp)
        )
        is CommandRunState.Failure -> Icon(
            Icons.Default.Error,
            contentDescription = null,
            tint     = DangerRed,
            modifier = Modifier.size(12.dp)
        )
        null -> Spacer(Modifier.size(12.dp))
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
    onSaveOwn: (
        label: String,
        command: String,
        showOutput: Boolean,
        group: PresetGroup,
        iconKey: String?
    ) -> Unit
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
    onSave: (
        label: String,
        command: String,
        showOutput: Boolean,
        group: PresetGroup,
        iconKey: String?
    ) -> Unit,
    onCancel: () -> Unit
) {
    val groups = uiState.groups.sortedBy { it.sortOrder }

    var label      by remember(initial) { mutableStateOf(initial?.label ?: "") }
    var command    by remember(initial) { mutableStateOf(initial?.command ?: "") }
    var showOutput by remember(initial) { mutableStateOf(initial?.showOutput ?: true) }
    var iconKey    by remember(initial) { mutableStateOf(initial?.iconKey) }
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

        Text(
            text  = stringResource(R.string.preset_icon),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            maxItemsInEachRow     = 6,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp)
        ) {
            // Match on a key the catalog still has: one dropped by a later version would leave
            // nothing highlighted and get silently overwritten on save.
            val highlighted = iconKey?.takeIf { it in PresetIcons } ?: DEFAULT_PRESET_ICON
            val accent = selected?.colorHex?.toComposeColor() ?: PrimaryGreen
            PresetIcons.forEach { (key, icon) ->
                val isCurrent = key == highlighted
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrent) accent.copy(alpha = 0.2f) else Color.Transparent
                        )
                        .then(
                            if (isCurrent) Modifier.border(1.dp, accent, CircleShape)
                            else Modifier
                        )
                        .clickable { iconKey = key }
                ) {
                    Icon(
                        icon,
                        contentDescription = key,
                        tint = if (isCurrent) accent
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

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
                onClick = { selected?.let { onSave(label, command, showOutput, it, iconKey) } },
                enabled = canSubmit,
                colors  = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape   = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.save_button), fontWeight = FontWeight.Bold)
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
                    QuickCommand(1, 1, "Running containers", "docker ps", 0, true, "Docker", "#1565C0", "cloud"),
                    QuickCommand(2, 1, "Disk free", "df -h", 1, false, "System", "#455A64", "storage"),
                    QuickCommand(3, 1, "Restart nginx", "systemctl restart nginx", 2, true, "System", "#455A64", "power"),
                    QuickCommand(4, 1, "Tail log", "tail -n 50 /var/log/syslog", 3, true, "Logs", "#AD1457", "bug"),
                    // No group snapshot and no icon: both fall back — neutral outline, terminal glyph.
                    QuickCommand(5, 1, "Uptime", "uptime", 4, showOutput = true)
                ),
                presets = listOf(
                    Preset(0, 1, "Running containers", "docker ps", 0),
                    Preset(1, 2, "Disk free", "df -h", 0)
                ),
                groups = listOf(
                    PresetGroup(1, "Docker", "#1565C0", 0),
                    PresetGroup(2, "System", "#2E7D32", 1)
                ),
                // Command 1 is left out on purpose: no run state is a state of its own.
                runStates = mapOf(
                    2L to CommandRunState.Running,
                    3L to CommandRunState.Done(
                        stdout   = "CONTAINER ID   IMAGE     STATUS\n9f1c2b3a4d5e   nginx     Up 3 hours",
                        stderr   = "",
                        exitCode = 0
                    ),
                    4L to CommandRunState.Done(stdout = "", stderr = "unit not found", exitCode = 1),
                    5L to CommandRunState.Failure(R.string.ssh_error_unreachable)
                ),
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
            onSaveOwn          = { _, _, _, _, _ -> },
            onRun              = {},
            onRemove           = {}
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
            onSaveOwn          = { _, _, _, _, _ -> },
            onRun              = {},
            onRemove           = {}
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
            onSaveOwn          = { _, _, _, _, _ -> },
            onRun              = {},
            onRemove           = {}
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
            onSaveOwn          = { _, _, _, _, _ -> },
            onRun              = {},
            onRemove           = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun AttachedCommandTileRunningPreview() {
    ServeraTheme {
        AttachedCommandTile(
            cmd       = QuickCommand(1, 1, "Disk free", "df -h", 0, true, "System", "#455A64", "storage"),
            runState  = CommandRunState.Running,
            tileWidth = 105.dp,
            onRun     = {},
            onEdit    = {},
            onRemove  = {}
        )
    }
}
