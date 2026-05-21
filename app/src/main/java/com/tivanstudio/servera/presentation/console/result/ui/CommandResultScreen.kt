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
    onBack: () -> Unit,
    onRepeat: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val copiedText = stringResource(R.string.copied)

    CommandResultContent(
        uiState = uiState,
        onBack = onBack,
        onRepeat = onRepeat,
        context = context,
        copiedText = copiedText
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandResultContent(
    uiState: CommandResultUiState,
    onBack: () -> Unit,
    onRepeat: () -> Unit,
    context: Context,
    copiedText: String
) {
    val result = uiState.result

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                },
                actions = {
                    if (result != null) {
                        IconButton(onClick = {
                            val text = buildString {
                                appendLine("# ${result.command}")
                                appendLine("Exit: ${result.exitCode}")
                                appendLine("STDOUT:\n${result.stdout}")
                                if (result.stderr.isNotBlank()) appendLine("STDERR:\n${result.stderr}")
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("result", text))
                            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryGreen)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        if (result == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_data), color = TextSecondary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = result.command,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        copyToClipboard(context, result.command)
                        Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary)
                    }
                }
            }

            Badge(
                containerColor = if (result.exitCode == 0) PrimaryGreen else DangerRed,
                contentColor   = TextPrimary
            ) {
                Text(
                    "Exit code: ${result.exitCode}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Medium
                )
            }

            Text(stringResource(R.string.duration_label, result.durationMs), color = TextSecondary, fontSize = 12.sp)

            ResultSection(title = stringResource(R.string.stdout_label), content = result.stdout, context = context, copiedText = copiedText)
            ResultSection(title = stringResource(R.string.stderr_label), content = result.stderr, context = context, copiedText = copiedText)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRepeat,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, tint = PrimaryGreen)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.result_repeat), color = PrimaryGreen)
                }
                OutlinedButton(
                    onClick = {
                        copyToClipboard(context, result.stdout)
                        Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryGreen)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.result_copy), color = PrimaryGreen)
                }
            }

            Spacer(Modifier.height(16.dp))
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
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
            if (content.isNotBlank()) {
                IconButton(
                    onClick = {
                        copyToClipboard(context, content)
                        Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Elevated),
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
                    color = if (content.isBlank()) TextSecondary else TextPrimary
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
                )
            ),
            onBack = {},
            onRepeat = {},
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
