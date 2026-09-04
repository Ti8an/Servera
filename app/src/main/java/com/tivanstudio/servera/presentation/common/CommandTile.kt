package com.tivanstudio.servera.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tivanstudio.servera.R
import com.tivanstudio.servera.presentation.theme.PrimaryGreen
import com.tivanstudio.servera.presentation.theme.presetIconOf

/** Tiles per row. Change it and [rememberCommandTileWidth] follows on its own. */
const val CommandTileColumns = 3

val CommandTileHeight = 92.dp
val CommandTileSpacing = 8.dp
val CommandGridPadding = 16.dp

/**
 * FlowRow gives the tiles no intrinsic width, and weight() would stretch a lone tile across the
 * whole row, so the column width is split up front instead.
 */
@Composable
fun rememberCommandTileWidth(columns: Int = CommandTileColumns): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    return (screenWidth - CommandGridPadding * 2 - CommandTileSpacing * (columns - 1)) / columns
}

/**
 * One command as a tile: label on top, the command itself along the bottom, and the preset icon
 * as a watermark behind both. Shared by the presets grid and the console's attached commands, so
 * the two read as the same object in two places.
 *
 * [badge] takes the bottom-right corner beside the command — the console puts its run state there;
 * without one the corner is held open so the command text wraps the same way either way.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommandTile(
    label: String,
    command: String,
    iconKey: String?,
    accentColor: Color,
    width: Dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    badge: @Composable (() -> Unit)? = null,
    contentDescription: String? = null
) {
    val description = contentDescription ?: "$label: $command"

    Card(
        shape  = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        // A highlighted tile has to read as highlighted from across the grid, not from a 12 dp
        // badge alone.
        border = BorderStroke(
            1.dp,
            accentColor.copy(alpha = if (highlighted) 0.6f else 0.25f)
        ),
        modifier = Modifier
            .size(width = width, height = CommandTileHeight)
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick     = onClick,
                onLongClick = onLongClick
            )
            .semantics { this.contentDescription = description }
    ) {
        Box {
            // Watermark: it runs off the right edge on purpose, and the card's shape clips it.
            Icon(
                presetIconOf(iconKey),
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.10f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(56.dp)
                    .offset(x = 12.dp)
            )

            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text       = label,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )

                Spacer(Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ~15 monospaced characters fit here, so most commands are cut. Ellipsis
                    // says there is more; a flush cut would read as a rendering glitch.
                    Text(
                        text       = command,
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 10.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines   = 1,
                        softWrap   = false,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f)
                    )
                    if (badge != null) badge() else Spacer(Modifier.size(12.dp))
                }
            }
        }
    }
}

/**
 * A catalog command offered for the taking: tap adds it, a long press offers to drop it into the
 * form for a tweak first. Shared by the console's catalog tab and the preset library dialog.
 *
 * One already taken keeps its tile — hiding it would make the catalog shift under the finger —
 * and marks itself with a tick and a brighter border instead.
 */
@Composable
fun CommandPickerTile(
    label: String,
    command: String,
    iconKey: String?,
    accentColor: Color,
    width: Dp,
    isAdded: Boolean,
    onAdd: () -> Unit,
    onCopyToForm: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // The tick is 12 dp of colour with no label of its own, so "already added" is spoken here:
    // without it every tile in the grid reads the same to TalkBack.
    val addedText   = stringResource(R.string.already_added)
    val description = if (isAdded) "$label: $command, $addedText" else "$label: $command"

    Box {
        CommandTile(
            label       = label,
            command     = command,
            iconKey     = iconKey,
            accentColor = accentColor,
            width       = width,
            onClick     = onAdd,
            onLongClick = { menuExpanded = true },
            highlighted = isAdded,
            badge = if (isAdded) {
                {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint     = PrimaryGreen,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else null,
            contentDescription = description
        )

        DropdownMenu(
            expanded         = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text        = { Text(stringResource(R.string.copy_to_form)) },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onCopyToForm()
                }
            )
        }
    }
}
