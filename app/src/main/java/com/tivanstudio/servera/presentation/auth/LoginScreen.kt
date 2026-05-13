package com.tivanstudio.servera.presentation.auth

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.presentation.theme.Elevated
import com.tivanstudio.servera.presentation.theme.PrimaryGreen
import com.tivanstudio.servera.presentation.theme.Surface
import com.tivanstudio.servera.presentation.theme.TextSecondary

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onAuthenticated: () -> Unit,
    onNavigateToCreatePassword: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToServers        -> onAuthenticated()
                is LoginEvent.NavigateToCreatePassword -> onNavigateToCreatePassword()
                is LoginEvent.ShowError                -> Toast.makeText(context, event.msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(72.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Remote Server Control",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.login() }),
            trailingIcon = {
                IconButton(onClick = viewModel::onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (uiState.isPasswordVisible) Icons.Default.VisibilityOff
                                      else Icons.Default.Visibility,
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
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.error != null
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 4.dp, top = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = viewModel::login,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Войти", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }

        if (uiState.isBiometricEnabled) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = Surface
            )
            Text("или", color = TextSecondary, fontSize = 12.sp)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = Surface
            )
            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = {
                    val activity = context as? FragmentActivity ?: return@OutlinedButton
                    val executor = ContextCompat.getMainExecutor(context)
                    val callback = object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            viewModel.onBiometricSuccess()
                        }
                        override fun onAuthenticationError(code: Int, msg: CharSequence) {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    val prompt = BiometricPrompt(activity, executor, callback)
                    val info = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Аутентификация")
                        .setSubtitle("Войдите с помощью биометрии")
                        .setNegativeButtonText("Отмена")
                        .build()
                    prompt.authenticate(info)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = PrimaryGreen)
                Spacer(Modifier.width(8.dp))
                Text("Войти по биометрии", color = PrimaryGreen)
            }
        }

        Spacer(Modifier.height(24.dp))

        TextButton(onClick = viewModel::navigateToCreatePassword) {
            Text(
                text = "Первый запуск? Создать пароль",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
