package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviepilot.app.BuildConfig
import com.moviepilot.app.R
import com.moviepilot.app.data.model.AccountProfile
import com.moviepilot.app.data.model.ServerProfile
import com.moviepilot.app.data.model.displayServerUrl
import com.moviepilot.app.ui.theme.MpBackground
import com.moviepilot.app.ui.theme.MpBackgroundAlt
import com.moviepilot.app.ui.theme.MpDanger
import com.moviepilot.app.ui.theme.MpDangerSoft
import com.moviepilot.app.ui.theme.MpIsDark
import com.moviepilot.app.ui.theme.MpOutline
import com.moviepilot.app.ui.theme.MpPrimary
import com.moviepilot.app.ui.theme.MpPrimarySoft
import com.moviepilot.app.ui.theme.MpSurface
import com.moviepilot.app.ui.theme.MpSurfaceVariant
import com.moviepilot.app.ui.theme.MpTextPrimary
import com.moviepilot.app.ui.theme.MpTextSecondary
import com.moviepilot.app.ui.theme.setSystemBarIcons
import com.moviepilot.app.ui.viewmodel.AuthViewModel

/**
 * 原生只负责「连哪台服务器、用哪个账号」。
 *
 * 配色跟随系统深浅色：浅色对齐网页版 V3 的 light 主题（#F4F5FA / #8D51F9），
 * 深色对齐 dark 主题（#0E1116 / #8D51F9）。
 * 无入场动画、无呼吸动画、无展开动画。
 */
@Composable
fun LoginScreen(
    state: AuthViewModel.UiState,
    viewModel: AuthViewModel
) {
    val focusManager = LocalFocusManager.current
    val dark = MpIsDark

    // 登录页没有网页参与，状态栏图标直接跟 App 主题走。
    // 这一步必须有：从网页版退出回来时，状态栏图标还留着网页那边的设置。
    val activity = LocalContext.current as? android.app.Activity
    LaunchedEffect(dark) {
        activity?.let { setSystemBarIcons(it, lightStatusBar = !dark) }
    }

    var serverInput by remember { mutableStateOf(state.serverInput) }
    var username by remember { mutableStateOf(state.initialUsername) }
    var password by remember { mutableStateOf(state.initialPassword) }
    var rememberPassword by remember { mutableStateOf(state.rememberPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showServers by remember { mutableStateOf(false) }

    // 切换服务器/账号后由 ViewModel 递增 revision，这里重置输入框
    LaunchedEffect(state.revision) {
        serverInput = state.serverInput
        username = state.initialUsername
        password = state.initialPassword
        rememberPassword = state.rememberPassword
        showServers = false
    }

    val loading = state.loginState is AuthViewModel.LoginState.Loading
    val canLogin = !loading &&
        serverInput.isNotBlank() &&
        username.isNotBlank() &&
        password.isNotBlank()

    fun submit() {
        if (!canLogin) return
        focusManager.clearFocus()
        viewModel.login(serverInput, username, password, rememberPassword)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to MpBackground,
                    0.55f to MpBackground,
                    1f to MpBackgroundAlt
                )
            )
    ) {
        // 顶部柔光，让浅色背景不至于太平
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MpPrimary.copy(alpha = if (dark) 0.16f else 0.14f),
                            Color.Transparent
                        ),
                        radius = 780f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(34.dp))

            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "MoviePilot",
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = "MoviePilot",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MpTextPrimary,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "连接你的影视库",
                fontSize = 13.sp,
                color = MpTextSecondary,
                letterSpacing = 0.3.sp
            )

            Spacer(Modifier.height(26.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (dark) 0.dp else 14.dp,
                        shape = RoundedCornerShape(18.dp),
                        clip = false,
                        ambientColor = MpPrimary.copy(alpha = 0.18f),
                        spotColor = MpPrimary.copy(alpha = 0.16f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(MpSurface)
                    .border(1.dp, MpOutline.copy(alpha = if (dark) 1f else 0.9f), RoundedCornerShape(18.dp))
            ) {
                // ── 服务器 ───────────────────────────────────────
                ServerFieldRow(
                    value = serverInput,
                    onValueChange = {
                        serverInput = it
                        viewModel.setServerInput(it)
                    },
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    showDropDown = state.servers.isNotEmpty() || showServers,
                    expanded = showServers,
                    onToggleList = { showServers = !showServers }
                )

                if (showServers) {
                    HorizontalDivider(color = MpOutline.copy(alpha = 0.8f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 208.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        state.servers.forEach { server ->
                            ServerRow(
                                server = server,
                                active = server.serverUrl == state.serverUrl,
                                onClick = {
                                    serverInput = displayServerUrl(server.serverUrl)
                                    viewModel.selectServer(server)
                                    showServers = false
                                },
                                onDelete = { viewModel.deleteServer(server.id) }
                            )
                        }
                    }
                    Text(
                        text = "地址自己填就行：IP、域名、http 或 https、带子路径都可以",
                        fontSize = 11.sp,
                        color = MpTextSecondary,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp, top = 2.dp)
                    )
                }

                HorizontalDivider(color = MpOutline.copy(alpha = 0.8f))

                // ── 账号快选 ─────────────────────────────────────
                if (state.accounts.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        Text(
                            text = "此服务器的账号",
                            fontSize = 11.sp,
                            color = MpTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(9.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            state.accounts.forEach { account ->
                                AccountChip(
                                    account = account,
                                    active = account.username.equals(username, ignoreCase = true),
                                    onClick = {
                                        username = account.username
                                        password = viewModel.passwordOf(account)
                                        rememberPassword = account.rememberPassword
                                    }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MpOutline.copy(alpha = 0.8f))
                }

                // ── 用户名 ───────────────────────────────────────
                FieldRow(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "用户名",
                    leadingIcon = Icons.Outlined.Person,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                HorizontalDivider(color = MpOutline.copy(alpha = 0.8f))

                // ── 密码 ─────────────────────────────────────────
                FieldRow(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "密码",
                    leadingIcon = Icons.Outlined.Lock,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailing = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                tint = MpTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                HorizontalDivider(color = MpOutline.copy(alpha = 0.8f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 16.dp, top = 3.dp, bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberPassword,
                        onCheckedChange = { rememberPassword = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MpPrimary,
                            uncheckedColor = MpTextSecondary
                        ),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "记住密码",
                        fontSize = 13.sp,
                        color = if (rememberPassword) MpTextPrimary else MpTextSecondary,
                        modifier = Modifier.clickable { rememberPassword = !rememberPassword }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            LoginButton(
                loading = loading,
                enabled = canLogin,
                onClick = { submit() }
            )

            val error = state.loginState as? AuthViewModel.LoginState.Error
            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MpDangerSoft)
                        .border(1.dp, MpDanger.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error.message,
                        color = MpDanger,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = buildString {
                    append("v").append(BuildConfig.VERSION_NAME)
                    val shown = displayServerUrl(state.serverUrl)
                    if (shown.isNotEmpty()) append(" · ").append(shown)
                },
                fontSize = 11.sp,
                color = MpTextSecondary.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

// ── 组件 ─────────────────────────────────────────────────────────────

@Composable
private fun LoginButton(
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val dark = MpIsDark
    val shape = RoundedCornerShape(14.dp)
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(MpPrimary, Color(0xFFA478FF)))
    } else {
        Brush.horizontalGradient(
            listOf(MpPrimary.copy(alpha = 0.34f), MpPrimary.copy(alpha = 0.34f))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = if (enabled && !dark) 12.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = MpPrimary.copy(alpha = 0.4f),
                spotColor = MpPrimary.copy(alpha = 0.4f)
            )
            .clip(shape)
            .background(brush)
            .clickable(enabled = enabled && !loading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("正在连接", color = Color.White, fontSize = 15.sp)
            }
        } else {
            Text(
                text = "登录",
                color = Color.White.copy(alpha = if (enabled) 1f else 0.75f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun ServerFieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
    showDropDown: Boolean,
    expanded: Boolean,
    onToggleList: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val accent = focused || value.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = null,
            tint = if (accent) MpPrimary else MpTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(color = MpTextPrimary, fontSize = 15.sp),
            singleLine = true,
            interactionSource = interaction,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            cursorBrush = SolidColor(MpPrimary),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "服务器地址，如 192.168.1.10:3000",
                            color = MpTextSecondary,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
            }
        )
        if (showDropDown) {
            IconButton(
                onClick = onToggleList,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.ArrowDropDown,
                    contentDescription = "服务器列表",
                    tint = if (expanded) MpPrimary else MpTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ServerRow(
    server: ServerProfile,
    active: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                fontSize = 14.sp,
                color = if (active) MpPrimary else MpTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = displayServerUrl(server.serverUrl),
                fontSize = 11.sp,
                color = MpTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (active) {
            Text(
                text = "当前",
                fontSize = 11.sp,
                color = MpPrimary,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除服务器",
                tint = MpTextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AccountChip(
    account: AccountProfile,
    active: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) MpPrimarySoft else MpSurfaceVariant)
            .border(
                1.dp,
                if (active) MpPrimary.copy(alpha = 0.45f) else MpOutline,
                RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (active) MpPrimary else MpOutline),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = account.username.take(1).uppercase(),
                fontSize = 10.sp,
                color = if (active) Color.White else MpTextSecondary
            )
        }
        Spacer(Modifier.width(7.dp))
        Text(
            text = account.username,
            fontSize = 13.sp,
            color = if (active) MpPrimary else MpTextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun FieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: @Composable (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val accent = focused || value.isNotEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = if (accent) MpPrimary else MpTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(color = MpTextPrimary, fontSize = 15.sp),
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            cursorBrush = SolidColor(MpPrimary),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = MpTextSecondary, fontSize = 15.sp)
                    }
                    inner()
                }
            }
        )
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
    }
}
