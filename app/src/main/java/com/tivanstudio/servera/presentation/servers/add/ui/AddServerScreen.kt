package com.tivanstudio.servera.presentation.servers.add.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
import com.tivanstudio.servera.presentation.servers.add.viewmodel.AddServerEvent
import com.tivanstudio.servera.presentation.servers.add.viewmodel.AddServerViewModel
import com.tivanstudio.servera.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(
    viewModel: AddServerViewModel = hiltViewModel(),
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddServerEvent.Saved      -> onSaved()
                is AddServerEvent.ShowError  -> Toast.makeText(context, event.msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditing) stringResource(R.string.edit_server_title)
                        else stringResource(R.string.add_server_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
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

            AppTextField(value = uiState.name,  label = stringResource(R.string.server_name_hint),  onValueChange = viewModel::onNameChange)
            AppTextField(value = uiState.host,  label = stringResource(R.string.server_host_hint),  onValueChange = viewModel::onHostChange)
            AppTextField(
                value = uiState.port,
                label = stringResource(R.string.server_port_hint),
                onValueChange = viewModel::onPortChange,
                keyboardType = KeyboardType.Number
            )
            AppTextField(value = uiState.login, label = stringResource(R.string.server_login_hint), onValueChange = viewModel::onLoginChange)

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text(stringResource(R.string.server_password_hint)) },
                singleLine = true,
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = viewModel::onTogglePassword) {
                        Icon(
                            if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                },
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            OutlinedButton(
                onClick = viewModel::onToggleAdvanced,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.server_advanced), color = TextSecondary)
                Spacer(Modifier.weight(1f))
                Icon(
                    if (uiState.isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }

            AnimatedVisibility(visible = uiState.isAdvancedExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.privateKey,
                        onValueChange = viewModel::onPrivateKeyChange,
                        label = { Text(stringResource(R.string.server_private_key_hint)) },
                        minLines = 4,
                        maxLines = 8,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppTextField(
                        value = uiState.timeout,
                        label = stringResource(R.string.server_timeout_hint),
                        onValueChange = viewModel::onTimeoutChange,
                        keyboardType = KeyboardType.Number
                    )
                }
            }

            uiState.testResult?.let { ok ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ok) PrimaryGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (ok) PrimaryGreen else DangerRed
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(if (ok) R.string.connection_ok else R.string.connection_failed),
                            color = if (ok) PrimaryGreen else DangerRed
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = viewModel::testConn,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isTesting && !uiState.isLoading,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uiState.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryGreen)
                    } else {
                        Text(stringResource(R.string.test_button), color = PrimaryGreen)
                    }
                }
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.weight(2f),
                    enabled = !uiState.isLoading && !uiState.isTesting,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(R.string.save_button), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AppTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor   = Elevated,
    unfocusedContainerColor = Elevated,
    focusedBorderColor      = PrimaryGreen,
    unfocusedBorderColor    = Surface
)
