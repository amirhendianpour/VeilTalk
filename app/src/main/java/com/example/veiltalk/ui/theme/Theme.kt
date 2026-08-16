package com.example.veiltalk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VeilRedLight,
    onPrimary = VeilWhite,
    secondary = VeilMediumGray,
    onSecondary = VeilWhite,
    background = VeilBlack,
    onBackground = VeilWhite,
    surface = VeilDarkGray,
    onSurface = VeilWhite,
    error = Color(0xFFCF6679),
    onError = VeilBlack
)

private val LightColorScheme = lightColorScheme(
    primary = VeilRed,
    onPrimary = VeilWhite,
    secondary = VeilDarkGray,
    onSecondary = VeilWhite,
    background = VeilLightGray,
    onBackground = VeilBlack,
    surface = VeilWhite,
    onSurface = VeilBlack,
    error = Color(0xFFB00020),
    onError = VeilWhite
)

@Composable
fun VeilTalkTheme(
    darkTheme: Boolean? = null,
    // We disable dynamic color to strictly follow the brand guidelines from the logo
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = darkTheme ?: isSystemInDarkTheme()
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
