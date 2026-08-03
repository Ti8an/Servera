package com.tivanstudio.servera.presentation.presets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.entity.Preset
import com.tivanstudio.servera.presentation.theme.PrimaryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetDialog(
    initial: Preset,
    categories: List<String>,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (category: String, label: String, command: String) -> Unit
) {
    var category by remember(initial) { mutableStateOf(initial.category) }
    var label    by remember(initial) { mutableStateOf(initial.label) }
    var command  by remember(initial) { mutableStateOf(initial.command) }
    var expanded by remember { mutableStateOf(false) }

    val canSubmit = category.isNotBlank() && label.isNotBlank() && command.isNotBlank()

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
                        value         = category,
                        onValueChange = {
                            category = it
                            expanded = true
                        },
                        label       = { Text(stringResource(R.string.preset_category)) },
                        placeholder = { Text(stringResource(R.string.preset_new_category)) },
                        singleLine  = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = PrimaryGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                    )
                    if (categories.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded         = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { option ->
                                DropdownMenuItem(
                                    text    = { Text(option) },
                                    onClick = {
                                        category = option
                                        expanded = false
                                    }
                                )
                            }
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
                onClick = { onSave(category, label, command) },
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
