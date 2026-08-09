package com.tivanstudio.servera.presentation.console.result.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.entity.CommandResult
import com.tivanstudio.servera.presentation.console.result.viewmodel.CommandResultUiState
import com.tivanstudio.servera.presentation.console.result.viewmodel.CommandResultViewModel
import com.tivanstudio.servera.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandResultScreen(
    viewModel: CommandResultViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val copiedText = stringResource(R.string.copied)

    CommandResultContent(
        uiState = uiState,
        onBack = onBack,
        context = context,
        copiedText = copiedText
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandResultContent(
    uiState: CommandResultUiState,
    onBack: () -> Unit,
    context: Context,
    copiedText: String
) {
    val result = uiState.result
    // Metadata is always present; the output only when it was actually stored.
    val hasOutput = uiState.outputSaved && result != null
    val copyText  = buildCopyText(uiState, stringResource(R.string.result_not_saved))

    val copyAll = {
        copyToClipboard(context, copyText)
        Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                },
                actions = {
                    IconButton(onClick = copyAll) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.result_copy),
                            tint = PrimaryGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            if (uiState.serverName != null || uiState.serverHost != null || uiState.groupName != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        uiState.serverName?.let { MetaRow(stringResource(R.string.result_server), it) }
                        uiState.serverHost?.let { MetaRow(stringResource(R.string.result_ip), it) }
                        uiState.groupName?.let { MetaRow(stringResource(R.string.result_group), it) }
                    }
                }
            }

            uiState.command?.let { command ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = command,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            copyToClipboard(context, command)
                            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            uiState.exitCode?.let { code ->
                Badge(
                    containerColor = if (code == 0) PrimaryGreen else DangerRed,
                    contentColor   = MaterialTheme.colorScheme.onSurface
                ) {
                    Text(
                        stringResource(R.string.exit_code_label, code),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (result != null && result.durationMs > 0) {
                Text(
                    stringResource(R.string.duration_label, result.durationMs),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            if (hasOutput) {
                ResultSection(
                    title = stringResource(R.string.stdout_label),
                    content = result.stdout,
                    context = context,
                    copiedText = copiedText
                )
                if (result.stderr.isNotBlank()) {
                    ResultSection(
                        title = stringResource(R.string.stderr_label),
                        content = result.stderr,
                        context = context,
                        copiedText = copiedText
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.no_data),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = copyAll,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryGreen)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.result_copy), color = PrimaryGreen)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f)
        )
    }
}

/** The whole card as plain text: metadata, command, exit code, then the output. */
@Composable
private fun buildCopyText(uiState: CommandResultUiState, notSavedLabel: String): String {
    val serverLabel = stringResource(R.string.result_server)
    val ipLabel     = stringResource(R.string.result_ip)
    val groupLabel  = stringResource(R.string.result_group)
    val cmdLabel    = stringResource(R.string.command_label)
    val stderrLabel = stringResource(R.string.stderr_label)
    val exitLabel   = uiState.exitCode?.let { stringResource(R.string.exit_code_label, it) }
    val result      = uiState.result

    return buildString {
        uiState.serverName?.let { appendLine("$serverLabel: $it") }
        uiState.serverHost?.let { appendLine("$ipLabel: $it") }
        uiState.groupName?.let { appendLine("$groupLabel: $it") }
        uiState.command?.let { appendLine("$cmdLabel: $it") }
        exitLabel?.let { appendLine(it) }
        appendLine()
        if (uiState.outputSaved && result != null) {
            appendLine(result.stdout)
            if (result.stderr.isNotBlank()) {
                appendLine("$stderrLabel:")
                appendLine(result.stderr)
            }
        } else {
            appendLine("($notSavedLabel)")
        }
    }
}

@Composable
private fun ResultSection(title: String, content: String, context: Context, copiedText: String) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (content.isNotBlank()) {
                IconButton(
                    onClick = {
                        copyToClipboard(context, content)
                        Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 300.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = content.ifBlank { stringResource(R.string.empty_output) },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = if (content.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("output", text))
}

@Preview(showBackground = true)
@Composable
private fun CommandResultContentPreview() {
    val context = LocalContext.current
    ServeraTheme {
        CommandResultContent(
            uiState = CommandResultUiState(
                result = CommandResult(
                    command = "ls -la /etc",
                    stdout = "total 1234\ndrwxr-xr-x 1 root root\n-rw-r--r-- 1 root root",
                    stderr = "",
                    exitCode = 0,
                    durationMs = 142
                ),
                serverName  = "Production",
                serverHost  = "192.168.1.1",
                groupName   = "System",
                command     = "ls -la /etc",
                exitCode    = 0,
                outputSaved = true
            ),
            onBack = {},
            context = context,
            copiedText = "Copied!"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CommandResultContentNotSavedPreview() {
    val context = LocalContext.current
    ServeraTheme {
        CommandResultContent(
            uiState = CommandResultUiState(
                result      = null,
                serverName  = "Staging",
                serverHost  = "10.0.0.1",
                groupName   = "Docker",
                command     = "docker ps",
                exitCode    = 1,
                outputSaved = false
            ),
            onBack = {},
            context = context,
            copiedText = "Copied!"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultSectionPreview() {
    val context = LocalContext.current
    ServeraTheme {
        ResultSection(
            title = "STDOUT",
            content = "total 1234\ndrwxr-xr-x 1 root root\n-rw-r--r-- 1 root root",
            context = context,
            copiedText = "Copied!"
        )
    }
}
