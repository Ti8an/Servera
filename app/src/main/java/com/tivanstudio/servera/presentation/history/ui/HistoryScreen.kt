package com.tivanstudio.servera.presentation.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.tivanstudio.servera.domain.entity.CommandHistory
import com.tivanstudio.servera.domain.entity.PresetGroup
import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.presentation.components.AppBottomBar
import com.tivanstudio.servera.presentation.history.viewmodel.HistoryEvent
import com.tivanstudio.servera.presentation.history.viewmodel.HistoryFilter
import com.tivanstudio.servera.presentation.history.viewmodel.HistoryPeriod
import com.tivanstudio.servera.presentation.history.viewmodel.HistoryStatus
import com.tivanstudio.servera.presentation.history.viewmodel.HistoryUiState
import com.tivanstudio.servera.presentation.history.viewmodel.HistoryViewModel
import com.tivanstudio.servera.presentation.navigation.Screen
import com.tivanstudio.servera.presentation.presets.ui.GroupDot
import com.tivanstudio.servera.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onNavigateToServers: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToResult: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HistoryEvent.NavigateToResult -> onNavigateToResult()
            }
        }
    }

    HistoryContent(
        uiState = uiState,
        onNavigateToServers = onNavigateToServers,
        onNavigateToPresets = onNavigateToPresets,
        onNavigateToSettings = onNavigateToSettings,
        onClearAll = viewModel::clearAll,
        onOpenFilter = viewModel::openFilter,
        onCloseFilter = viewModel::closeFilter,
        onUpdateFilter = viewModel::updateFilter,
        onResetFilter = viewModel::resetFilter,
        onOpenResult = viewModel::openResult
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onNavigateToServers: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onClearAll: () -> Unit,
    onOpenFilter: () -> Unit,
    onCloseFilter: () -> Unit,
    onUpdateFilter: (HistoryFilter) -> Unit,
    onResetFilter: () -> Unit,
    onOpenResult: (CommandHistory) -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text  = { Text(stringResource(R.string.delete_server_message)) },
            confirmButton = {
                TextButton(onClick = { onClearAll(); showClearDialog = false }) {
                    Text(stringResource(R.string.clear_history), color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (uiState.showFilterSheet) {
        FilterSheet(
            uiState  = uiState,
            onApply  = { onUpdateFilter(it); onCloseFilter() },
            onDismiss = onCloseFilter
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenFilter) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.filter_title)
                            )
                            if (uiState.filter.isActive) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGreen)
                                )
                            }
                        }
                    }
                    if (uiState.allHistory.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = Screen.History.route,
                onServers    = onNavigateToServers,
                onPresets    = onNavigateToPresets,
                onHistory    = {},
                onSettings   = onNavigateToSettings
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (uiState.filter.isActive) {
                ActiveFilterChips(
                    uiState        = uiState,
                    onUpdateFilter = onUpdateFilter
                )
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryGreen)
                    }
                }
                // Nothing recorded at all — the filter is not why the list is empty.
                uiState.allHistory.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.empty_history),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                uiState.filtered.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.no_history_for_filter),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = onResetFilter) {
                                Text(stringResource(R.string.filter_reset), color = PrimaryGreen)
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Spacer(Modifier.height(8.dp)) }
                        items(uiState.filtered, key = { it.id }) { item ->
                            HistoryItemCard(item = item, onClick = { onOpenResult(item) })
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

/** One dismissible chip per active filter parameter. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilterChips(
    uiState: HistoryUiState,
    onUpdateFilter: (HistoryFilter) -> Unit
) {
    val filter = uiState.filter

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filter.serverId?.let { id ->
            val name = uiState.servers.firstOrNull { it.id == id }?.name ?: "#$id"
            DismissibleFilterChip(
                label   = "${stringResource(R.string.filter_server)}: $name",
                onClear = { onUpdateFilter(filter.copy(serverId = null)) }
            )
        }
        filter.groupName?.let { group ->
            DismissibleFilterChip(
                label   = "${stringResource(R.string.filter_group)}: $group",
                onClear = { onUpdateFilter(filter.copy(groupName = null)) }
            )
        }
        if (filter.status != HistoryStatus.ALL) {
            DismissibleFilterChip(
                label   = stringResource(
                    if (filter.status == HistoryStatus.SUCCESS) R.string.filter_status_success
                    else R.string.filter_status_error
                ),
                onClear = { onUpdateFilter(filter.copy(status = HistoryStatus.ALL)) }
            )
        }
        if (filter.period != HistoryPeriod.ALL) {
            DismissibleFilterChip(
                label   = stringResource(
                    if (filter.period == HistoryPeriod.TODAY) R.string.filter_period_today
                    else R.string.filter_period_week
                ),
                onClear = { onUpdateFilter(filter.copy(period = HistoryPeriod.ALL)) }
            )
        }
        if (filter.query.isNotBlank()) {
            DismissibleFilterChip(
                label   = "${stringResource(R.string.filter_search)}: ${filter.query}",
                onClear = { onUpdateFilter(filter.copy(query = "")) }
            )
        }
    }
}

@Composable
private fun DismissibleFilterChip(label: String, onClear: () -> Unit) {
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

/**
 * Filters are edited on a local copy so a half-made selection never touches the
 * list; only "Apply" hands the result back.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    uiState: HistoryUiState,
    onApply: (HistoryFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var local by remember(uiState.filter) { mutableStateOf(uiState.filter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text       = stringResource(R.string.filter_title),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            FilterSectionTitle(stringResource(R.string.filter_server))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = local.serverId == null,
                    onClick  = { local = local.copy(serverId = null) },
                    label    = { Text(stringResource(R.string.filter_all)) }
                )
                uiState.servers.forEach { server ->
                    FilterChip(
                        selected = local.serverId == server.id,
                        onClick  = { local = local.copy(serverId = server.id) },
                        label    = { Text(server.name) }
                    )
                }
            }

            FilterSectionTitle(stringResource(R.string.filter_group))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = local.groupName == null,
                    onClick  = { local = local.copy(groupName = null) },
                    label    = { Text(stringResource(R.string.filter_all)) }
                )
                uiState.groups.forEach { group ->
                    FilterChip(
                        selected = local.groupName == group.name,
                        onClick  = { local = local.copy(groupName = group.name) },
                        label    = { Text(group.name) },
                        leadingIcon = { GroupDot(colorHex = group.colorHex, size = 10) }
                    )
                }
            }

            FilterSectionTitle(stringResource(R.string.filter_status))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    HistoryStatus.ALL     to R.string.filter_all,
                    HistoryStatus.SUCCESS to R.string.filter_status_success,
                    HistoryStatus.ERROR   to R.string.filter_status_error
                ).forEach { (status, res) ->
                    FilterChip(
                        selected = local.status == status,
                        onClick  = { local = local.copy(status = status) },
                        label    = { Text(stringResource(res)) }
                    )
                }
            }

            FilterSectionTitle(stringResource(R.string.filter_period))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    HistoryPeriod.ALL   to R.string.filter_period_all,
                    HistoryPeriod.TODAY to R.string.filter_period_today,
                    HistoryPeriod.WEEK  to R.string.filter_period_week
                ).forEach { (period, res) ->
                    FilterChip(
                        selected = local.period == period,
                        onClick  = { local = local.copy(period = period) },
                        label    = { Text(stringResource(res)) }
                    )
                }
            }

            OutlinedTextField(
                value         = local.query,
                onValueChange = { local = local.copy(query = it) },
                label         = { Text(stringResource(R.string.filter_search)) },
                singleLine    = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = PrimaryGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Row(
                modifier              = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { local = HistoryFilter() }) {
                    Text(
                        stringResource(R.string.filter_reset),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onApply(local) },
                    colors  = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape   = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.filter_apply), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        modifier   = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun HistoryItemCard(item: CommandHistory, onClick: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.command,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Badge(
                    containerColor = if (item.exitCode == 0) PrimaryGreen else DangerRed,
                    contentColor   = MaterialTheme.colorScheme.onSurface
                ) {
                    Text("${item.exitCode}", modifier = Modifier.padding(4.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            if (item.stdout.isNotBlank()) {
                Text(
                    text = item.stdout.lines().first(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fmt.format(Date(item.executedAt)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.groupName?.let { group ->
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text     = group,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun HistoryContentPreview() {
    ServeraTheme {
        HistoryContent(
            uiState = HistoryUiState(
                isLoading = false,
                allHistory = listOf(
                    CommandHistory(1, 1, "ls -la /etc", "total 256\ndrwxr-xr-x", "", 0, System.currentTimeMillis(), "System", resultSaved = true),
                    CommandHistory(2, 2, "df -h", "", "", 0, System.currentTimeMillis() - 60_000, "Docker", resultSaved = false),
                    CommandHistory(3, 1, "cat /etc/invalid", "", "No such file", 1, System.currentTimeMillis() - 120_000, resultSaved = true)
                ),
                servers = listOf(
                    Server(1, "Production", "192.168.1.1", 22, "root", ""),
                    Server(2, "Staging", "10.0.0.1", 22, "deploy", "")
                ),
                groups = listOf(
                    PresetGroup(1, "Docker", "#1565C0", 0),
                    PresetGroup(2, "System", "#2E7D32", 1)
                )
            ),
            onNavigateToServers = {},
            onNavigateToPresets = {},
            onNavigateToSettings = {},
            onClearAll = {},
            onOpenFilter = {},
            onCloseFilter = {},
            onUpdateFilter = {},
            onResetFilter = {},
            onOpenResult = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryItemCardPreview() {
    ServeraTheme {
        HistoryItemCard(
            item = CommandHistory(
                id = 1,
                serverId = 1,
                command = "ls -la /etc",
                stdout = "total 256\ndrwxr-xr-x 1 root root",
                stderr = "",
                exitCode = 0,
                executedAt = System.currentTimeMillis(),
                groupName = "System",
                resultSaved = true
            ),
            onClick = {}
        )
    }
}
