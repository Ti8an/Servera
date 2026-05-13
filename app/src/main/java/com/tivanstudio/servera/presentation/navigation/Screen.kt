package com.tivanstudio.servera.presentation.navigation

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object CreatePassword : Screen("create_password")
    object ServerList     : Screen("servers")
    object AddServer      : Screen("servers/add?serverId={serverId}") {
        fun createRoute(id: Long? = null) =
            if (id != null) "servers/add?serverId=$id" else "servers/add"
    }
    object Console        : Screen("servers/{serverId}/console") {
        fun createRoute(id: Long) = "servers/$id/console"
    }
    object Execute        : Screen("servers/{serverId}/execute") {
        fun createRoute(id: Long) = "servers/$id/execute"
    }
    object Result         : Screen("result")
    object History        : Screen("history")
    object Settings       : Screen("settings")
}
