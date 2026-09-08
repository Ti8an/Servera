package com.tivanstudio.servera.presentation.common.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Pieces shared by every filter surface, so a filter looks and behaves the same wherever it
 * appears -- the history list and the catalog picker at the time of writing.
 */

/** One active filter parameter, tapped to clear it. */
@Composable
fun DismissibleFilterChip(label: String, onClear: () -> Unit) {
    AssistChip(
        onClick = onClear,
        label   = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

/** Heading above one group of controls in a filter sheet. */
@Composable
fun FilterSectionTitle(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        modifier   = Modifier.padding(top = 8.dp)
    )
}
