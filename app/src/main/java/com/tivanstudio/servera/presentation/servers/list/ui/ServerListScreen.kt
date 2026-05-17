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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.presentation.components.AppBottomBar
import com.tivanstudio.servera.presentation.navigation.Screen
import com.tivanstudio.servera.presentation.servers.list.viewmodel.ServerListViewModel
import com.tivanstudio.servera.presentation.servers.list.viewmodel.ServerUiModel
import com.tivanstudio.servera.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    viewModel: ServerListViewModel = hiltViewModel(),
    onNavigateToAdd: () -> Unit,
    onNavigateToConsole: (Long) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchVisible by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Long?>(null) }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Удалить сервер?") },
            text  = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteServer(deleteTarget!!)
                    deleteTarget = null
                }) { Text("Удалить", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Отмена") }
            },
            containerColor = Surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchVisible) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearch,
                            placeholder = { Text("Поиск…", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor   = Elevated,
                                unfocusedContainerColor = Elevated,
                                focusedBorderColor      = PrimaryGreen,
                                unfocusedBorderColor    = Surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Мои серверы", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { searchVisible = !searchVisible }) {
                        Icon(if (searchVisible) Icons.Default.Close else Icons.Default.Search, contentDescription = null)
                    }
                    IconButton(onClick = viewModel::refreshStatus) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = Screen.ServerList.route,
                onServers  = {},
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
                Text("+ Добавить сервер", color = PrimaryGreen)
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
                                tint = TextSecondary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Нет серверов.\nДобавьте первый!",
                                color = TextSecondary,
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
                                server   = server,
                                onClick  = { onNavigateToConsole(server.id) },
                                onEdit   = { onNavigateToAdd() },
                                onDelete = { deleteTarget = server.id }
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerListItem(
    server: ServerUiModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when {
            server.isChecking -> TextSecondary
            server.isOnline   -> PrimaryGreen
            else              -> DangerRed
        },
        label = "status_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape  = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Dns,
                contentDescription = null,
                tint = InfoBlue,
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
                    color = TextSecondary,
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
                            server.isChecking -> "Проверка…"
                            server.isOnline   -> "В сети"
                            else              -> "Не в сети"
                        },
                        color = statusColor,
                        fontSize = 11.sp
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
            }
        }
    }
}
