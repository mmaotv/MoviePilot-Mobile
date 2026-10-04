package com.moviepilot.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moviepilot.app.R
import com.moviepilot.app.data.model.ServerProfile
import com.moviepilot.app.ui.viewmodel.AuthViewModel

// ── MoviePilot 品牌配色系统（对齐 PWA 前端） ──────────────────────────────────

// 背景
private val BgPrimary      = Color(0xFFE8E4DF)
private val BgSecondary    = Color(0xFFDDD9D4)

// 卡片
private val CardBg         = Color(0xFFFFFFFF)
private val CardBorder     = Color(0xFFE0DBD6)
private val FieldDivider   = Color(0xFFEEEAE5)

// 品牌
private val BrandPurple    = Color(0xFF7C3AED)
private val BrandPurpleLight = Color(0xFF8B5CF6)
private val BrandPurpleDisabled = Color(0xFFC4B5FD)
private val BrandPurpleBg  = Color(0xFFF3F0FF)

// 文字
private val TextPrimary    = Color(0xFF1A1A2E)
private val TextSecondary  = Color(0xFF6B7280)
private val TextPlaceholder= Color(0xFFB0A8A0)

// 错误
private val ErrorRed       = Color(0xFFEF4444)
private val ErrorBg        = Color(0xFFFEE2E2)
private val ErrorBorder    = Color(0xFFFCA5A5)

// 成功/已记住
private val BrandGreen     = Color(0xFF22C55E)

// 警告（连接超时）
private val WarnOrange     = Color(0xFFF59E0B)
private val WarnBg         = Color(0xFFFFFBEB)
private val WarnBorder     = Color(0xFFFDE68A)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current

    var username        by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var serverUrl       by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val loginState       by viewModel.loginState.collectAsState()
    val currentServerUrl by viewModel.serverUrl.collectAsState()
    val serverProfiles   by viewModel.serverProfiles.collectAsState()
    val activeServerId   by viewModel.activeServerId.collectAsState()
    val rememberPassword by viewModel.rememberPassword.collectAsState()
    val connectionLost   by viewModel.connectionLost.collectAsState()

    // 用户快速选择面板
    var showUserPicker   by remember { mutableStateOf(false) }
    // 服务器选择面板
    var showServerPicker by remember { mutableStateOf(false) }

    // 连接超时提示显示状态
    var showConnectionWarning by remember { mutableStateOf(false) }

    // 当 ViewModel 通知连接丢失时，显示提示
    LaunchedEffect(connectionLost) {
        if (connectionLost != null) {
            showConnectionWarning = true
        }
    }

    // 从当前激活的 ServerProfile 预填用户名和密码
    LaunchedEffect(activeServerId, serverProfiles) {
        val activeProfile = serverProfiles.find { it.id == activeServerId }
        if (activeProfile != null) {
            if (activeProfile.username.isNotEmpty() && username.isEmpty()) {
                username = activeProfile.username
            }
            // 如果记住密码，自动填充密码（解密失败不崩溃）
            if (activeProfile.rememberPassword && password.isEmpty()) {
                try {
                    val decryptedPwd = viewModel.getDecryptedPassword(activeProfile)
                    if (decryptedPwd != null) {
                        password = decryptedPwd
                    }
                } catch (_: Exception) {
                    // 解密失败，跳过自动填充
                }
            }
        }
    }

    fun getDisplayUrl() = serverUrl
        .removePrefix("https://")
        .removePrefix("http://")
        .removeSuffix("/")

    fun saveServerUrl(input: String) {
        serverUrl = input
        val full = if (input.startsWith("http://") || input.startsWith("https://")) input
                   else "http://$input"
        viewModel.setServerUrl(full)
    }

    LaunchedEffect(currentServerUrl) {
        if (serverUrl.isEmpty() && currentServerUrl.isNotEmpty()) {
            serverUrl = currentServerUrl
        }
    }
    // ⚠️ 仅在 loginState 从非 Success 变为 Success 时触发，避免首次合成时误触发
    var previousLoginState by remember { mutableStateOf<AuthViewModel.LoginState>(AuthViewModel.LoginState.Idle) }
    LaunchedEffect(loginState) {
        if (previousLoginState !is AuthViewModel.LoginState.Success &&
            loginState is AuthViewModel.LoginState.Success) {
            onLoginSuccess()
        }
        previousLoginState = loginState
    }

    // 入场动画
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Logo 呼吸动画
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.96f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to BgSecondary,
                        1.0f to BgPrimary
                    ),
                    radius = 1200f
                )
            )
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)) + slideInVertically(
                tween(500, easing = EaseOutCubic)
            ) { it / 6 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
                    .windowInsetsPadding(WindowInsets.systemBars),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(48.dp))

                // ── Logo 区域（无框纯LOGO样式）───────────────────────────
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "MoviePilot Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse },
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(24.dp))

                // 品牌名
                Text(
                    text = "MOVIEPILOT",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "影视管理 · 自动化订阅",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(36.dp))

                // ── 连接超时警告 ──────────────────────────────────
                AnimatedVisibility(
                    visible = showConnectionWarning,
                    enter = fadeIn() + expandVertically(),
                    exit  = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(WarnBg)
                            .border(1.dp, WarnBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", fontSize = 14.sp)
                        Text(
                            text = connectionLost ?: "无法连接到服务器",
                            color = WarnOrange,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            showConnectionWarning = false
                            viewModel.clearConnectionLost()
                        }) {
                            Text("关闭", color = WarnOrange, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── 登录卡片（白色卡片，PWA 风格） ────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color(0x14000000),
                            spotColor = Color(0x1A7C3AED)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column {
                        // ── 用户快速选择 ──────────────────────────────
                        val profiles = serverProfiles.filter { it.username.isNotEmpty() }
                        if (profiles.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .background(BrandPurpleBg.copy(alpha = 0.4f))
                                    .clickable { showUserPicker = !showUserPicker }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = BrandPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "快速选择用户",
                                    color = BrandPurple,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (showUserPicker) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = BrandPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // ── 用户选择下拉面板 ───────────────────────
                            AnimatedVisibility(
                                visible = showUserPicker,
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF9F7F5))
                                        .padding(vertical = 4.dp)
                                ) {
                                    profiles.sortedByDescending { it.lastUsedAt }.forEach { profile ->
                                        val isActive = profile.id == activeServerId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    // 快速切换：自动填充服务器/账号/密码
                                                    viewModel.switchToServer(profile)
                                                    showUserPicker = false
                                                    val displayUrl = profile.serverUrl
                                                        .removePrefix("https://")
                                                        .removePrefix("http://")
                                                        .removeSuffix("/")
                                                    serverUrl = displayUrl
                                                    username = profile.username
                                                    // 自动填充密码（解密失败不崩溃）
                                                    val decryptedPwd = try {
                                                        viewModel.getDecryptedPassword(profile)
                                                    } catch (_: Exception) { null }
                                                    password = decryptedPwd ?: ""
                                                }
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 用户头像
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isActive) BrandPurple.copy(alpha = 0.15f)
                                                        else Color(0xFFE8E4DF)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = profile.username.take(1).uppercase(),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isActive) BrandPurple else TextSecondary
                                                )
                                            }

                                            Spacer(Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = profile.username,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isActive) BrandPurple else TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = profile.name,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // 记住密码标记
                                            if (profile.rememberPassword) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "已记住密码",
                                                    tint = BrandGreen,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                            }

                                            if (isActive) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = BrandPurple.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = " 当前 ",
                                                        fontSize = 10.sp,
                                                        color = BrandPurple,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            FieldSeparator()
                        }

                        // 服务器地址（带历史选择入口）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                tint = if (getDisplayUrl().isNotEmpty()) BrandPurple else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(Modifier.width(12.dp))

                            androidx.compose.foundation.text.BasicTextField(
                                value = getDisplayUrl(),
                                onValueChange = { saveServerUrl(it) },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = 0.sp
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                cursorBrush = SolidColor(BrandPurple),
                                decorationBox = { inner ->
                                    Box {
                                        if (getDisplayUrl().isEmpty()) {
                                            Text(
                                                text = "服务器地址  例：192.168.1.100:5000",
                                                color = TextPlaceholder,
                                                fontSize = 15.sp
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )

                            // 服务器历史下拉按钮
                            if (serverProfiles.size > 1) {
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { showServerPicker = !showServerPicker },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "选择服务器",
                                        tint = BrandPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // ── 服务器历史选择下拉面板 ─────────────────────
                        AnimatedVisibility(
                            visible = showServerPicker && serverProfiles.size > 1,
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF9F7F5))
                                    .padding(vertical = 6.dp)
                            ) {
                                serverProfiles.sortedByDescending { it.lastUsedAt }.forEach { profile ->
                                    val isActive = profile.id == activeServerId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.switchToServer(profile)
                                                showServerPicker = false
                                                val displayUrl = profile.serverUrl
                                                    .removePrefix("https://")
                                                    .removePrefix("http://")
                                                    .removeSuffix("/")
                                                serverUrl = displayUrl
                                                username = profile.username
                                                val decryptedPwd = try {
                                                    viewModel.getDecryptedPassword(profile)
                                                } catch (_: Exception) { null }
                                                password = decryptedPwd ?: ""
                                            }
                                            .padding(horizontal = 20.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isActive) BrandPurple.copy(alpha = 0.15f)
                                                    else Color(0xFFE8E4DF)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Language,
                                                contentDescription = null,
                                                tint = if (isActive) BrandPurple else TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = profile.name,
                                                fontSize = 13.sp,
                                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isActive) BrandPurple else TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (profile.username.isNotEmpty()) {
                                                Text(
                                                    text = profile.username,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (isActive) {
                                            Text(
                                                text = "当前",
                                                fontSize = 11.sp,
                                                color = BrandPurple,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        FieldSeparator()

                        // 用户名
                        MpTextField(
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

                        FieldSeparator()

                        // 密码
                        MpTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "密码",
                            leadingIcon = Icons.Outlined.Lock,
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None else PasswordVisualTransformation(),
                            trailingContent = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible)
                                            Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                                        saveServerUrl(getDisplayUrl())
                                        viewModel.login(username, password)
                                    }
                                }
                            )
                        )

                        // ── 记住密码勾选 ──────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberPassword,
                                onCheckedChange = { viewModel.setRememberPassword(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = BrandPurple,
                                    uncheckedColor = TextSecondary
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "记住密码",
                                fontSize = 13.sp,
                                color = if (rememberPassword) TextPrimary else TextSecondary,
                                modifier = Modifier.clickable {
                                    viewModel.setRememberPassword(!rememberPassword)
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── 登录按钮 ──────────────────────────────────────
                val isLoading = loginState is AuthViewModel.LoginState.Loading
                val canLogin  = !isLoading && serverUrl.isNotBlank()
                               && username.isNotBlank() && password.isNotBlank()

                Button(
                    onClick = {
                        try {
                            saveServerUrl(getDisplayUrl())
                            viewModel.login(username, password)
                        } catch (e: Exception) {
                            // 防止 onClick 内未捕获的异常导致崩溃
                        }
                    },
                    enabled = canLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPurple,
                        disabledContainerColor = BrandPurpleDisabled
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation  = 0.dp
                    )
                ) {
                    AnimatedContent(
                        targetState = isLoading,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "btn"
                    ) { loading ->
                        if (loading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    "正在连接...",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("→", color = Color.White, fontSize = 16.sp)
                                Text(
                                    "登录",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                // ── 错误提示 ──────────────────────────────────────
                AnimatedVisibility(
                    visible = loginState is AuthViewModel.LoginState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit  = fadeOut() + shrinkVertically()
                ) {
                    val errMsg = (loginState as? AuthViewModel.LoginState.Error)?.message ?: ""
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ErrorBg)
                            .border(1.dp, ErrorBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", fontSize = 15.sp)
                        Text(
                            text = errMsg,
                            color = ErrorRed,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── 底部信息 ──────────────────────────────────────
                Text(
                    text = if (currentServerUrl.isNotEmpty()) currentServerUrl else "尚未配置服务器",
                    color = if (currentServerUrl.isNotEmpty()) BrandPurple else TextPlaceholder,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(56.dp))
            }
        }
    }
}

// ── 可复用组件 ─────────────────────────────────────────────────────────────

/** 输入行内分隔线 */
@Composable
private fun FieldSeparator() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        thickness = 0.5.dp,
        color = FieldDivider
    )
}

/** MoviePilot 风格输入行：白底，无外边框，仅靠分隔线区分 */
@Composable
private fun MpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = if (value.isNotEmpty()) BrandPurple else TextSecondary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(12.dp))

        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            ),
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            cursorBrush = SolidColor(BrandPurple),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = TextPlaceholder,
                            fontSize = 15.sp
                        )
                    }
                    inner()
                }
            }
        )

        if (trailingContent != null) {
            Spacer(Modifier.width(4.dp))
            trailingContent()
        }
    }
}
