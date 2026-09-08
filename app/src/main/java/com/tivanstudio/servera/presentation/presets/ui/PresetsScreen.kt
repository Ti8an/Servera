package com.tivanstudio.servera.presentation.presets.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.tivanstudio.servera.domain.entity.PresetSource
import com.tivanstudio.servera.presentation.common.CommandGridPadding
import com.tivanstudio.servera.presentation.common.CommandPickerTile
import com.tivanstudio.servera.presentation.common.CommandTile
import com.tivanstudio.servera.presentation.common.CommandTileColumns
import com.tivanstudio.servera.presentation.common.CommandTileSpacing
import com.tivanstudio.servera.presentation.common.rememberCommandTileWidth
import com.tivanstudio.servera.presentation.common.ui.DismissibleFilterChip
import com.tivanstudio.servera.presentation.common.ui.FilterSectionTitle
import com.tivanstudio.servera.presentation.components.AppBottomBar
import com.tivanstudio.servera.presentation.navigation.Screen
import com.tivanstudio.servera.presentation.presets.viewmodel.PresetsUiState
import com.tivanstudio.servera.presentation.presets.viewmodel.PresetsViewModel
import com.tivanstudio.servera.presentation.theme.*
import kotlinx.coroutines.launch

@Composable
fun PresetsScreen(
    viewModel: PresetsViewModel = hiltViewModel(),
    onNavigateToServers: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGroups: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PresetsScreenContent(
        uiState              = uiState,
        onNavigateToServers  = onNavigateToServers,
        onNavigateToHistory  = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToGroups   = onNavigateToGroups,
        onAdd           = viewModel::startAdd,
        onEdit          = viewModel::startEdit,
        onDelete        = viewModel::deletePreset,
        onOpenSourceChooser  = viewModel::openSourceChooser,
        onDismissSourceChooser = viewModel::dismissSourceChooser,
        onOpenLibrary        = viewModel::openLibrary,
        onDismissLibrary     = viewModel::dismissLibrary,
        onAddFromLibrary     = viewModel::addFromLibrary,
        onCopyFromLibrary    = viewModel::copyFromLibrary,
        onClearMessage  = viewModel::clearMessage,
        onDismissDialog = viewModel::dismissDialog,
        onSave          = viewModel::savePreset
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetsScreenContent(
    uiState: PresetsUiState,
    onNavigateToServers: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Preset) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenSourceChooser: () -> Unit,
    onDismissSourceChooser: () -> Unit,
    onOpenLibrary: () -> Unit,
    onDismissLibrary: () -> Unit,
    onAddFromLibrary: (Preset) -> Unit,
    onCopyFromLibrary: (Preset) -> Unit,
    onClearMessage: () -> Unit,
    onDismissDialog: () -> Unit,
    onSave: (groupId: Long, label: String, command: String, iconKey: String?) -> Unit
) {
    val hasGroups = uiState.groups.isNotEmpty()

    val snackbarHostState = remember { SnackbarHostState() }
    val message = uiState.updateMessageRes?.let { stringResource(it) }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onClearMessage()
        }
    }

    if (uiState.editing != null) {
        PresetDialog(
            initial   = uiState.editing,
            // Built-in groups are catalog entries with no row in preset_groups, and presets.groupId
            // is a foreign key onto it — offering one as a target would fail the insert.
            groups    = uiState.groups.filter { it.source == PresetSource.CUSTOM },
            isNew     = uiState.isNew,
            onDismiss = onDismissDialog,
            onSave    = onSave
        )
    }

    if (uiState.showSourceChooser) {
        SourceChooserDialog(
            onDismiss        = onDismissSourceChooser,
            onOpenLibrary    = onOpenLibrary,
            onCreateManually = onAdd
        )
    }

    if (uiState.showLibrary) {
        PresetLibraryDialog(
            uiState      = uiState,
            onDismiss    = onDismissLibrary,
            onAdd        = onAddFromLibrary,
            onCopyToForm = onCopyFromLibrary
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.presets_title), fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onNavigateToGroups) {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = stringResource(R.string.groups_title)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = Screen.Presets.route,
                onServers    = onNavigateToServers,
                onPresets    = {},
                onHistory    = onNavigateToHistory,
                onSettings   = onNavigateToSettings
            )
        },
        floatingActionButton = {
            // A preset needs a group to live in — no groups, nothing to add.
            if (hasGroups) {
                FloatingActionButton(
                    onClick        = onOpenSourceChooser,
                    containerColor = PrimaryGreen
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.presets_title),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        if (!hasGroups) {
            NoGroupsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onNavigateToGroups = onNavigateToGroups
            )
            return@Scaffold
        }

        val tileWidth = rememberCommandTileWidth()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = CommandGridPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            uiState.grouped.forEach { (group, presets) ->
                item(key = "group_${group.id}") {
                    GroupSection(
                        group     = group,
                        presets   = presets,
                        tileWidth = tileWidth,
                        onEdit    = onEdit,
                        onDelete  = onDelete
                    )
                }
            }
        }
    }
}

/** One group as its name plus a wrapping grid of its presets; an empty group is just the name. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupSection(
    group: PresetGroup,
    presets: List<Preset>,
    tileWidth: Dp,
    onEdit: (Preset) -> Unit,
    onDelete: (Long) -> Unit
) {
    Column {
        Text(
            text       = group.name,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Normal,
            color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.padding(bottom = 12.dp)
        )

        FlowRow(
            maxItemsInEachRow     = CommandTileColumns,
            horizontalArrangement = Arrangement.spacedBy(CommandTileSpacing),
            verticalArrangement   = Arrangement.spacedBy(CommandTileSpacing)
        ) {
            presets.forEach { preset ->
                PresetTile(
                    preset    = preset,
                    group     = group,
                    tileWidth = tileWidth,
                    onEdit    = { onEdit(preset) },
                    onDelete  = { onDelete(preset.id) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NoGroupsState(
    modifier: Modifier = Modifier,
    onNavigateToGroups: () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Text(
                text      = stringResource(R.string.no_groups_hint),
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onNavigateToGroups,
                colors  = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape   = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Category, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.groups_title),
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * A preset as a tile: neutral surface, with the group's colour carried by the border and the
 * terminal glyph rather than by a fill — a solid tile would read as an on/off state a preset
 * does not have.
 *
 * The grid holds only the user's own presets, so every tile edits and deletes: the built-in
 * catalog is read-only and lives behind the library dialog instead.
 */
@Composable
private fun PresetTile(
    preset: Preset,
    group: PresetGroup,
    tileWidth: Dp,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        CommandTile(
            label       = preset.label,
            command     = preset.command,
            iconKey     = preset.iconKey,
            accentColor = group.colorHex.toComposeColor(),
            width       = tileWidth,
            onClick     = onEdit,
            onLongClick = { menuExpanded = true },
            // Nothing ever badges a preset tile, so the command gets the whole row.
            reservesBadgeSlot = false
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
                    onDelete()
                }
            )
        }
    }
}

/** Step one: a new command is either lifted from the catalog or typed from scratch. */
@Composable
private fun SourceChooserDialog(
    onDismiss: () -> Unit,
    onOpenLibrary: () -> Unit,
    onCreateManually: () -> Unit
) {
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
                Button(
                    onClick = onOpenLibrary,
                    colors  = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape   = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Bookmarks, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.open_library), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCreateManually,
                    shape   = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.create_manually))
                }
            }
        },
        // Both choices are buttons in the body; there is nothing left to confirm.
        confirmButton = {},
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

/**
 * Step two: the built-in catalog, browsable in full. Adding takes a preset as it stands; the copy
 * action drops it into the edit dialog instead, for when it needs a tweak first.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PresetLibraryDialog(
    uiState: PresetsUiState,
    onDismiss: () -> Unit,
    onAdd: (Preset) -> Unit,
    onCopyToForm: (Preset) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope      = rememberCoroutineScope()
    val addedMsg   = stringResource(R.string.preset_added)
    val alreadyMsg = stringResource(R.string.already_added)

    // Lives only as long as the dialog is open, by design -- see LibraryFilter.
    var filter by remember { mutableStateOf(LibraryFilter()) }
    var showFilterSheet by remember { mutableStateOf(false) }

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
                                stringResource(R.string.preset_library),
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
                        actions = {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.filter_title),
                                    tint = if (filter.isActive) PrimaryGreen
                                           else LocalContentColor.current
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { padding ->
                // Compared by text: a catalog preset has no id its copy in Room keeps.
                val alreadyAdded = uiState.customCommandStrings
                val tileWidth    = rememberCommandTileWidth()
                val visible      = uiState.builtinGrouped.applyFilter(filter, alreadyAdded)

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (filter.isActive) {
                        LibraryFilterChips(
                            uiState  = uiState,
                            filter   = filter,
                            onChange = { filter = it }
                        )
                    }

                    // A filter that matched nothing and an empty catalog are different problems
                    // with different ways out, so they get different empty states.
                    if (visible.isEmpty()) {
                        LibraryEmptyState(
                            isFiltered = filter.isActive,
                            onReset    = { filter = LibraryFilter() }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = CommandGridPadding),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            // A group is one item, header and grid together: splitting the tiles
                            // into their own items would let a row straddle two groups as the
                            // list recycles.
                            visible.forEach { (group, presets) ->
                                item(key = "library_group_${group.id}") {
                                    Column {
                                        Row(
                                            modifier          = Modifier.padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            GroupDot(colorHex = group.colorHex, size = 12)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text  = group.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        FlowRow(
                                            maxItemsInEachRow     = CommandTileColumns,
                                            horizontalArrangement = Arrangement.spacedBy(CommandTileSpacing),
                                            verticalArrangement   = Arrangement.spacedBy(CommandTileSpacing)
                                        ) {
                                            presets.forEach { preset ->
                                                val isAdded = preset.command in alreadyAdded
                                                CommandPickerTile(
                                                    label       = preset.label,
                                                    command     = preset.command,
                                                    iconKey     = preset.iconKey,
                                                    accentColor = group.colorHex.toComposeColor(),
                                                    width       = tileWidth,
                                                    isAdded     = isAdded,
                                                    onAdd = {
                                                        // Tapping one that is already in says so
                                                        // rather than doing nothing: silence
                                                        // reads as a hang.
                                                        if (isAdded) {
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar(alreadyMsg)
                                                            }
                                                        } else {
                                                            onAdd(preset)
                                                            // The dialog stays open: several
                                                            // presets usually go at once.
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar(addedMsg)
                                                            }
                                                        }
                                                    },
                                                    onCopyToForm = { onCopyToForm(preset) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showFilterSheet) {
                LibraryFilterSheet(
                    uiState   = uiState,
                    filter    = filter,
                    onApply   = { filter = it; showFilterSheet = false },
                    onDismiss = { showFilterSheet = false }
                )
            }
        }
    }
}

/** Nothing to show: either the catalog itself is empty or the filter cut everything out. */
@Composable
private fun LibraryEmptyState(isFiltered: Boolean, onReset: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Text(
                text      = stringResource(
                    if (isFiltered) R.string.filter_nothing_found else R.string.presets_picker_empty
                ),
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (isFiltered) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.filter_reset), color = PrimaryGreen)
                }
            }
        }
    }
}

/** One dismissible chip per active filter parameter, mirroring the history list. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryFilterChips(
    uiState: PresetsUiState,
    filter: LibraryFilter,
    onChange: (LibraryFilter) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filter.groupId?.let { id ->
            val name = uiState.groupOf(id)?.name ?: "#$id"
            DismissibleFilterChip(
                label   = "${stringResource(R.string.filter_group)}: $name",
                onClear = { onChange(filter.copy(groupId = null)) }
            )
        }
        if (filter.query.isNotBlank()) {
            DismissibleFilterChip(
                label   = "${stringResource(R.string.filter_search)}: ${filter.query}",
                onClear = { onChange(filter.copy(query = "")) }
            )
        }
        if (filter.hideAdded) {
            DismissibleFilterChip(
                label   = stringResource(R.string.filter_hide_attached),
                onClear = { onChange(filter.copy(hideAdded = false)) }
            )
        }
    }
}

/**
 * Edited on a local copy, as in the history sheet: a half-made selection never reaches the
 * grid, and only Apply hands the result back.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LibraryFilterSheet(
    uiState: PresetsUiState,
    filter: LibraryFilter,
    onApply: (LibraryFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var local by remember(filter) { mutableStateOf(filter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text       = stringResource(R.string.filter_title),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            FilterSectionTitle(stringResource(R.string.filter_group))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = local.groupId == null,
                    onClick  = { local = local.copy(groupId = null) },
                    label    = { Text(stringResource(R.string.filter_all)) }
                )
                // Only the catalog groups: the users own groups have nothing in here.
                uiState.builtinGrouped.forEach { (group, _) ->
                    FilterChip(
                        selected = local.groupId == group.id,
                        onClick  = { local = local.copy(groupId = group.id) },
                        label    = { Text(group.name) },
                        leadingIcon = { GroupDot(colorHex = group.colorHex, size = 10) }
                    )
                }
            }

            OutlinedTextField(
                value         = local.query,
                onValueChange = { local = local.copy(query = it) },
                label         = { Text(stringResource(R.string.filter_search)) },
                singleLine    = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = PrimaryGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Row(
                modifier          = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = stringResource(R.string.filter_hide_attached),
                    style    = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked         = local.hideAdded,
                    onCheckedChange = { local = local.copy(hideAdded = it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = PrimaryGreen)
                )
            }

            Row(
                modifier              = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { local = LibraryFilter() }) {
                    Text(
                        stringResource(R.string.filter_reset),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onApply(local) },
                    colors  = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape   = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.filter_apply), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * What the preset library is narrowed to.
 *
 * Local to the open dialog rather than held in the view model: a filter is a way of looking at
 * the list while choosing something, not a setting anyone expects to find again next time.
 */
private data class LibraryFilter(
    val groupId: Long? = null,
    val query: String = "",
    val hideAdded: Boolean = false
) {
    val isActive: Boolean get() = groupId != null || query.isNotBlank() || hideAdded
}

/**
 * The catalog narrowed by [libraryFilter]. A group left with no presets drops out entirely: a
 * header over an empty space reads as a loading glitch rather than as a filter result.
 */
private fun List<Pair<PresetGroup, List<Preset>>>.applyFilter(
    libraryFilter: LibraryFilter,
    alreadyAdded: Set<String>
): List<Pair<PresetGroup, List<Preset>>> =
    filter { (group, _) -> libraryFilter.groupId == null || group.id == libraryFilter.groupId }
        .map { (group, presets) ->
            group to presets.filter { preset ->
                val matchesQuery = libraryFilter.query.isBlank() ||
                    preset.label.contains(libraryFilter.query, ignoreCase = true) ||
                    preset.command.contains(libraryFilter.query, ignoreCase = true)
                val allowedByAdded = !libraryFilter.hideAdded || preset.command !in alreadyAdded
                matchesQuery && allowedByAdded
            }
        }
        .filter { (_, presets) -> presets.isNotEmpty() }

// ── Previews ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun PresetsScreenContentPreview() {
    ServeraTheme {
        PresetsScreenContent(
            uiState = PresetsUiState(
                // Both blocks number sortOrder from zero, so this ordering only comes out as
                // Docker, Network, My scripts, System, Empty once source outranks sortOrder.
                groups = listOf(
                    PresetGroup(-10, "Docker", "#1565C0", 0, PresetSource.BUILTIN),
                    PresetGroup(-11, "Network", "#AD1457", 1, PresetSource.BUILTIN),
                    PresetGroup(1, "My scripts", "#2E7D32", 0, PresetSource.CUSTOM),
                    PresetGroup(2, "System", "#455A64", 1, PresetSource.CUSTOM),
                    // No presets of its own — the row is nothing but the add tile.
                    PresetGroup(3, "Empty", "#6A1B9A", 2, PresetSource.CUSTOM)
                ),
                presets = listOf(
                    // Built-ins carry negative catalog ids; the users own rows come from Room.
                    Preset(-1, -10, "Running containers", "docker ps", 0, PresetSource.BUILTIN, "storage"),
                    Preset(-2, -10, "Compose logs", "docker compose logs --tail=100", 1, PresetSource.BUILTIN, "cloud"),
                    // Long enough to be cut on any tile width — shows the ellipsis doing its job.
                    Preset(-4, -10, "Prod logs", "docker compose -f docker-compose.prod.yml logs --tail=200 -f", 2, PresetSource.BUILTIN, "bug"),
                    Preset(-3, -11, "Open ports", "ss -tulpn", 0, PresetSource.BUILTIN, "speed"),
                    // Sits next to the built-in above, so the padlock has something to contrast with.
                    Preset(3, -11, "My tunnel", "ssh -D 1080 gateway", 1, PresetSource.CUSTOM, "lock"),
                    Preset(1, 1, "Deploy", "./deploy.sh", 0, PresetSource.CUSTOM, "upload"),
                    Preset(2, 2, "Disk free", "df -h", 0, PresetSource.CUSTOM, "memory")
                )
            ),
            onNavigateToServers  = {},
            onNavigateToHistory  = {},
            onNavigateToSettings = {},
            onNavigateToGroups   = {},
            onAdd           = {},
            onEdit          = {},
            onDelete        = {},
            onOpenSourceChooser    = {},
            onDismissSourceChooser = {},
            onOpenLibrary          = {},
            onDismissLibrary       = {},
            onAddFromLibrary       = {},
            onCopyFromLibrary      = {},
            onClearMessage  = {},
            onDismissDialog = {},
            onSave          = { _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SourceChooserDialogPreview() {
    ServeraTheme {
        SourceChooserDialog(
            onDismiss        = {},
            onOpenLibrary    = {},
            onCreateManually = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun PresetLibraryDialogPreview() {
    ServeraTheme {
        PresetLibraryDialog(
            uiState = PresetsUiState(
                groups = listOf(
                    PresetGroup(-10, "Docker", "#1565C0", 0, PresetSource.BUILTIN),
                    PresetGroup(-11, "Network", "#AD1457", 1, PresetSource.BUILTIN),
                    PresetGroup(1, "Docker", "#1565C0", 0, PresetSource.CUSTOM)
                ),
                presets = listOf(
                    Preset(-1, -10, "Running containers", "docker ps", 0, PresetSource.BUILTIN, "cloud"),
                    Preset(-2, -10, "Compose logs", "docker compose logs --tail=100", 1, PresetSource.BUILTIN),
                    Preset(-3, -11, "Open ports", "ss -tulpn", 0, PresetSource.BUILTIN, "speed"),
                    // Same command text as the first built-in, so that row shows the tick.
                    Preset(1, 1, "Running containers", "docker ps", 0, PresetSource.CUSTOM, "cloud")
                )
            ),
            onDismiss    = {},
            onAdd        = {},
            onCopyToForm = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun PresetsScreenNoGroupsPreview() {
    ServeraTheme {
        PresetsScreenContent(
            uiState              = PresetsUiState(),
            onNavigateToServers  = {},
            onNavigateToHistory  = {},
            onNavigateToSettings = {},
            onNavigateToGroups   = {},
            onAdd           = {},
            onEdit          = {},
            onDelete        = {},
            onOpenSourceChooser    = {},
            onDismissSourceChooser = {},
            onOpenLibrary          = {},
            onDismissLibrary       = {},
            onAddFromLibrary       = {},
            onCopyFromLibrary      = {},
            onClearMessage  = {},
            onDismissDialog = {},
            onSave          = { _, _, _, _ -> }
        )
    }
}
