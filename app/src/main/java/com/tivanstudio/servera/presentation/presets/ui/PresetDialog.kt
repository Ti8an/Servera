package com.tivanstudio.servera.presentation.presets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.presentation.theme.PrimaryGreen
import com.tivanstudio.servera.presentation.theme.ServeraTheme
import com.tivanstudio.servera.presentation.theme.toComposeColor

/** Shared by the presets screen and the console's add-command dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetDialog(
    initial: Preset,
    groups: List<PresetGroup>,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (groupId: Long, label: String, command: String) -> Unit
) {
    var groupId by remember(initial) { mutableStateOf(initial.groupId) }
    var label   by remember(initial) { mutableStateOf(initial.label) }
    var command by remember(initial) { mutableStateOf(initial.command) }
    var expanded by remember { mutableStateOf(false) }

    val selectedGroup = groups.firstOrNull { it.id == groupId }
    val canSubmit = selectedGroup != null && label.isNotBlank() && command.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text       = stringResource(R.string.presets_title),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded         = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value         = selectedGroup?.name.orEmpty(),
                        onValueChange = {},
                        readOnly      = true,
                        singleLine    = true,
                        label         = { Text(stringResource(R.string.groups_title)) },
                        leadingIcon = selectedGroup?.let { group ->
                            {
                                GroupDot(colorHex = group.colorHex, size = 14)
                            }
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = PrimaryGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded         = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        GroupDot(colorHex = group.colorHex, size = 12)
                                        Spacer(Modifier.width(8.dp))
                                        Text(group.name)
                                    }
                                },
                                onClick = {
                                    groupId  = group.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value         = label,
                    onValueChange = { label = it },
                    label         = { Text(stringResource(R.string.preset_label)) },
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
                    label         = { Text(stringResource(R.string.preset_command)) },
                    singleLine    = false,
                    minLines      = 4,
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(groupId, label, command) },
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
fun GroupDot(colorHex: String, size: Int) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(colorHex.toComposeColor())
    )
}

@Preview(showBackground = true)
@Composable
private fun PresetDialogPreview() {
    ServeraTheme {
        PresetDialog(
            initial = Preset(0, 1, "Running containers", "docker ps", 0),
            groups  = listOf(
                PresetGroup(1, "Docker", "#1565C0", 0),
                PresetGroup(2, "System", "#2E7D32", 1)
            ),
            isNew     = true,
            onDismiss = {},
            onSave    = { _, _, _ -> }
        )
    }
}
