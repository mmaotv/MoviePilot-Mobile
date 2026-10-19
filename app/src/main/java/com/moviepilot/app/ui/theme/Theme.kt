package com.moviepilot.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 配色与服务器网页版（V3）对齐。
 *
 * 浅色取自网页版 light 主题：
 *   background #F4F5FA / on-surface #3A3541 / primary #8D51F9
 *   grey-100 #F0F2F8 / bordered-surface #FFFFFF / grey-300 #E0E0E0
 *   error #FF4C51 / success #56CA00 / secondary #8A8D93
 *
 * 深色取自网页版 dark 主题：background #0E1116 / primary #8D51F9。
 */
@Immutable
data class MpPalette(
    val dark: Boolean,
    val background: Color,
    /** 背景渐变的第二个色，仅用于登录页 */
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val primary: Color,
    /** primary 的淡色底，用于选中态/图标底 */
    val primarySoft: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val danger: Color,
    val dangerSoft: Color,
    val success: Color,
    val scrim: Color
)

val MpLightPalette = MpPalette(
    dark = false,
    background = Color(0xFFF4F5FA),
    backgroundAlt = Color(0xFFEDE9FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F2F8),
    outline = Color(0xFFE3E5EE),
    primary = Color(0xFF8D51F9),
    primarySoft = Color(0xFFF2EBFE),
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF3A3541),
    textSecondary = Color(0xFF8A8D93),
    danger = Color(0xFFFF4C51),
    dangerSoft = Color(0xFFFFEFF0),
    success = Color(0xFF56CA00),
    scrim = Color(0x43000000)
)

val MpDarkPalette = MpPalette(
    dark = true,
    background = Color(0xFF0E1116),
    backgroundAlt = Color(0xFF161129),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF21262D),
    outline = Color(0xFF30363D),
    primary = Color(0xFF8D51F9),
    primarySoft = Color(0xFF241C3A),
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFFE6EDF3),
    textSecondary = Color(0xFF8B949E),
    danger = Color(0xFFFF6B6E),
    dangerSoft = Color(0xFF2D1416),
    success = Color(0xFF3FB950),
    scrim = Color(0x99000000)
)

val LocalMpPalette = staticCompositionLocalOf { MpLightPalette }

@Composable
fun MoviePilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) MpDarkPalette else MpLightPalette
    CompositionLocalProvider(LocalMpPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = Typography,
            content = content
        )
    }
}

private fun MpPalette.toColorScheme() = if (dark) {
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primarySoft,
        onPrimaryContainer = textPrimary,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = textSecondary,
        outline = outline,
        error = danger,
        onError = Color.White
    )
} else {
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primarySoft,
        onPrimaryContainer = textPrimary,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = textSecondary,
        outline = outline,
        error = danger,
        onError = Color.White
    )
}

// ── 取色入口：跟随系统深浅色自动切换 ─────────────────────────────────

val MpBackground: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.background

val MpBackgroundAlt: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.backgroundAlt

val MpSurface: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.surface

val MpSurfaceVariant: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.surfaceVariant

val MpOutline: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.outline

val MpPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.primary

val MpPrimarySoft: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.primarySoft

val MpOnPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.onPrimary

val MpTextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.textPrimary

val MpTextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.textSecondary

val MpDanger: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.danger

val MpDangerSoft: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.dangerSoft

val MpSuccess: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.success

val MpScrim: Color
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.scrim

/** 当前是否深色，用于阴影/透明度的差异化处理 */
val MpIsDark: Boolean
    @Composable @ReadOnlyComposable get() = LocalMpPalette.current.dark
