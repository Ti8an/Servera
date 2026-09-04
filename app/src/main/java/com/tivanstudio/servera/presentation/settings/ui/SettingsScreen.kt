package com.tivanstudio.servera.presentation.settings.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
import com.tivanstudio.servera.data.crypto.SecurityLevel
import com.tivanstudio.servera.presentation.components.AppBottomBar
import com.tivanstudio.servera.presentation.navigation.Screen
import com.tivanstudio.servera.presentation.settings.viewmodel.SettingsUiState
import com.tivanstudio.servera.presentation.settings.viewmodel.SettingsViewModel
import com.tivanstudio.servera.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToServers: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToNetworkScan: () -> Unit,
    onNavigateToNetworkInfo: () -> Unit,
    onNavigateToChangePassword: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // STRONG, not WEAK: the BEK is bound to a class-3 biometric, so a face sensor the platform
    // ranks as weak could never release it and the switch would fail on the prompt.
    val isBiometricAvailable = remember {
        val mgr = BiometricManager.from(context)
        mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    val enrollTitle    = stringResource(R.string.biometric_enroll_title)
    val enrollSubtitle = stringResource(R.string.biometric_enroll_subtitle)
    val enrollCancel   = stringResource(R.string.biometric_cancel)

    LaunchedEffect(uiState.showBiometricPrompt) {
        val cipher = uiState.pendingBiometricCipher
        if (!uiState.showBiometricPrompt || cipher == null) return@LaunchedEffect

        val activity = context as? FragmentActivity
        if (activity == null) {
            viewModel.onBiometricEnrollError()
            return@LaunchedEffect
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                // The cipher out of the result is the one the hardware unlocked; the one we
                // handed in is still gated and would throw on doFinal.
                val authenticated = result.cryptoObject?.cipher
                if (authenticated != null) {
                    viewModel.onBiometricEnrollSuccess(authenticated)
                } else {
                    viewModel.onBiometricEnrollError()
                }
            }

            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                viewModel.onBiometricEnrollError()
            }
        }

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(enrollTitle)
            .setSubtitle(enrollSubtitle)
            .setNegativeButtonText(enrollCancel)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        BiometricPrompt(activity, ContextCompat.getMainExecutor(context), callback)
            .authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    val rateAppError = stringResource(R.string.rate_app_error)

    val onRateApp: () -> Unit = {
        val pkg = context.packageName.removeSuffix(".debug")
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, rateAppError, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SettingsContent(
        uiState = uiState,
        isBiometricAvailable = isBiometricAvailable,
        onNavigateToServers = onNavigateToServers,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToPresets = onNavigateToPresets,
        onToggleBiometric = viewModel::onBiometricToggle,
        onToggleDarkTheme = viewModel::toggleDarkTheme,
        onToggleSaveCommandsAlways = viewModel::toggleSaveCommandsAlways,
        onToggleSaveResultInHistory = viewModel::toggleSaveResultInHistory,
        onToggleEnhanced = viewModel::onToggleEnhanced,
        onPickLevel = viewModel::onPickLevel,
        onConfirmDisableEnhanced = viewModel::onConfirmDisableEnhanced,
        onSubmitLevelPassword = viewModel::onSubmitLevelPassword,
        onDismissLevelChange = viewModel::onDismissLevelChange,
        onMessageShown = viewModel::onMessageShown,
        onNavigateToNetworkScan = onNavigateToNetworkScan,
        onNavigateToNetworkInfo = onNavigateToNetworkInfo,
        onNavigateToChangePassword = onNavigateToChangePassword,
        onRateApp = onRateApp
    )
}

/** The label for a level, shared by the picker and the caption under the switch. */
@Composable
private fun levelLabel(level: SecurityLevel): String = stringResource(
    when (level) {
        SecurityLevel.MIN -> R.string.level_min
        SecurityLevel.MID -> R.string.level_mid
        SecurityLevel.HIGH -> R.string.level_high
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    isBiometricAvailable: Boolean,
    onNavigateToServers: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleSaveCommandsAlways: (Boolean) -> Unit,
    onToggleSaveResultInHistory: (Boolean) -> Unit,
    onToggleEnhanced: (Boolean) -> Unit,
    onPickLevel: (SecurityLevel) -> Unit,
    onConfirmDisableEnhanced: () -> Unit,
    onSubmitLevelPassword: (String) -> Unit,
    onDismissLevelChange: () -> Unit,
    onMessageShown: () -> Unit,
    onNavigateToNetworkScan: () -> Unit,
    onNavigateToNetworkInfo: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onRateApp: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = uiState.messageRes?.let { stringResource(it) }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    if (uiState.showLevelPicker) {
        LevelPickerDialog(
            currentLevel = uiState.currentLevel,
            onApply = onPickLevel,
            onDismiss = onDismissLevelChange
        )
    }

    if (uiState.showDisableConfirm) {
        AlertDialog(
            onDismissRequest = onDismissLevelChange,
            title = { Text(stringResource(R.string.enhanced_title)) },
            text = { Text(stringResource(R.string.enhanced_off_msg)) },
            confirmButton = {
                TextButton(onClick = onConfirmDisableEnhanced) {
                    Text(stringResource(R.string.level_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissLevelChange) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.pendingLevel != null) {
        LevelPasswordDialog(
            isWorking = uiState.isChangingLevel,
            isError = uiState.levelPasswordError,
            onSubmit = onSubmitLevelPassword,
            onDismiss = onDismissLevelChange
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = Screen.Settings.route,
                onServers    = onNavigateToServers,
                onPresets    = onNavigateToPresets,
                onHistory    = onNavigateToHistory,
                onSettings   = {}
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.section_appearance))

            SettingSwitchCard(
                icon = Icons.Default.DarkMode,
                title = stringResource(R.string.dark_theme_setting),
                subtitle = stringResource(R.string.dark_theme_description),
                checked = uiState.isDarkTheme,
                onCheckedChange = onToggleDarkTheme
            )

            Spacer(Modifier.height(8.dp))

            SectionTitle(stringResource(R.string.section_security))

            SettingSwitchCard(
                icon = Icons.Default.Fingerprint,
                title = stringResource(R.string.biometric_setting),
                subtitle = "Fingerprint / Face ID",
                checked = uiState.isBiometricEnabled && isBiometricAvailable,
                onCheckedChange = { if (isBiometricAvailable) onToggleBiometric(it) },
                enabled = isBiometricAvailable
            )

            if (!isBiometricAvailable) {
                Text(
                    stringResource(R.string.biometric_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            SettingSwitchCard(
                icon = Icons.Default.Shield,
                title = stringResource(R.string.enhanced_title),
                subtitle = stringResource(R.string.enhanced_desc),
                checked = uiState.enhancedEnabled,
                onCheckedChange = onToggleEnhanced
            )

            if (uiState.enhancedEnabled) {
                Text(
                    levelLabel(uiState.currentLevel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            ActionCard(
                icon = Icons.Default.Key,
                title = stringResource(R.string.change_password_title),
                subtitle = stringResource(R.string.change_password_subtitle),
                onClick = onNavigateToChangePassword
            )

            Spacer(Modifier.height(8.dp))

            SectionTitle(stringResource(R.string.section_commands))

            SettingSwitchCard(
                icon = Icons.Default.Save,
                title = stringResource(R.string.save_commands_always_setting),
                subtitle = stringResource(R.string.save_commands_always_description),
                checked = uiState.isSaveCommandsAlways,
                onCheckedChange = onToggleSaveCommandsAlways
            )

            SettingSwitchCard(
                icon = Icons.Default.Article,
                title = stringResource(R.string.save_result_setting),
                subtitle = stringResource(R.string.save_result_description),
                checked = uiState.isSaveResultInHistory,
                onCheckedChange = onToggleSaveResultInHistory
            )

            Spacer(Modifier.height(8.dp))

            SectionTitle(stringResource(R.string.section_tools))

            ActionCard(
                icon = Icons.Default.Wifi,
                title = stringResource(R.string.net_scan_title),
                subtitle = stringResource(R.string.net_scan_subtitle),
                onClick = onNavigateToNetworkScan
            )

            ActionCard(
                icon = Icons.Default.Router,
                title = stringResource(R.string.network_info_title),
                subtitle = stringResource(R.string.network_info_subtitle),
                onClick = onNavigateToNetworkInfo
            )

            Spacer(Modifier.height(8.dp))

            SectionTitle(stringResource(R.string.section_about))

            InfoCard(
                icon = Icons.Default.Info,
                title = "Servera",
                subtitle = "${stringResource(R.string.app_version)} ${uiState.appVersion}"
            )

            InfoCard(
                icon = Icons.Default.Security,
                title = stringResource(R.string.encryption_label),
                subtitle = "AES-256-GCM + Android Keystore"
            )

            Spacer(Modifier.height(8.dp))

            SectionTitle(stringResource(R.string.section_support))

            ActionCard(
                icon = Icons.Default.StarRate,
                title = stringResource(R.string.rate_app_title),
                subtitle = stringResource(R.string.rate_app_subtitle),
                onClick = onRateApp
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Three radio rows plus Apply; picking here only moves on to the password step. */
@Composable
private fun LevelPickerDialog(
    currentLevel: SecurityLevel,
    onApply: (SecurityLevel) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember(currentLevel) { mutableStateOf(currentLevel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.level_pick_title)) },
        text = {
            Column {
                SecurityLevel.entries.forEach { level ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = level }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = level == selected, onClick = { selected = level })
                        Spacer(Modifier.width(8.dp))
                        Text(levelLabel(level), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selected) }) {
                Text(stringResource(R.string.level_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * The re-wrap needs the password to unwrap the DEK first. It is a full PBKDF2 derivation each
 * way, so the dialog locks down and shows a spinner while it runs.
 */
@Composable
private fun LevelPasswordDialog(
    isWorking: Boolean,
    isError: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = { Text(stringResource(R.string.level_pick_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.level_password_prompt)) },
                    singleLine = true,
                    enabled = !isWorking,
                    isError = isError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        stringResource(R.string.level_wrong_password),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
                if (isWorking) {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(password) },
                enabled = !isWorking && password.isNotEmpty()
            ) {
                Text(stringResource(R.string.level_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun SettingCard(
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun SettingSwitchCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    SettingCard {
        Icon(icon, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = PrimaryGreen
            )
        )
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    SettingCard {
        Icon(icon, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    SettingCard(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentDarkPreview() {
    ServeraTheme(darkTheme = true) {
        SettingsContent(
            uiState = SettingsUiState(isBiometricEnabled = false, appVersion = "1.1.2-3", isDarkTheme = true),
            isBiometricAvailable = true,
            onNavigateToServers = {},
            onNavigateToHistory = {},
            onNavigateToPresets = {},
            onToggleBiometric = {},
            onToggleDarkTheme = {},
            onToggleSaveCommandsAlways = {},
            onToggleSaveResultInHistory = {},
            onToggleEnhanced = {},
            onPickLevel = {},
            onConfirmDisableEnhanced = {},
            onSubmitLevelPassword = {},
            onDismissLevelChange = {},
            onMessageShown = {},
            onNavigateToNetworkScan = {},
            onNavigateToNetworkInfo = {},
            onNavigateToChangePassword = {},
            onRateApp = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentLightPreview() {
    ServeraTheme(darkTheme = false) {
        SettingsContent(
            uiState = SettingsUiState(isBiometricEnabled = false, appVersion = "1.1.2-3", isDarkTheme = false),
            isBiometricAvailable = true,
            onNavigateToServers = {},
            onNavigateToHistory = {},
            onNavigateToPresets = {},
            onToggleBiometric = {},
            onToggleDarkTheme = {},
            onToggleSaveCommandsAlways = {},
            onToggleSaveResultInHistory = {},
            onToggleEnhanced = {},
            onPickLevel = {},
            onConfirmDisableEnhanced = {},
            onSubmitLevelPassword = {},
            onDismissLevelChange = {},
            onMessageShown = {},
            onNavigateToNetworkScan = {},
            onNavigateToNetworkInfo = {},
            onNavigateToChangePassword = {},
            onRateApp = {}
        )
    }
}
