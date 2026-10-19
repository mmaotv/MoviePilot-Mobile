package com.moviepilot.app.ui

import androidx.compose.runtime.Composable
import com.moviepilot.app.ui.screens.LoginScreen
import com.moviepilot.app.ui.screens.PwaScreen
import com.moviepilot.app.ui.viewmodel.AuthViewModel

/**
 * 只有两个界面：登录页 / 网页版。
 * 没有转场动画，登录成功后直接切到 WebView。
 */
@Composable
fun AppRoot(
    state: AuthViewModel.UiState,
    viewModel: AuthViewModel
) {
    if (state.loggedIn) {
        PwaScreen(state = state, viewModel = viewModel)
    } else {
        LoginScreen(state = state, viewModel = viewModel)
    }
}
