package com.teleflow.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Light = lightColorScheme(
    primary = TeleBlue,
    onPrimary = TextOnBlue,
    primaryContainer = TeleBlueWash,
    onPrimaryContainer = TeleBlueDark,
    secondary = TeleBlueLight,
    background = BgLight,
    onBackground = TextPrimary,
    surface = CardLight,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFE4E6EB),
    onSurfaceVariant = TextSecondary,
    outline = DividerLight,
    outlineVariant = DividerLight,
    error = Red,
    onError = TextOnBlue
)

private val Dark = darkColorScheme(
    primary = TeleBlueLight,
    onPrimary = TextOnBlue,
    primaryContainer = TeleBlueDark,
    onPrimaryContainer = TeleBlueWash,
    secondary = TeleBlueLight,
    background = BgDark,
    onBackground = Color(0xFFE4E6EB),
    surface = CardDark,
    onSurface = Color(0xFFE4E6EB),
    surfaceVariant = Color(0xFF2B3945),
    onSurfaceVariant = Color(0xFF8696A7),
    outline = DividerDark,
    outlineVariant = DividerDark,
    error = Red,
    onError = TextOnBlue
)

@Composable
fun TeleFlowTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (dark) Dark else Light
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val w = (view.context as Activity).window
            w.statusBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
