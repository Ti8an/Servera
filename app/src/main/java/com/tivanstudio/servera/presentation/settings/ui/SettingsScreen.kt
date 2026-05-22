package com.tivanstudio.servera.presentation.settings.ui

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tivanstudio.servera.R
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
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isBiometricAvailable = remember {
        val mgr = BiometricManager.from(context)
        mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    SettingsContent(
        uiState = uiState,
        isBiometricAvailable = isBiometricAvailable,
        onNavigateToServers = onNavigateToServers,
        onNavigateToHistory = onNavigateToHistory,
        onToggleBiometric = viewModel::toggleBiometric,
        onToggleDarkTheme = viewModel::toggleDarkTheme
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    isBiometricAvailable: Boolean,
    onNavigateToServers: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    Scaffold(
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
                onHistory    = onNavigateToHistory,
                onSettings   = {}
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.section_appearance),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = InfoBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dark_theme_setting), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.dark_theme_description), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Switch(
                        checked = uiState.isDarkTheme,
                        onCheckedChange = onToggleDarkTheme,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = PrimaryGreen
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.section_security),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.biometric_setting), color = MaterialTheme.colorScheme.onSurface,  fontWeight = FontWeight.Medium)
                        Text("Fingerprint / Face ID", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Switch(
                        checked = uiState.isBiometricEnabled && isBiometricAvailable,
                        onCheckedChange = { if (isBiometricAvailable) onToggleBiometric(it) },
                        enabled = isBiometricAvailable,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = PrimaryGreen
                        )
                    )
                }
            }

            if (!isBiometricAvailable) {
                Text(
                    stringResource(R.string.biometric_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.section_about),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Servera", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        Text("${stringResource(R.string.app_version)} ${uiState.appVersion}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.encryption_label), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        Text("AES-256-GCM + Android Keystore", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }
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
            onToggleBiometric = {},
            onToggleDarkTheme = {}
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
            onToggleBiometric = {},
            onToggleDarkTheme = {}
        )
    }
}
