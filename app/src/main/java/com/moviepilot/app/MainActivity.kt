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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.moviepilot.app.ui.AppRoot
import com.moviepilot.app.ui.screens.UpdateDialog
import com.moviepilot.app.ui.theme.MoviePilotTheme
import com.moviepilot.app.ui.theme.MpBackground
import com.moviepilot.app.ui.viewmodel.AuthViewModel
import com.moviepilot.app.ui.viewmodel.UpdateViewModel
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

                // 更新通道是全局的：登录页也要能提示，所以挂在 Activity 这一层，
                // 和切换面板里拿到的 hiltViewModel() 是同一个实例。
                val updateViewModel: UpdateViewModel = hiltViewModel()
                val updateState by updateViewModel.ui.collectAsState()

                LaunchedEffect(state.initializing) {
                    if (!state.initializing) keepSplash = false
                }

                // 冷启动静默检查一次：真有新版本才弹窗
                LaunchedEffect(Unit) { updateViewModel.checkOnLaunch() }

                // 从「安装未知应用」设置页返回时刷新授权状态
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            updateViewModel.refreshPermissionState()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MpBackground)
                ) {
                    if (!state.initializing) {
                        AppRoot(state = state, viewModel = viewModel)
                    }

                    UpdateDialog(
                        state = updateState,
                        onDownload = updateViewModel::download,
                        onInstall = updateViewModel::install,
                        onOpenSettings = {
                            updateViewModel.openPermissionSettings()
                        },
                        onRetry = updateViewModel::checkManually,
                        onSkip = updateViewModel::skipThisVersion,
                        onDismiss = updateViewModel::dismiss
                    )
                }
            }
        }
    }
}
