package com.tivanstudio.servera.presentation.console.execute.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
import com.tivanstudio.servera.presentation.console.execute.viewmodel.ExecuteCommandEvent
import com.tivanstudio.servera.presentation.console.execute.viewmodel.ExecuteCommandUiState
import com.tivanstudio.servera.presentation.console.execute.viewmodel.ExecuteCommandViewModel
import com.tivanstudio.servera.presentation.theme.*

private val EXAMPLE_COMMANDS = listOf(
    "uname -a", "df -h", "free -h", "top -bn1", "ps aux", "uptime", "whoami", "pwd", "ls -la"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecuteCommandScreen(
    viewModel: ExecuteCommandViewModel = hiltViewModel(),
    onResult: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExecuteCommandEvent.NavigateToResult -> onResult()
                is ExecuteCommandEvent.ShowError        -> {}
            }
        }
    }

    ExecuteCommandContent(
        uiState = uiState,
        onCommandChange = viewModel::onCommandChange,
        onSetCommand = viewModel::setCommand,
        onExecute = viewModel::execute,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExecuteCommandContent(
    uiState: ExecuteCommandUiState,
    onCommandChange: (String) -> Unit,
    onSetCommand: (String) -> Unit,
    onExecute: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.execute_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.command,
                onValueChange = onCommandChange,
                label = { Text(stringResource(R.string.command_label)) },
                placeholder = { Text("uname -a", fontFamily = FontFamily.Monospace, color = TextSecondary) },
                minLines = 4,
                maxLines = 10,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = Elevated,
                    unfocusedContainerColor = Elevated,
                    focusedBorderColor      = PrimaryGreen,
                    unfocusedBorderColor    = Surface
                ),
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.error != null
            )

            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Text(stringResource(R.string.example_commands), color = TextSecondary, fontSize = 12.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EXAMPLE_COMMANDS) { cmd ->
                    SuggestionChip(
                        onClick = { onSetCommand(cmd) },
                        label = { Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Elevated)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onExecute,
                enabled = !uiState.isExecuting && uiState.command.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isExecuting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.executing_label), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.execute_button), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExecuteCommandContentPreview() {
    ServeraTheme {
        ExecuteCommandContent(
            uiState = ExecuteCommandUiState(command = "ls -la /etc"),
            onCommandChange = {},
            onSetCommand = {},
            onExecute = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExecuteCommandContentExecutingPreview() {
    ServeraTheme {
        ExecuteCommandContent(
            uiState = ExecuteCommandUiState(command = "top -bn1", isExecuting = true),
            onCommandChange = {},
            onSetCommand = {},
            onExecute = {},
            onBack = {}
        )
    }
}
