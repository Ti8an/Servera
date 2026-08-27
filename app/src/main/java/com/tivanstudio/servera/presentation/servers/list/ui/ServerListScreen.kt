package com.tivanstudio.servera.presentation.servers.list.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.tivanstudio.servera.presentation.components.AppBottomBar
import com.tivanstudio.servera.presentation.navigation.Screen
import com.tivanstudio.servera.presentation.servers.list.viewmodel.ServerListUiState
import com.tivanstudio.servera.presentation.servers.list.viewmodel.ServerListViewModel
import com.tivanstudio.servera.presentation.servers.list.viewmodel.ServerUiModel
import com.tivanstudio.servera.presentation.theme.*

@Composable
fun ServerListScreen(
    viewModel: ServerListViewModel = hiltViewModel(),
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToConsole: (Long) -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ServerListContent(
        uiState = uiState,
        onNavigateToAdd = onNavigateToAdd,
        onNavigateToEdit = onNavigateToEdit,
        onNavigateToConsole = onNavigateToConsole,
        onNavigateToPresets = onNavigateToPresets,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
        onSearch = viewModel::onSearch,
        onDelete = viewModel::deleteServer,
        onCheckOne = viewModel::checkOne,
        onDismissStatusError = viewModel::dismissStatusError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerListContent(
    uiState: ServerListUiState,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToConsole: (Long) -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSearch: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onCheckOne: (Long) -> Unit,
    onDismissStatusError: () -> Unit
) {
    var searchVisible by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Long?>(null) }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_server_title)) },
            text  = { Text(stringResource(R.string.delete_server_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(deleteTarget!!)
                    deleteTarget = null
                }) { Text(stringResource(R.string.delete_confirm), color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    uiState.statusError?.let { statusError ->
        val serverName = uiState.servers.firstOrNull { it.id == statusError.serverId }?.name.orEmpty()
        AlertDialog(
            onDismissRequest = onDismissStatusError,
            title = { Text(stringResource(R.string.status_problem_title)) },
            text  = { Text("$serverName\n\n${stringResource(statusError.messageRes)}") },
            confirmButton = {
                TextButton(onClick = onDismissStatusError) { Text(stringResource(R.string.ok)) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchVisible) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearch,
                            placeholder = { Text(stringResource(R.string.search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor      = PrimaryGreen,
                                unfocusedBorderColor    = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(stringResource(R.string.servers_title), fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { searchVisible = !searchVisible }) {
                        Icon(if (searchVisible) Icons.Default.Close else Icons.Default.Search, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = Screen.ServerList.route,
                onServers  = {},
                onPresets  = onNavigateToPresets,
                onHistory  = onNavigateToHistory,
                onSettings = onNavigateToSettings
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToAdd,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryGreen)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_server_button), color = PrimaryGreen)
            }

            Spacer(Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryGreen)
                    }
                }
                uiState.servers.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.empty_servers),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                else -> {
                    val filtered = uiState.servers.filter {
                        uiState.searchQuery.isBlank() ||
                        it.name.contains(uiState.searchQuery, ignoreCase = true) ||
                        it.host.contains(uiState.searchQuery, ignoreCase = true)
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filtered, key = { it.id }) { server ->
                            ServerListItem(
                                server     = server,
                                onClick    = { onNavigateToConsole(server.id) },
                                onEdit     = { onNavigateToEdit(server.id) },
                                onDelete   = { deleteTarget = server.id },
                                onCheckOne = { onCheckOne(server.id) }
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerListItem(
    server: ServerUiModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCheckOne: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when {
            server.isCorrupted       -> DangerRed
            server.isChecking        -> MaterialTheme.colorScheme.onSurfaceVariant
            server.isOnline == true  -> PrimaryGreen
            server.isOnline == false -> DangerRed
            else                     -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "status_color"
    )

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { true }
    )

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.StartToEnd -> {
                if (!server.isCorrupted) onEdit()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            else -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val swipeDir = dismissState.targetValue
            val bgColor by animateColorAsState(
                targetValue = when {
                    swipeDir == SwipeToDismissBoxValue.EndToStart -> DangerRed
                    swipeDir == SwipeToDismissBoxValue.StartToEnd && !server.isCorrupted -> PrimaryGreen
                    else                                          -> Color.Transparent
                },
                label = "swipe_bg_color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (swipeDir) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else                             -> Alignment.CenterStart
                }
            ) {
                when {
                    swipeDir == SwipeToDismissBoxValue.EndToStart ->
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                    swipeDir == SwipeToDismissBoxValue.StartToEnd && !server.isCorrupted ->
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                    else -> {}
                }
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !server.isCorrupted, onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape  = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (server.isCorrupted) Icons.Default.LockReset else Icons.Default.Dns,
                    contentDescription = null,
                    tint = if (server.isCorrupted) DangerRed else InfoBlue,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${server.login}@${server.host}:${server.port}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(statusColor, shape = MaterialTheme.shapes.small)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when {
                                server.isCorrupted       -> stringResource(R.string.status_corrupted)
                                server.isChecking        -> stringResource(R.string.status_checking)
                                server.isOnline == true  -> stringResource(R.string.status_online)
                                server.isOnline == false -> stringResource(R.string.status_offline)
                                else                     -> stringResource(R.string.status_unknown)
                            },
                            color = statusColor,
                            fontSize = 11.sp
                        )
                    }
                    if (server.isCorrupted) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.server_corrupted_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                if (server.isCorrupted) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_confirm),
                            tint = DangerRed
                        )
                    }
                } else IconButton(onClick = onCheckOne, enabled = !server.isChecking) {
                    if (server.isChecking) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            color       = PrimaryGreen,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.NetworkCheck,
                            contentDescription = stringResource(R.string.check_status),
                            tint = PrimaryGreen
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ServerListContentPreview() {
    ServeraTheme {
        ServerListContent(
            uiState = ServerListUiState(
                isLoading = false,
                servers = listOf(
                    ServerUiModel(1, "Production", "192.168.1.1", 22, "root", isOnline = true),
                    ServerUiModel(2, "Staging", "10.0.0.1", 22, "deploy", isOnline = false),
                    ServerUiModel(3, "Dev", "172.16.0.1", 2222, "admin", isOnline = null),
                    ServerUiModel(4, "Backup", "172.16.0.2", 22, "admin", isChecking = true)
                ),
                statusError = null
            ),
            onNavigateToAdd = {},
            onNavigateToEdit = {},
            onNavigateToConsole = {},
            onNavigateToPresets = {},
            onNavigateToHistory = {},
            onNavigateToSettings = {},
            onSearch = {},
            onDelete = {},
            onCheckOne = {},
            onDismissStatusError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ServerListItemOnlinePreview() {
    ServeraTheme {
        ServerListItem(
            server = ServerUiModel(1, "Production Server", "192.168.1.100", 22, "root", isOnline = true),
            onClick = {},
            onEdit = {},
            onDelete = {},
            onCheckOne = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ServerListItemOfflinePreview() {
    ServeraTheme {
        ServerListItem(
            server = ServerUiModel(2, "Staging Server", "10.0.0.1", 22, "deploy", isOnline = false),
            onClick = {},
            onEdit = {},
            onDelete = {},
            onCheckOne = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ServerListItemUncheckedPreview() {
    ServeraTheme {
        ServerListItem(
            server = ServerUiModel(3, "Dev Server", "172.16.0.1", 2222, "admin", isOnline = null),
            onClick = {},
            onEdit = {},
            onDelete = {},
            onCheckOne = {}
        )
    }
}
