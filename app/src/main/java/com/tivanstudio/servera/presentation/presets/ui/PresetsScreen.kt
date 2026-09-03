package com.tivanstudio.servera.presentation.presets.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.domain.entity.PresetSource
import com.tivanstudio.servera.presentation.components.AppBottomBar
import com.tivanstudio.servera.presentation.navigation.Screen
import com.tivanstudio.servera.presentation.presets.viewmodel.PresetsUiState
import com.tivanstudio.servera.presentation.presets.viewmodel.PresetsViewModel
import com.tivanstudio.servera.presentation.theme.*

/** Tiles per row. Bump to 3 and the width maths below follows on its own. */
private const val PRESET_TILE_COLUMNS = 2

private val TileHeight = 120.dp
private val TileSpacing = 8.dp
private val ScreenPadding = 16.dp

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
        onRefresh       = viewModel::refresh,
        onCopy          = viewModel::startCopy,
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
    onAdd: (Long) -> Unit,
    onEdit: (Preset) -> Unit,
    onDelete: (Long) -> Unit,
    onRefresh: () -> Unit,
    onCopy: (Preset) -> Unit,
    onClearMessage: () -> Unit,
    onDismissDialog: () -> Unit,
    onSave: (groupId: Long, label: String, command: String) -> Unit
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.presets_title), fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !uiState.isUpdating) {
                        if (uiState.isUpdating) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.presets_update)
                            )
                        }
                    }
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
        }
    ) { padding ->
        // A preset needs a group to live in — no groups, no grid to add into.
        if (!hasGroups) {
            NoGroupsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onNavigateToGroups = onNavigateToGroups
            )
            return@Scaffold
        }

        // FlowRow gives the tiles no intrinsic width, and weight() would stretch a lone tile
        // across the whole row, so the column width is split up front instead.
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val tileWidth = (screenWidth - ScreenPadding * 2 -
            TileSpacing * (PRESET_TILE_COLUMNS - 1)) / PRESET_TILE_COLUMNS

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ScreenPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            uiState.grouped.forEach { (group, presets) ->
                item(key = "group_${group.id}") {
                    GroupSection(
                        group     = group,
                        presets   = presets,
                        tileWidth = tileWidth,
                        onAdd     = onAdd,
                        onEdit    = onEdit,
                        onDelete  = onDelete,
                        onCopy    = onCopy
                    )
                }
            }
        }
    }
}

/** One group as its name plus a wrapping grid of tiles, the add tile last. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupSection(
    group: PresetGroup,
    presets: List<Preset>,
    tileWidth: Dp,
    onAdd: (Long) -> Unit,
    onEdit: (Preset) -> Unit,
    onDelete: (Long) -> Unit,
    onCopy: (Preset) -> Unit
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
            maxItemsInEachRow     = PRESET_TILE_COLUMNS,
            horizontalArrangement = Arrangement.spacedBy(TileSpacing),
            verticalArrangement   = Arrangement.spacedBy(TileSpacing)
        ) {
            presets.forEach { preset ->
                PresetTile(
                    preset    = preset,
                    group     = group,
                    tileWidth = tileWidth,
                    onEdit    = { onEdit(preset) },
                    onDelete  = { onDelete(preset.id) },
                    onCopy    = { onCopy(preset) }
                )
            }
            // An empty group is just this tile on its own — no separate empty state needed.
            AddTile(
                group     = group,
                tileWidth = tileWidth,
                onAdd     = { onAdd(group.id) }
            )
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
 * A tap opens the dialog either way. Built-ins have no Room row behind them, so it opens on a
 * copy of the preset instead of on the preset itself; the padlock is the only thing that says so.
 * That copy is also the only thing a built-in can do, so long-press has no menu to offer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetTile(
    preset: Preset,
    group: PresetGroup,
    tileWidth: Dp,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val groupColor  = group.colorHex.toComposeColor()
    val isBuiltin   = preset.source == PresetSource.BUILTIN
    val description = "${preset.label}: ${preset.command}"

    Box {
        Card(
            shape  = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, groupColor.copy(alpha = 0.25f)),
            modifier = Modifier
                .size(width = tileWidth, height = TileHeight)
                .clip(MaterialTheme.shapes.large)
                .combinedClickable(
                    onClick     = { if (isBuiltin) onCopy() else onEdit() },
                    onLongClick = { if (!isBuiltin) menuExpanded = true }
                )
                .semantics { contentDescription = description }
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(groupColor.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint     = groupColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (isBuiltin) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = stringResource(R.string.preset_built_in),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text       = preset.label,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text       = preset.command,
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 11.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                }
            }
        }

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

/** Closes every grid: a preset tile's footprint, dashed in the group's colour. */
@Composable
private fun AddTile(group: PresetGroup, tileWidth: Dp, onAdd: () -> Unit) {
    val groupColor = group.colorHex.toComposeColor()
    val dashColor  = groupColor.copy(alpha = 0.4f)
    val shape      = MaterialTheme.shapes.large

    Card(
        shape    = shape,
        colors   = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .size(width = tileWidth, height = TileHeight)
            .drawBehind {
                // Inset by half the stroke: centred on the bounds, its outer half would fall
                // outside them and get clipped away.
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color        = dashColor,
                    topLeft      = Offset(stroke / 2, stroke / 2),
                    size         = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(shape.topStart.toPx(size, this)),
                    style = Stroke(
                        width      = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                )
            }
            .clip(shape)
            .clickable(onClick = onAdd)
    ) {
        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.preset_add_to_group),
                tint     = groupColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

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
                    Preset(-1, -10, "Running containers", "docker ps", 0, PresetSource.BUILTIN),
                    Preset(-2, -10, "Compose logs", "docker compose logs --tail=100", 1, PresetSource.BUILTIN),
                    Preset(-3, -11, "Open ports", "ss -tulpn", 0, PresetSource.BUILTIN),
                    // Sits next to the built-in above, so the padlock has something to contrast with.
                    Preset(3, -11, "My tunnel", "ssh -D 1080 gateway", 1, PresetSource.CUSTOM),
                    Preset(1, 1, "Deploy", "./deploy.sh", 0, PresetSource.CUSTOM),
                    Preset(2, 2, "Disk free", "df -h", 0, PresetSource.CUSTOM)
                )
            ),
            onNavigateToServers  = {},
            onNavigateToHistory  = {},
            onNavigateToSettings = {},
            onNavigateToGroups   = {},
            onAdd           = {},
            onEdit          = {},
            onDelete        = {},
            onRefresh       = {},
            onCopy          = {},
            onClearMessage  = {},
            onDismissDialog = {},
            onSave          = { _, _, _ -> }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun PresetsScreenUpdatingPreview() {
    ServeraTheme {
        PresetsScreenContent(
            uiState = PresetsUiState(
                groups  = listOf(PresetGroup(-10, "Docker", "#1565C0", 0, PresetSource.BUILTIN)),
                presets = listOf(
                    Preset(-1, -10, "Running containers", "docker ps", 0, PresetSource.BUILTIN)
                ),
                isUpdating = true
            ),
            onNavigateToServers  = {},
            onNavigateToHistory  = {},
            onNavigateToSettings = {},
            onNavigateToGroups   = {},
            onAdd           = {},
            onEdit          = {},
            onDelete        = {},
            onRefresh       = {},
            onCopy          = {},
            onClearMessage  = {},
            onDismissDialog = {},
            onSave          = { _, _, _ -> }
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
            onRefresh       = {},
            onCopy          = {},
            onClearMessage  = {},
            onDismissDialog = {},
            onSave          = { _, _, _ -> }
        )
    }
}
