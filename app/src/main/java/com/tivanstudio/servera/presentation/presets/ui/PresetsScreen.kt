package com.tivanstudio.servera.presentation.presets.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
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
import com.tivanstudio.servera.domain.entity.Preset
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
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PresetsScreenContent(
        uiState              = uiState,
        onNavigateToServers  = onNavigateToServers,
        onNavigateToHistory  = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
        onAdd           = viewModel::startAdd,
        onEdit          = viewModel::startEdit,
        onCopyToCustom  = viewModel::copyBuiltinToCustom,
        onDelete        = viewModel::deletePreset,
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
    onAdd: () -> Unit,
    onEdit: (Preset) -> Unit,
    onCopyToCustom: (Preset) -> Unit,
    onDelete: (Long) -> Unit,
    onDismissDialog: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    if (uiState.editing != null) {
        PresetDialog(
            initial    = uiState.editing,
            categories = uiState.categories,
            isNew      = uiState.isNew,
            onDismiss  = onDismissDialog,
            onSave     = onSave
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.presets_title), fontWeight = FontWeight.Bold)
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
    ) { padding ->
        val grouped = uiState.grouped

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.categories.forEach { category ->
                stickyHeader(key = "header_$category") {
                    Text(
                        text       = category,
                        style      = MaterialTheme.typography.titleSmall,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier   = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 8.dp)
                    )
                }

                items(
                    items = grouped[category].orEmpty(),
                    key   = { preset ->
                        when (preset.source) {
                            PresetSource.BUILTIN -> "builtin_${preset.category}_${preset.label}"
                            PresetSource.CUSTOM  -> "custom_${preset.id}"
                        }
                    }
                ) { preset ->
                    when (preset.source) {
                        PresetSource.BUILTIN -> BuiltinPresetRow(
                            preset = preset,
                            onCopyToCustom = { onCopyToCustom(preset) }
                        )
                        PresetSource.CUSTOM -> CustomPresetRow(
                            preset   = preset,
                            onEdit   = { onEdit(preset) },
                            onDelete = { onDelete(preset.id) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun BuiltinPresetRow(
    preset: Preset,
    onCopyToCustom: () -> Unit
) {
    PresetCard(
        preset = preset,
        badge  = {
            Badge(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor   = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    stringResource(R.string.preset_builtin),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        },
        trailing = {
            IconButton(onClick = onCopyToCustom) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint     = InfoBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomPresetRow(
    preset: Preset,
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
        PresetCard(
            preset  = preset,
            onClick = onEdit,
            trailing = {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint     = DangerRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun PresetCard(
    preset: Preset,
    onClick: (() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val shape = MaterialTheme.shapes.medium
    val content: @Composable () -> Unit = {
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
                        overflow   = TextOverflow.Ellipsis
                    )
                    if (badge != null) {
                        Spacer(Modifier.width(6.dp))
                        badge()
                    }
                }
                Text(
                    text       = preset.command,
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }

    if (onClick != null) {
        Card(
            onClick  = onClick,
            colors   = colors,
            shape    = shape,
            modifier = Modifier.fillMaxWidth()
        ) { content() }
    } else {
        Card(
            colors   = colors,
            shape    = shape,
            modifier = Modifier.fillMaxWidth()
        ) { content() }
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
                presets = listOf(
                    Preset(0, "Docker", "Running containers", "docker ps", PresetSource.BUILTIN, 0),
                    Preset(1, "Docker", "Compose logs", "docker compose logs --tail=100", PresetSource.CUSTOM, 1),
                    Preset(2, "System", "Disk free", "df -h", PresetSource.CUSTOM, 0)
                )
            ),
            onNavigateToServers  = {},
            onNavigateToHistory  = {},
            onNavigateToSettings = {},
            onAdd           = {},
            onEdit          = {},
            onCopyToCustom  = {},
            onDelete        = {},
            onDismissDialog = {},
            onSave          = { _, _, _ -> }
        )
    }
}
