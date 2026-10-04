package com.moviepilot.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Apple HIG 排版层级 ───────────────────────────────────────────────────
// LargeTitle  34sp Bold   (页面主标题，与 iOS UIFont.TextStyle.largeTitle 对齐)
// Title1      28sp Bold   (二级标题)
// Title2      22sp Bold   (三级标题)
// Headline    17sp SemiBold (强调正文)
// Body        17sp Regular (正文)
// Callout     16sp Regular (辅助说明)
// Subheadline 15sp Regular (表单标签)
// Footnote    13sp Regular (底部提示)
// Caption     12sp Regular (最小辅助文字)
val Typography = Typography(
    // LargeTitle → displayLarge
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.5).sp
    ),
    // Title2 → titleLarge
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // Headline → titleMedium
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.24).sp
    ),
    // Body → bodyLarge
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.24).sp
    ),
    // Subheadline → bodyMedium
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    // Footnote → bodySmall
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    // Caption → labelSmall
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)
