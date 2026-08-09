package com.tivanstudio.servera.presentation.settings.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
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
    onNavigateToHistory: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToNetworkScan: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isBiometricAvailable = remember {
        val mgr = BiometricManager.from(context)
        mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
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
        onToggleBiometric = viewModel::toggleBiometric,
        onToggleDarkTheme = viewModel::toggleDarkTheme,
        onToggleSaveCommandsAlways = viewModel::toggleSaveCommandsAlways,
        onToggleSaveResultInHistory = viewModel::toggleSaveResultInHistory,
        onNavigateToNetworkScan = onNavigateToNetworkScan,
        onRateApp = onRateApp
    )
}

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
    onNavigateToNetworkScan: () -> Unit,
    onRateApp: () -> Unit
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
            onNavigateToNetworkScan = {},
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
            onNavigateToNetworkScan = {},
            onRateApp = {}
        )
    }
}
