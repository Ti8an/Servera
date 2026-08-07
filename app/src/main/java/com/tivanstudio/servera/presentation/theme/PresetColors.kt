package com.tivanstudio.servera.presentation.theme

import androidx.compose.ui.graphics.Color

val PresetColors = listOf(
    "#2E7D32",
    "#1565C0",
    "#00838F",
    "#6A1B9A",
    "#AD1457",
    "#EF6C00",
    "#C62828",
    "#558B2F",
    "#37474F",
    "#4E342E"
)

fun String.toComposeColor() = Color(android.graphics.Color.parseColor(this))
