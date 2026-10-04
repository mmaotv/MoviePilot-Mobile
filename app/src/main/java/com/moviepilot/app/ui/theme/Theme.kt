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

// 深色主题：与服务器 PWA v3.1.0 完全一致，
// 保证「启动页 → 登录页 → 侧边栏 → PWA 内容」全程无配色跳变。
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8D51F9),
    secondary = Color(0xFFA78BFA),
    tertiary = Color(0xFF3FB950),
    background = Color(0xFF0E1116),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF21262D),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D)
)

// 浅色主题：PWA 侧栏/弹层在浅色下也使用同一套强调色
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8D51F9),
    secondary = Color(0xFFA78BFA),
    tertiary = Color(0xFF3FB950),
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEBEDF0),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFF0E1116),
    onSurface = Color(0xFF0E1116),
    onSurfaceVariant = Color(0xFF57606A),
    outline = Color(0xFFD0D7DE)
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
