package com.tivanstudio.servera.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

private val DarkColors = darkColorScheme(
    primary            = PrimaryGreen,
    onPrimary          = Background,
    secondary          = InfoBlue,
    onSecondary        = TextPrimary,
    error              = DangerRed,
    background         = Background,
    onBackground       = TextPrimary,
    surface            = Surface,
    onSurface          = TextPrimary,
    surfaceVariant     = Elevated,
    onSurfaceVariant   = TextSecondary,
    outline            = Elevated,
    primaryContainer   = Surface,
    onPrimaryContainer = PrimaryGreen
)

private val LightColors = lightColorScheme(
    primary            = PrimaryGreen,
    onPrimary          = Color.White,
    secondary          = InfoBlue,
    onSecondary        = LightTextPrimary,
    error              = DangerRed,
    background         = LightBackground,
    onBackground       = LightTextPrimary,
    surface            = LightSurface,
    onSurface          = LightTextPrimary,
    surfaceVariant     = LightElevated,
    onSurfaceVariant   = LightTextSecondary,
    outline            = LightElevated,
    primaryContainer   = LightSurface,
    onPrimaryContainer = PrimaryGreen
)

@Composable
fun ServeraTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content
    )
}

@Preview(showBackground = true)
@Composable
private fun ServeraThemePreview() {
    ServeraTheme {
        Text("Servera Theme", color = PrimaryGreen)
    }
}
