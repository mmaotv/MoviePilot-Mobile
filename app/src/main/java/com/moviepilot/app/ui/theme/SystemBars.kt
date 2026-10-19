package com.moviepilot.app.ui.theme

import android.app.Activity
import androidx.core.view.WindowCompat

/**
 * 统一设置系统栏图标的明暗。
 *
 * @param lightStatusBar 状态栏背景是否为浅色（true = 深色图标）
 * @param lightNavigationBar 导航栏背景是否为浅色（true = 深色图标）
 *
 * 之所以不在 MainActivity 里一次性定死：登录页跟 App 主题走，
 * 网页版跟网页顶栏的实际颜色走。两边各管各的，就不会出现
 * 「深色主题下状态栏还是白的」「浅色主题下状态栏文字是白的」这种串台。
 */
fun setSystemBarIcons(
    activity: Activity,
    lightStatusBar: Boolean,
    lightNavigationBar: Boolean = lightStatusBar
) {
    runCatching {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = lightStatusBar
        controller.isAppearanceLightNavigationBars = lightNavigationBar
    }
}
