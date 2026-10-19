package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviepilot.app.data.model.AccountProfile
import com.moviepilot.app.data.model.ServerProfile
import com.moviepilot.app.data.model.displayServerUrl
import com.moviepilot.app.data.model.parseServerUrl
import com.moviepilot.app.ui.theme.MpDanger
import com.moviepilot.app.ui.theme.MpOutline
import com.moviepilot.app.ui.theme.MpPrimary
import com.moviepilot.app.ui.theme.MpScrim
import com.moviepilot.app.ui.theme.MpSurface
import com.moviepilot.app.ui.theme.MpSurfaceVariant
import com.moviepilot.app.ui.theme.MpTextPrimary
import com.moviepilot.app.ui.theme.MpTextSecondary
import com.moviepilot.app.ui.viewmodel.AuthViewModel

/**
 * 唯一的原生功能面板：切换服务器 / 切换账号 / 退出登录。
 * 无滑入动画，直接覆盖在网页版之上。
 */
@Composable
fun SwitchPanel(
    state: AuthViewModel.UiState,
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    // 服务器地址随手输入，不受已保存列表限制
    var serverInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }
    val resolvedUrl = parseServerUrl(serverInput)

    fun submitServer() {
        if (resolvedUrl == null) {
            inputError = if (serverInput.isBlank()) "请输入服务器地址" else "地址格式不正确"
            return
        }
        inputError = null
        viewModel.connectToServer(serverInput)
        serverInput = ""
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MpScrim)
                .clickable { onDismiss() }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.74f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MpSurface)
        ) {
            // 顶部把手
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MpOutline)
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "切换",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = MpTextPrimary
                    )
                    Text(
                        text = buildString {
                            append(displayServerUrl(state.serverUrl))
                            if (state.currentUsername.isNotBlank()) {
                                append(" · ").append(state.currentUsername)
                            }
                        },
                        fontSize = 11.sp,
                        color = MpTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MpTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = MpOutline.copy(alpha = 0.6f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── 服务器 ─────────────────────────────────────
                SectionHeader(title = "服务器")

                // 直接输入任意地址，不限于下面已保存的列表
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PanelField(
                            value = serverInput,
                            onValueChange = {
                                serverInput = it
                                inputError = null
                            },
                            placeholder = "输入服务器地址，如 192.168.1.10:3000",
                            modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Uri,
                            onSubmit = { submitServer() }
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (resolvedUrl != null) MpPrimary
                                    else MpSurfaceVariant
                                )
                                .clickable { submitServer() }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "连接",
                                fontSize = 13.sp,
                                color = if (resolvedUrl != null) Color.White else MpTextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = inputError
                            ?: "http / https、域名或 IP、带子路径都行，连过会自动存到下面",
                        fontSize = 10.sp,
                        color = if (inputError != null) MpDanger else MpTextSecondary
                    )
                }

                if (state.servers.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "已保存",
                            fontSize = 10.sp,
                            color = MpTextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                }

                state.servers.forEach { server ->
                    ServerItem(
                        server = server,
                        active = server.serverUrl == state.serverUrl,
                        onClick = { viewModel.switchServer(server) },
                        onDelete = { viewModel.deleteServer(server.id) }
                    )
                }

                if (state.servers.isEmpty()) {
                    Text(
                        text = "还没有保存的服务器，上面输入地址连一次就会记在这里",
                        fontSize = 11.sp,
                        color = MpTextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // ── 账号 ───────────────────────────────────────
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MpOutline.copy(alpha = 0.6f))
                SectionHeader(
                    title = "账号",
                    action = "新增",
                    onAction = { viewModel.prepareNewAccount() }
                )

                if (state.accounts.isEmpty()) {
                    Text(
                        text = "还没有保存的账号，登录一次后会自动记在这里",
                        fontSize = 12.sp,
                        color = MpTextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                } else {
                    state.accounts.forEach { account ->
                        AccountItem(
                            account = account,
                            active = account.username.equals(state.currentUsername, true),
                            showPasswordSaved = account.rememberPassword,
                            onClick = { viewModel.switchAccount(account) },
                            onDelete = { viewModel.deleteAccount(account.id) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            HorizontalDivider(color = MpOutline.copy(alpha = 0.6f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .clickable { viewModel.logout() }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "退出登录",
                    fontSize = 14.sp,
                    color = MpDanger,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "回到登录页",
                    fontSize = 11.sp,
                    color = MpTextSecondary
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = MpTextSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f)
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.height(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MpPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(action, color = MpPrimary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ServerItem(
    server: ServerProfile,
    active: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
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
            Badge(text = "当前")
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除服务器",
                tint = MpTextSecondary.copy(alpha = 0.55f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AccountItem(
    account: AccountProfile,
    active: Boolean,
    showPasswordSaved: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (active) MpPrimary.copy(alpha = 0.2f) else MpSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = account.username.take(1).uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (active) MpPrimary else MpTextSecondary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.username,
                fontSize = 14.sp,
                color = if (active) MpPrimary else MpTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showPasswordSaved) {
                Text(
                    text = "已保存密码",
                    fontSize = 10.sp,
                    color = MpTextSecondary
                )
            }
        }
        if (active) {
            Badge(text = "当前")
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除账号",
                tint = MpTextSecondary.copy(alpha = 0.55f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun Badge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(MpPrimary.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 10.sp, color = MpPrimary)
    }
}

@Composable
private fun PanelField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onSubmit: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(MpSurfaceVariant)
            .border(1.dp, MpOutline, RoundedCornerShape(9.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = MpTextPrimary, fontSize = 13.sp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Default
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (onSubmit != null) onSubmit() }
            ),
            cursorBrush = SolidColor(MpPrimary),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = MpTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
            }
        )
    }
}
