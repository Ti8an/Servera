package com.tivanstudio.servera.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tivanstudio.servera.domain.analytics.Analytics
import com.tivanstudio.servera.domain.analytics.AnalyticsEvent
import com.tivanstudio.servera.presentation.auth.ui.ChangePasswordScreen
import com.tivanstudio.servera.presentation.auth.ui.CreatePasswordScreen
import com.tivanstudio.servera.presentation.auth.ui.LoginScreen
import com.tivanstudio.servera.presentation.console.ui.ConsoleScreen
import com.tivanstudio.servera.presentation.console.execute.ui.ExecuteCommandScreen
import com.tivanstudio.servera.presentation.console.result.ui.CommandResultScreen
import com.tivanstudio.servera.presentation.network.info.NetworkInfoScreen
import com.tivanstudio.servera.presentation.history.ui.HistoryScreen
import com.tivanstudio.servera.presentation.presets.groups.PresetGroupsScreen
import com.tivanstudio.servera.presentation.presets.ui.PresetsScreen
import com.tivanstudio.servera.presentation.servers.add.ui.AddServerScreen
import com.tivanstudio.servera.presentation.servers.list.ui.ServerListScreen
import com.tivanstudio.servera.presentation.settings.ui.SettingsScreen
import com.tivanstudio.servera.presentation.tools.network.NetworkScanScreen

@Composable
fun AppNavGraph(navController: NavHostController, analytics: Analytics) {
    TrackScreenViews(navController = navController, analytics = analytics)

    NavHost(navController = navController, startDestination = Screen.Login.route) {

        composable(Screen.Login.route) {
            LoginScreen(
                onAuthenticated = {
                    navController.navigate(Screen.ServerList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToCreatePassword = {
                    navController.navigate(Screen.CreatePassword.route)
                }
            )
        }

        composable(Screen.CreatePassword.route) {
            CreatePasswordScreen(
                onPasswordCreated = {
                    navController.navigate(Screen.ServerList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ServerList.route) {
            ServerListScreen(
                onNavigateToAdd     = { navController.navigate(Screen.AddServer.createRoute()) },
                onNavigateToEdit    = { id -> navController.navigate(Screen.AddServer.createRoute(id)) },
                onNavigateToConsole = { id -> navController.navigate(Screen.Console.createRoute(id)) },
                onNavigateToPresets  = { navController.navigate(Screen.Presets.route) { launchSingleTop = true } },
                onNavigateToHistory  = { navController.navigate(Screen.History.route) { launchSingleTop = true } },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } }
            )
        }

        composable(
            route = Screen.AddServer.route,
            arguments = listOf(navArgument("serverId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            AddServerScreen(
                onSaved = { navController.popBackStack() },
                onBack  = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Console.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) {
            ConsoleScreen(
                onNavigateToExecute = { id -> navController.navigate(Screen.Execute.createRoute(id)) },
                onNavigateToResult  = { navController.navigate(Screen.Result.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Execute.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) {
            ExecuteCommandScreen(
                onResult = { navController.navigate(Screen.Result.route) },
                onBack   = { navController.popBackStack() }
            )
        }

        composable(Screen.Result.route) {
            CommandResultScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateToServers  = { navController.navigate(Screen.ServerList.route) { launchSingleTop = true } },
                onNavigateToPresets  = { navController.navigate(Screen.Presets.route) { launchSingleTop = true } },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                onNavigateToResult   = { navController.navigate(Screen.Result.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToServers  = { navController.navigate(Screen.ServerList.route) { launchSingleTop = true } },
                onNavigateToPresets  = { navController.navigate(Screen.Presets.route) { launchSingleTop = true } },
                onNavigateToHistory  = { navController.navigate(Screen.History.route) { launchSingleTop = true } },
                onNavigateToNetworkScan = { navController.navigate(Screen.NetworkScan.route) },
                onNavigateToNetworkInfo = { navController.navigate(Screen.NetworkInfo.route) },
                onNavigateToChangePassword = { navController.navigate(Screen.ChangePassword.route) }
            )
        }

        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NetworkScan.route) {
            NetworkScanScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NetworkInfo.route) {
            NetworkInfoScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PresetGroups.route) {
            PresetGroupsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Presets.route) {
            PresetsScreen(
                onNavigateToServers  = { navController.navigate(Screen.ServerList.route) { launchSingleTop = true } },
                onNavigateToHistory  = { navController.navigate(Screen.History.route) { launchSingleTop = true } },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                onNavigateToGroups   = { navController.navigate(Screen.PresetGroups.route) { launchSingleTop = true } }
            )
        }
    }
}

/**
 * Firebase reports screen_view on its own only for Activities, and this app is one Activity with
 * a Compose graph -- so the graph has to say when a route opens.
 *
 * [NavDestination.route] is the *pattern* a destination was registered with, never the filled-in
 * route, so "servers/{serverId}/console" is what arrives here and no server id can leak. The
 * allowlist below is belt and braces on top of that: an unrecognised route is dropped rather than
 * reported, so a future destination cannot start shipping strings by being forgotten here.
 */
@Composable
private fun TrackScreenViews(navController: NavHostController, analytics: Analytics) {
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route ?: return@collect
            if (route in TRACKED_ROUTES) analytics.log(AnalyticsEvent.ScreenView(route))
        }
    }
}

private val TRACKED_ROUTES: Set<String> = setOf(
    Screen.Login.route,
    Screen.CreatePassword.route,
    Screen.ChangePassword.route,
    Screen.ServerList.route,
    Screen.AddServer.route,
    Screen.Console.route,
    Screen.Execute.route,
    Screen.Result.route,
    Screen.History.route,
    Screen.Settings.route,
    Screen.Presets.route,
    Screen.PresetGroups.route,
    Screen.NetworkScan.route,
    Screen.NetworkInfo.route
)
