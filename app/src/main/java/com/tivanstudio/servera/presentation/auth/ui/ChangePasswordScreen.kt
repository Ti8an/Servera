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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
import com.tivanstudio.servera.presentation.auth.PasswordStrength
import com.tivanstudio.servera.presentation.auth.viewmodel.ChangePasswordEvent
import com.tivanstudio.servera.presentation.auth.viewmodel.ChangePasswordUiState
import com.tivanstudio.servera.presentation.auth.viewmodel.ChangePasswordViewModel
import com.tivanstudio.servera.presentation.theme.PrimaryGreen
import com.tivanstudio.servera.presentation.theme.ServeraTheme

@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val changedMessage = stringResource(R.string.change_password_success)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChangePasswordEvent.PasswordChanged -> {
                    Toast.makeText(context, changedMessage, Toast.LENGTH_SHORT).show()
                    onBack()
                }
            }
        }
    }

    ChangePasswordContent(
        uiState = uiState,
        onOldPasswordChange = viewModel::onOldPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onToggleVisibility = viewModel::onToggleVisibility,
        onSubmit = viewModel::submit,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordContent(
    uiState: ChangePasswordUiState,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.change_password_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            PasswordField(
                value = uiState.oldPassword,
                onValueChange = onOldPasswordChange,
                label = stringResource(R.string.change_password_old_hint),
                isVisible = uiState.isPasswordVisible,
                onToggleVisibility = onToggleVisibility
            )

            Spacer(Modifier.height(12.dp))

            PasswordField(
                value = uiState.newPassword,
                onValueChange = onNewPasswordChange,
                label = stringResource(R.string.change_password_new_hint),
                isVisible = uiState.isPasswordVisible
            )

            Spacer(Modifier.height(8.dp))

            if (uiState.newPassword.isNotEmpty()) {
                PasswordStrengthMeter(strength = uiState.strength)
                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.crack_prefix) + " " +
                        stringResource(uiState.crackTimeRes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            PasswordField(
                value = uiState.confirm,
                onValueChange = onConfirmChange,
                label = stringResource(R.string.change_password_confirm_hint),
                isVisible = uiState.isPasswordVisible,
                isError = uiState.error != null
            )

            if (uiState.error != null) {
                Text(
                    text = stringResource(uiState.error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isLoading && uiState.canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        stringResource(R.string.change_password_button),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onToggleVisibility: (() -> Unit)? = null,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isVisible) VisualTransformation.None
                               else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = onToggleVisibility?.let {
            {
                IconButton(onClick = it) {
                    Icon(
                        if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor      = PrimaryGreen,
            unfocusedBorderColor    = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth(),
        isError = isError
    )
}

@Preview(showBackground = true)
@Composable
private fun ChangePasswordContentPreview() {
    ServeraTheme {
        ChangePasswordContent(
            uiState = ChangePasswordUiState(),
            onOldPasswordChange = {},
            onNewPasswordChange = {},
            onConfirmChange = {},
            onToggleVisibility = {},
            onSubmit = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChangePasswordContentErrorPreview() {
    ServeraTheme {
        ChangePasswordContent(
            uiState = ChangePasswordUiState(
                oldPassword = "old",
                newPassword = "newpass12",
                confirm = "newpass12",
                strength = PasswordStrength.MEDIUM,
                crackTimeRes = R.string.crack_days,
                error = R.string.error_wrong_password
            ),
            onOldPasswordChange = {},
            onNewPasswordChange = {},
            onConfirmChange = {},
            onToggleVisibility = {},
            onSubmit = {},
            onBack = {}
        )
    }
}
