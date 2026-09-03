package com.tivanstudio.servera.presentation.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Upload
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The icons a preset can carry, by the key stored in the database.
 *
 * Keys are strings rather than [ImageVector]s so the domain model stays free of Compose. Insertion
 * order is the order the picker lays them out — `mapOf` gives a `LinkedHashMap`.
 */
val PresetIcons: Map<String, ImageVector> = mapOf(
    "terminal" to Icons.Default.Terminal,
    "storage"  to Icons.Default.Storage,
    "memory"   to Icons.Default.Memory,
    "cloud"    to Icons.Default.Cloud,
    "folder"   to Icons.Default.Folder,
    "refresh"  to Icons.Default.Refresh,
    "download" to Icons.Default.Download,
    "upload"   to Icons.Default.Upload,
    "lock"     to Icons.Default.Lock,
    "bug"      to Icons.Default.BugReport,
    "speed"    to Icons.Default.Speed,
    "power"    to Icons.Default.PowerSettingsNew
)

/** What a preset shows when it has no icon of its own, and what the picker highlights for one. */
const val DEFAULT_PRESET_ICON = "terminal"

/** A key dropped from [PresetIcons] by a later version falls back here instead of crashing. */
fun presetIconOf(key: String?): ImageVector = PresetIcons[key] ?: Icons.Default.Terminal
