package com.tivanstudio.servera.presentation.presets.groups

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.presentation.theme.DangerRed
import com.tivanstudio.servera.presentation.theme.PresetColors
import com.tivanstudio.servera.presentation.theme.PrimaryGreen
import com.tivanstudio.servera.presentation.theme.ServeraTheme
import com.tivanstudio.servera.presentation.theme.toComposeColor

@Composable
fun PresetGroupsScreen(
    viewModel: PresetGroupsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PresetGroupsContent(
        uiState   = uiState,
        onBack    = onBack,
        onAdd     = viewModel::startAdd,
        onEdit    = viewModel::startEdit,
        onDelete  = viewModel::delete,
        onDismiss = viewModel::dismiss,
        onSave    = viewModel::save
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetGroupsContent(
    uiState: PresetGroupsUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (PresetGroup) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<PresetGroup?>(null) }

    if (uiState.editing != null) {
        GroupDialog(
            initial   = uiState.editing,
            isNew     = uiState.isNew,
            onDismiss = onDismiss,
            onSave    = onSave
        )
    }

    deleteTarget?.let { target ->
        val presetCount = uiState.presetCountOf(target.id)
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = MaterialTheme.colorScheme.surface,
            title = { Text(target.name) },
            text  = {
                if (presetCount > 0) {
                    Text(stringResource(R.string.group_delete_warning), color = DangerRed)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) { Text(stringResource(R.string.delete_confirm), color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.groups_title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = PrimaryGreen) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.groups_title),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding      = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.groups, key = { it.id }) { group ->
                GroupRow(
                    group    = group,
                    onEdit   = { onEdit(group) },
                    onDelete = { deleteTarget = group }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupRow(
    group: PresetGroup,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
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
                label = "group_swipe_bg"
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
                modifier          = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(group.colorHex.toComposeColor())
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text       = group.name,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
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

@Composable
private fun GroupDialog(
    initial: PresetGroup,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String) -> Unit
) {
    var name  by remember(initial) { mutableStateOf(initial.name) }
    var color by remember(initial) { mutableStateOf(initial.colorHex) }

    val canSubmit = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text       = stringResource(R.string.groups_title),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text(stringResource(R.string.group_name)) },
                    singleLine    = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text       = stringResource(R.string.group_color),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                ColorPalette(
                    selected = color,
                    onSelect = { color = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, color) },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPalette(
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp)
    ) {
        PresetColors.forEach { hex ->
            val isSelected = hex.equals(selected, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(hex.toComposeColor())
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun PresetGroupsContentPreview() {
    ServeraTheme {
        PresetGroupsContent(
            uiState = PresetGroupsUiState(
                groups = listOf(
                    PresetGroup(1, "Docker", "#1565C0", 0),
                    PresetGroup(2, "System", "#2E7D32", 1),
                    PresetGroup(3, "Network", "#AD1457", 2)
                ),
                presetCounts = mapOf(1L to 4, 2L to 2)
            ),
            onBack    = {},
            onAdd     = {},
            onEdit    = {},
            onDelete  = {},
            onDismiss = {},
            onSave    = { _, _ -> }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun GroupDialogPreview() {
    ServeraTheme {
        GroupDialog(
            initial   = PresetGroup(0, "Docker", "#1565C0", 0),
            isNew     = true,
            onDismiss = {},
            onSave    = { _, _ -> }
        )
    }
}
