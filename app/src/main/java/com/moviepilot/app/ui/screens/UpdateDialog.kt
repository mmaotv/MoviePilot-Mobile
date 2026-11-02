package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviepilot.app.BuildConfig
import com.moviepilot.app.ui.theme.MpDanger
import com.moviepilot.app.ui.theme.MpPrimary
import com.moviepilot.app.ui.theme.MpPrimarySoft
import com.moviepilot.app.ui.theme.MpSurface
import com.moviepilot.app.ui.theme.MpSurfaceVariant
import com.moviepilot.app.ui.theme.MpTextPrimary
import com.moviepilot.app.ui.theme.MpTextSecondary
import com.moviepilot.app.ui.viewmodel.UpdateViewModel

/**
 * 更新弹窗。一个弹窗覆盖全部状态：检查中 / 有新版本 / 下载中 / 需要授权 / 失败 / 已是最新。
 */
@Composable
fun UpdateDialog(
    state: UpdateViewModel.UiState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.visible) return

    val update = state.update

    AlertDialog(
        onDismissRequest = { if (!state.downloading) onDismiss() },
        shape = RoundedCornerShape(18.dp),
        containerColor = MpSurface,
        title = {
            Column {
                Text(
                    text = when {
                        state.checking -> "检查更新"
                        state.upToDate -> "已是最新版本"
                        update != null -> "发现新版本 v${update.versionName}"
                        state.error != null -> "更新失败"
                        else -> "检查更新"
                    },
                    color = MpTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (update != null) {
                        "当前版本 v${BuildConfig.VERSION_NAME}"
                    } else {
                        "MoviePilot v${BuildConfig.VERSION_NAME}"
                    },
                    color = MpTextSecondary,
                    fontSize = 11.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when {
                    state.checking -> {
                        Text("正在连接 GitHub 检查版本…", color = MpTextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MpPrimary,
                            trackColor = MpSurfaceVariant
                        )
                    }

                    state.upToDate -> {
                        Text(
                            "已经是最新版本，等下次发布后会再提醒你。",
                            color = MpTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    state.error != null -> {
                        Text(state.error, color = MpDanger, fontSize = 13.sp)
                    }

                    state.needPermission -> {
                        Text(
                            "安装前需要允许本应用「安装未知应用」。\n" +
                                "打开开关后返回本页，再点一次安装即可。",
                            color = MpTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    state.downloading -> DownloadProgress(state)

                    update != null -> UpdateBrief(state)
                }
            }
        },
        confirmButton = {
            when {
                state.checking -> Unit

                state.upToDate -> TextButton(onClick = onDismiss) {
                    Text("好", color = MpPrimary, fontSize = 14.sp)
                }

                state.error != null -> TextButton(onClick = onRetry) {
                    Text("重试", color = MpPrimary, fontSize = 14.sp)
                }

                state.needPermission -> TextButton(onClick = onOpenSettings) {
                    Text("去开启", color = MpPrimary, fontSize = 14.sp)
                }

                state.downloading -> Unit

                state.apkFile != null -> TextButton(onClick = onInstall) {
                    Text("安装", color = MpPrimary, fontSize = 14.sp)
                }

                update != null -> TextButton(onClick = onDownload) {
                    Text("立即更新", color = MpPrimary, fontSize = 14.sp)
                }
            }
        },
        dismissButton = {
            when {
                state.checking || state.downloading || state.upToDate -> Unit

                state.error != null -> TextButton(onClick = onDismiss) {
                    Text("关闭", color = MpTextSecondary, fontSize = 14.sp)
                }

                state.needPermission -> TextButton(onClick = onDismiss) {
                    Text("取消", color = MpTextSecondary, fontSize = 14.sp)
                }

                update != null && state.apkFile == null -> Row {
                    TextButton(onClick = onSkip) {
                        Text("跳过此版本", color = MpTextSecondary, fontSize = 12.sp)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("稍后", color = MpTextSecondary, fontSize = 14.sp)
                    }
                }

                else -> Unit
            }
        }
    )
}

@Composable
private fun DownloadProgress(state: UpdateViewModel.UiState) {
    val percent = state.progress
    Text(
        text = if (state.totalBytes > 0) {
            "正在下载 $percent% · ${humanSize(state.downloadedBytes)} / ${humanSize(state.totalBytes)}"
        } else {
            "正在下载 ${humanSize(state.downloadedBytes)}"
        },
        color = MpTextPrimary,
        fontSize = 13.sp
    )
    Spacer(Modifier.height(12.dp))
    LinearProgressIndicator(
        progress = { percent / 100f },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = MpPrimary,
        trackColor = MpSurfaceVariant
    )
    Spacer(Modifier.height(10.dp))
    Text(
        "下载完成后会直接拉起系统安装程序。",
        color = MpTextSecondary,
        fontSize = 11.sp
    )
}

@Composable
private fun UpdateBrief(state: UpdateViewModel.UiState) {
    val update = state.update ?: return

    if (state.apkFile != null) {
        Text(
            "安装包已下载完成，点「安装」继续。",
            color = MpTextSecondary,
            fontSize = 13.sp
        )
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        InfoChip(text = humanSize(update.sizeBytes))
        if (update.publishedAt.isNotBlank()) {
            Spacer(Modifier.width(6.dp))
            InfoChip(text = update.publishedAt.take(10))
        }
    }

    if (update.notes.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MpSurfaceVariant)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = cleanNotes(update.notes),
                color = MpTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MpPrimarySoft)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = text, color = MpPrimary, fontSize = 11.sp)
    }
}

private fun humanSize(bytes: Long): String = when {
    bytes <= 0 -> "未知大小"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
}

/** Release 正文是 Markdown，弹窗里只做最轻的清理，保留换行与要点 */
private fun cleanNotes(raw: String): String = raw
    .replace("\r\n", "\n")
    .lines()
    .joinToString("\n") { line ->
        line.trimStart('#').trim()
            .replace("**", "")
            .replace("`", "")
    }
    .replace(Regex("\n{3,}"), "\n\n")
    .trim()
