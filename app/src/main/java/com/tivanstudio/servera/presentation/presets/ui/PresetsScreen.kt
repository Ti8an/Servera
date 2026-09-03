package com.tivanstudio.servera.presentation.presets.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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

/** Every tile in a row shares one footprint, the add tile included, so the row scrolls evenly. */
private val TileWidth = 168.dp
private val TileHeight = 92.dp

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
        onCopyToCustom  = viewModel::copyToCustom,
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
    onCopyToCustom: (Preset) -> Unit,
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
            groups    = uiState.groups,
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
        // A preset needs a group to live in — no groups, no rows to add into.
        if (!hasGroups) {
            NoGroupsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onNavigateToGroups = onNavigateToGroups
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding      = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            uiState.grouped.forEach { (group, presets) ->
                item(key = "group_${group.id}") {
                    GroupSection(
                        group          = group,
                        presets        = presets,
                        onAdd          = onAdd,
                        onEdit         = onEdit,
                        onDelete       = onDelete,
                        onCopyToCustom = onCopyToCustom
                    )
                }
            }
        }
    }
}

/**
 * One group as a header plus a horizontal row of tiles. The row carries its own horizontal padding
 * instead of inheriting it from the column, so tiles slide under the edges and the next one peeks
 * in rather than being clipped flat.
 */
@Composable
private fun GroupSection(
    group: PresetGroup,
    presets: List<Preset>,
    onAdd: (Long) -> Unit,
    onEdit: (Preset) -> Unit,
    onDelete: (Long) -> Unit,
    onCopyToCustom: (Preset) -> Unit
) {
    Column {
        GroupHeader(group = group, count = presets.size)
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets, key = { it.id }) { preset ->
                PresetTile(
                    preset         = preset,
                    group          = group,
                    onEdit         = { onEdit(preset) },
                    onDelete       = { onDelete(preset.id) },
                    onCopyToCustom = { onCopyToCustom(preset) }
                )
            }
            // An empty group is just this tile on its own — no separate empty state needed.
            item(key = "add_${group.id}") {
                AddTile(group = group, onAdd = { onAdd(group.id) })
            }
        }
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

@Composable
private fun GroupHeader(group: PresetGroup, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroupDot(colorHex = group.colorHex, size = 14)
        Spacer(Modifier.width(8.dp))
        Text(
            text     = group.name,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text  = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * A preset as a tile tinted with its group's colour. The tint stays translucent over the surface —
 * the raw group colour would swallow the monospaced command on a dark theme.
 *
 * Built-ins have no Room row behind them, so editing and deleting are off the table: their menu
 * offers the copy-into-my-presets fork instead, and a tap opens that menu rather than a dead edit.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetTile(
    preset: Preset,
    group: PresetGroup,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyToCustom: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val groupColor  = group.colorHex.toComposeColor()
    val isBuiltin   = preset.source == PresetSource.BUILTIN
    val description = "${preset.label}: ${preset.command}"

    Box {
        Card(
            shape  = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = groupColor
                    .copy(alpha = 0.18f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
            ),
            border = BorderStroke(1.dp, groupColor.copy(alpha = 0.45f)),
            modifier = Modifier
                .size(width = TileWidth, height = TileHeight)
                .clip(MaterialTheme.shapes.medium)
                .combinedClickable(
                    onClick     = { if (isBuiltin) menuExpanded = true else onEdit() },
                    onLongClick = { menuExpanded = true }
                )
                .semantics { contentDescription = description }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text       = preset.label,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = preset.command,
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(
            expanded         = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            if (isBuiltin) {
                DropdownMenuItem(
                    text        = { Text(stringResource(R.string.copy_to_custom)) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onCopyToCustom()
                    }
                )
            } else {
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
}

/** Closes every row: a preset tile's footprint, dashed in the group's colour. */
@Composable
private fun AddTile(group: PresetGroup, onAdd: () -> Unit) {
    val groupColor = group.colorHex.toComposeColor()
    val dashColor  = groupColor.copy(alpha = 0.4f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = TileWidth, height = TileHeight)
            .clip(MaterialTheme.shapes.medium)
            .background(Color.Transparent)
            .drawBehind {
                // Inset by half the stroke: centred on the bounds, its outer half falls outside
                // and the clip eats it.
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color        = dashColor,
                    topLeft      = Offset(stroke / 2, stroke / 2),
                    size         = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(
                        width      = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                )
            }
            .clickable(onClick = onAdd)
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = stringResource(R.string.preset_add_to_group),
            tint     = groupColor,
            modifier = Modifier.size(24.dp)
        )
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
            onCopyToCustom  = {},
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
            onCopyToCustom  = {},
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
            onCopyToCustom  = {},
            onClearMessage  = {},
            onDismissDialog = {},
            onSave          = { _, _, _ -> }
        )
    }
}
