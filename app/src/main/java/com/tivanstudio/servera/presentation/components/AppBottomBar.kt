package com.tivanstudio.servera.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tivanstudio.servera.R
import com.tivanstudio.servera.presentation.navigation.Screen
import com.tivanstudio.servera.presentation.theme.PrimaryGreen
import com.tivanstudio.servera.presentation.theme.ServeraTheme
import com.tivanstudio.servera.presentation.theme.Surface
import com.tivanstudio.servera.presentation.theme.TextSecondary

@Composable
fun AppBottomBar(
    currentRoute: String,
    onServers: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    NavigationBar(containerColor = Surface) {
        NavigationBarItem(
            selected = currentRoute == Screen.ServerList.route,
            onClick  = onServers,
            icon     = { Icon(Icons.Default.Dns, contentDescription = null) },
            label    = { Text(stringResource(R.string.nav_servers)) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = PrimaryGreen,
                selectedTextColor   = PrimaryGreen,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor      = PrimaryGreen.copy(alpha = 0.15f)
            )
        )
        NavigationBarItem(
            selected = currentRoute == Screen.History.route,
            onClick  = onHistory,
            icon     = { Icon(Icons.Default.History, contentDescription = null) },
            label    = { Text(stringResource(R.string.nav_history)) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = PrimaryGreen,
                selectedTextColor   = PrimaryGreen,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor      = PrimaryGreen.copy(alpha = 0.15f)
            )
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Settings.route,
            onClick  = onSettings,
            icon     = { Icon(Icons.Default.Settings, contentDescription = null) },
            label    = { Text(stringResource(R.string.nav_settings)) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = PrimaryGreen,
                selectedTextColor   = PrimaryGreen,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor      = PrimaryGreen.copy(alpha = 0.15f)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBottomBarServersPreview() {
    ServeraTheme {
        AppBottomBar(
            currentRoute = Screen.ServerList.route,
            onServers = {},
            onHistory = {},
            onSettings = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBottomBarHistoryPreview() {
    ServeraTheme {
        AppBottomBar(
            currentRoute = Screen.History.route,
            onServers = {},
            onHistory = {},
            onSettings = {}
        )
    }
}
