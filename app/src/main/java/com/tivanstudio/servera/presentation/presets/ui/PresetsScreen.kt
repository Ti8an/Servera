package com.tivanstudio.servera.presentation.presets.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
        },
        floatingActionButton = {
            // A preset needs a group to live in — no groups, no adding.
            if (hasGroups) {
                FloatingActionButton(
                    onClick        = onAdd,
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
        floatingActionButtonPosition = FabPosition.Start
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding      = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.grouped.forEach { (group, presets) ->
                stickyHeader(key = "header_${group.id}") {
                    GroupHeader(group = group)
                }

                items(presets, key = { it.id }) { preset ->
                    PresetRow(
                        preset         = preset,
                        group          = group,
                        onEdit         = { onEdit(preset) },
                        onDelete       = { onDelete(preset.id) },
                        onCopyToCustom = { onCopyToCustom(preset) }
                    )
                }
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
private fun GroupHeader(group: PresetGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroupDot(colorHex = group.colorHex, size = 14)
        Spacer(Modifier.width(8.dp))
        Text(
            text     = group.name,
            style    = MaterialTheme.typography.titleSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Built-ins come from the Remote Config catalog and have no Room row behind them, so they get a
 * read-only card with a copy-into-my-presets action instead of edit and delete.
 */
@Composable
private fun PresetRow(
    preset: Preset,
    group: PresetGroup,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyToCustom: () -> Unit
) {
    when (preset.source) {
        PresetSource.BUILTIN -> BuiltinPresetRow(
            preset         = preset,
            group          = group,
            onCopyToCustom = onCopyToCustom
        )
        PresetSource.CUSTOM -> CustomPresetRow(
            preset   = preset,
            group    = group,
            onEdit   = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
private fun BuiltinPresetRow(
    preset: Preset,
    group: PresetGroup,
    onCopyToCustom: () -> Unit
) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        PresetCardBody(preset = preset, group = group) {
            IconButton(onClick = onCopyToCustom) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy_to_custom),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomPresetRow(
    preset: Preset,
    group: PresetGroup,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
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
                label = "preset_swipe_bg"
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
            onClick  = onEdit,
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape    = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            PresetCardBody(preset = preset, group = group) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint     = DangerRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** Label, command and group chip — the part both row flavours share; [action] closes the row. */
@Composable
private fun PresetCardBody(
    preset: Preset,
    group: PresetGroup,
    action: @Composable () -> Unit
) {
    Row(
        modifier          = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = preset.label,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false)
                )
                if (preset.source == PresetSource.BUILTIN) {
                    Spacer(Modifier.width(6.dp))
                    BuiltinBadge()
                }
            }
            Text(
                text       = preset.command,
                fontFamily = FontFamily.Monospace,
                fontSize   = 12.sp,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        GroupChip(group = group)

        action()
    }
}

@Composable
private fun BuiltinBadge() {
    Text(
        text     = stringResource(R.string.preset_builtin),
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun GroupChip(group: PresetGroup) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GroupDot(colorHex = group.colorHex, size = 8)
        Spacer(Modifier.width(4.dp))
        Text(
            text     = group.name,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Preview(showBackground = true)
@Composable
private fun PresetsScreenContentPreview() {
    ServeraTheme {
        PresetsScreenContent(
            uiState = PresetsUiState(
                // Both blocks number sortOrder from zero, so this ordering only comes out as
                // Docker, Network, My scripts, System once source outranks sortOrder.
                groups = listOf(
                    PresetGroup(-10, "Docker", "#1565C0", 0, PresetSource.BUILTIN),
                    PresetGroup(-11, "Network", "#AD1457", 1, PresetSource.BUILTIN),
                    PresetGroup(1, "My scripts", "#2E7D32", 0, PresetSource.CUSTOM),
                    PresetGroup(2, "System", "#455A64", 1, PresetSource.CUSTOM)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
