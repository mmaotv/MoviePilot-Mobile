package com.moviepilot.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.moviepilot.app.ui.AppRoot
import com.moviepilot.app.ui.theme.MoviePilotTheme
import com.moviepilot.app.ui.theme.MpBackground
import com.moviepilot.app.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 只负责「系统栏透明 + 内容铺满」这一件事。
        // 图标的明暗交给各个界面自己设置（登录页跟主题、网页版跟网页顶栏色），
        // 否则两边会互相覆盖，出现深浅色串台。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        setContent {
            val dark = isSystemInDarkTheme()

            MoviePilotTheme(darkTheme = dark) {
                val viewModel: AuthViewModel = hiltViewModel()
                val state by viewModel.ui.collectAsState()

                LaunchedEffect(state.initializing) {
                    if (!state.initializing) keepSplash = false
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MpBackground)
                ) {
                    if (!state.initializing) {
                        AppRoot(state = state, viewModel = viewModel)
                    }
                }
            }
        }
    }
}
