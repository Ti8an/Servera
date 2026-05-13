package com.tivanstudio.servera.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tivanstudio.servera.presentation.auth.CreatePasswordScreen
import com.tivanstudio.servera.presentation.auth.LoginScreen
import com.tivanstudio.servera.presentation.console.ConsoleScreen
import com.tivanstudio.servera.presentation.console.execute.ExecuteCommandScreen
import com.tivanstudio.servera.presentation.console.result.CommandResultScreen
import com.tivanstudio.servera.presentation.history.HistoryScreen
import com.tivanstudio.servera.presentation.servers.add.AddServerScreen
import com.tivanstudio.servera.presentation.servers.list.ServerListScreen
import com.tivanstudio.servera.presentation.settings.SettingsScreen

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
