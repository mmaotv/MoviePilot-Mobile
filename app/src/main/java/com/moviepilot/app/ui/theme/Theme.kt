package com.moviepilot.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6200EA),
    secondary = Color(0xFF9C27B0),
    tertiary = Color(0xFF4ecca3),
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF2D2D2D),
    surfaceVariant = Color(0xFF3D3D3D),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EA),    // 紫色，与 PWA 一致
    secondary = Color(0xFF9C27B0),
    tertiary = Color(0xFF4ecca3),  // 保留绿色点缀
    background = Color(0xFFF8F8FF), // 浅色背景（接近白色）
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDE7F6),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A)
)

@Composable
fun MoviePilotTheme(
    appTheme: AppTheme = AppTheme.LIGHT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    
    val darkTheme = when (appTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> systemDarkTheme
    }
    
    val colorScheme = when {
        dynamicColor -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
