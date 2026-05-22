package com.tivanstudio.servera

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.tivanstudio.servera.data.preferences.ThemePreferences
import com.tivanstudio.servera.presentation.navigation.AppNavGraph
import com.tivanstudio.servera.presentation.theme.ServeraTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by themePreferences.isDarkTheme.collectAsState()
            ServeraTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
