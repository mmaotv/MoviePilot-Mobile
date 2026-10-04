package com.moviepilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.moviepilot.app.navigation.AppNavigation
import com.moviepilot.app.ui.theme.MoviePilotTheme
import com.moviepilot.app.ui.theme.ThemeManager
import com.moviepilot.app.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. 安装 SplashScreen（必须在 super.onCreate 之前）
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2. 让 Splash 保持显示直到登录状态恢复完毕
        //    通过 setKeepOnScreenCondition 返回 true 时继续保持显示
        //    AuthViewModel 初始化完成后返回 false，Splash 自动消失
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        setContent {
            val currentTheme by themeManager.currentTheme

            MoviePilotTheme(appTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val currentUser by authViewModel.currentUser.collectAsState()
                    val isInitializing by authViewModel.isInitializing.collectAsState()

                    // 初始化完成 → 释放 Splash
                    LaunchedEffect(isInitializing) {
                        if (!isInitializing) {
                            keepSplash = false
                        }
                    }

                    if (isInitializing) {
                        // Splash 仍显示中，Compose 层渲染透明占位即可
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        AppNavigation(
                            isLoggedIn = currentUser != null,
                            onLogout = {
                                authViewModel.logout()
                            },
                            authViewModel = authViewModel,
                            themeManager = themeManager
                        )
                    }
                }
            }
        }
    }
}


