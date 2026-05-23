package com.tivanstudio.servera.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tivanstudio.servera.presentation.auth.ui.CreatePasswordScreen
import com.tivanstudio.servera.presentation.auth.ui.LoginScreen
import com.tivanstudio.servera.presentation.console.ui.ConsoleScreen
import com.tivanstudio.servera.presentation.console.execute.ui.ExecuteCommandScreen
import com.tivanstudio.servera.presentation.console.result.ui.CommandResultScreen
import com.tivanstudio.servera.presentation.history.ui.HistoryScreen
import com.tivanstudio.servera.presentation.servers.add.ui.AddServerScreen
import com.tivanstudio.servera.presentation.servers.list.ui.ServerListScreen
import com.tivanstudio.servera.presentation.settings.ui.SettingsScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
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
                onBack   = { navController.popBackStack() },
                onRepeat = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateToServers  = { navController.navigate(Screen.ServerList.route) { launchSingleTop = true } },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToServers  = { navController.navigate(Screen.ServerList.route) { launchSingleTop = true } },
                onNavigateToHistory  = { navController.navigate(Screen.History.route) { launchSingleTop = true } }
            )
        }
    }
}
