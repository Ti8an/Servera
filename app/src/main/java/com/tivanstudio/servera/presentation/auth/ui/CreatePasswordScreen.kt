package com.tivanstudio.servera.presentation.auth.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
import com.tivanstudio.servera.presentation.auth.viewmodel.CreatePasswordEvent
import com.tivanstudio.servera.presentation.auth.viewmodel.CreatePasswordUiState
import com.tivanstudio.servera.presentation.auth.viewmodel.CreatePasswordViewModel
import com.tivanstudio.servera.presentation.theme.Elevated
import com.tivanstudio.servera.presentation.theme.PrimaryGreen
import com.tivanstudio.servera.presentation.theme.ServeraTheme
import com.tivanstudio.servera.presentation.theme.Surface
import com.tivanstudio.servera.presentation.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePasswordScreen(
    viewModel: CreatePasswordViewModel = hiltViewModel(),
    onPasswordCreated: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CreatePasswordEvent.PasswordCreated -> onPasswordCreated()
                is CreatePasswordEvent.ShowError       -> Toast.makeText(context, event.msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    CreatePasswordContent(
        uiState = uiState,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onToggleVisibility = viewModel::onToggleVisibility,
        onCreatePassword = viewModel::createPassword,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePasswordContent(
    uiState: CreatePasswordUiState,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onCreatePassword: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_password_title)) },
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.create_password_hint)) },
                singleLine = true,
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = Elevated,
                    unfocusedContainerColor = Elevated,
                    focusedBorderColor      = PrimaryGreen,
                    unfocusedBorderColor    = Surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.confirm,
                onValueChange = onConfirmChange,
                label = { Text(stringResource(R.string.create_password_confirm_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onCreatePassword,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.create_password_button), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreatePasswordContentPreview() {
    ServeraTheme {
        CreatePasswordContent(
            uiState = CreatePasswordUiState(password = "", confirm = "", isLoading = false),
            onPasswordChange = {},
            onConfirmChange = {},
            onToggleVisibility = {},
            onCreatePassword = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreatePasswordContentErrorPreview() {
    ServeraTheme {
        CreatePasswordContent(
            uiState = CreatePasswordUiState(
                password = "pass",
                confirm = "pass2",
                error = "Passwords do not match"
            ),
            onPasswordChange = {},
            onConfirmChange = {},
            onToggleVisibility = {},
            onCreatePassword = {},
            onBack = {}
        )
    }
}
