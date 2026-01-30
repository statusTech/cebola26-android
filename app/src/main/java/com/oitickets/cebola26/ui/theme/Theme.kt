package com.oitickets.cebola26.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CebolaGreenPrimary,
    onPrimary = White,
    primaryContainer = CebolaGreenDark,
    onPrimaryContainer = CebolaGreenContainer,
    secondary = CebolaAmber,
    onSecondary = Black,
    secondaryContainer = CebolaAmberDark,
    background = Black,
    surface = Color(0xFF1C1B1F),
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = CebolaGreenPrimary,
    onPrimary = White,
    primaryContainer = CebolaGreenContainer,
    onPrimaryContainer = CebolaGreenDark,
    secondary = CebolaAmber,
    onSecondary = Black,
    secondaryContainer = CebolaAmberContainer,
    onSecondaryContainer = CebolaAmberDark,
    tertiary = CebolaGreenDark,
    background = BackgroundLight,
    surface = White,
    onBackground = Black,
    onSurface = Black,
    error = ErrorRed
)

@Composable
fun Cebola26Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disponível no Android 12+
    // Mudei para FALSE para forçar a identidade visual da festa
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Configura a cor da Barra de Status (Topo do celular) para combinar com o app
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Define a barra de status como Verde (Primary)
            window.statusBarColor = colorScheme.primary.toArgb()
            // Define os ícones da barra de status como brancos (false para light status bars)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Certifique-se que Typography existe em Type.kt
        content = content
    )
}