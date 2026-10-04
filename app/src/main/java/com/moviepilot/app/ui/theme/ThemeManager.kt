package com.moviepilot.app.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.moviepilot.app.data.local.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme {
    DARK, LIGHT, SYSTEM
}

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    val currentTheme = mutableStateOf(AppTheme.LIGHT)  // 默认浅色

    init {
        // 从持久化存储加载主题设置
        scope.launch {
            val savedMode = preferencesManager.getThemeMode()
            currentTheme.value = when (savedMode) {
                PreferencesManager.ThemeMode.DARK -> AppTheme.DARK
                PreferencesManager.ThemeMode.LIGHT -> AppTheme.LIGHT
                PreferencesManager.ThemeMode.SYSTEM -> AppTheme.SYSTEM
            }
        }
    }

    fun toggleTheme() {
        val newTheme = when (currentTheme.value) {
            AppTheme.LIGHT -> AppTheme.DARK
            AppTheme.DARK -> AppTheme.SYSTEM
            AppTheme.SYSTEM -> AppTheme.LIGHT
        }
        setTheme(newTheme)
    }

    fun setTheme(theme: AppTheme) {
        currentTheme.value = theme
        scope.launch {
            val mode = when (theme) {
                AppTheme.DARK -> PreferencesManager.ThemeMode.DARK
                AppTheme.LIGHT -> PreferencesManager.ThemeMode.LIGHT
                AppTheme.SYSTEM -> PreferencesManager.ThemeMode.SYSTEM
            }
            preferencesManager.saveThemeMode(mode)
        }
    }
    
    fun isDarkTheme(): Boolean {
        return currentTheme.value == AppTheme.DARK
    }
}
