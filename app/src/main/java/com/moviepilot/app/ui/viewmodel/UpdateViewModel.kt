package com.moviepilot.app.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.local.PreferencesManager
import com.moviepilot.app.data.update.ApkInstaller
import com.moviepilot.app.data.update.AppUpdate
import com.moviepilot.app.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 更新通道的界面状态机。
 *
 * 三处入口共用同一个实例（登录页/主界面的 ViewModelStore 都是 Activity）：
 *  - 冷启动自动检查（静默，只有真的有新版才弹）
 *  - 切换面板里的「检查更新」（结果一定会给反馈）
 *  - 弹窗里的下载 / 安装 / 去开权限
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: UpdateRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    data class UiState(
        /** 弹窗是否显示 */
        val visible: Boolean = false,
        val checking: Boolean = false,
        val update: AppUpdate? = null,
        val downloading: Boolean = false,
        /** 0..100 */
        val progress: Int = 0,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        /** 已经下载完、等待安装的包 */
        val apkFile: File? = null,
        /** 需要用户先打开「安装未知应用」 */
        val needPermission: Boolean = false,
        /** 已是最新版本（手动检查时的提示） */
        val upToDate: Boolean = false,
        val error: String? = null
    ) {
        val busy: Boolean get() = checking || downloading
        val hasNewVersion: Boolean get() = update != null
    }

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    private var launchCheckStarted = false

    // ── 检查 ─────────────────────────────────────────────────────

    /**
     * 冷启动检查。同一次进程只跑一次；15 分钟内查过就跳过，避免频繁冷启动刷接口。
     * 有新版才弹窗，没新版安静地什么都不做。
     */
    fun checkOnLaunch() {
        if (launchCheckStarted) return
        launchCheckStarted = true
        viewModelScope.launch {
            val last = prefs.getLastUpdateCheck()
            val elapsed = System.currentTimeMillis() - last
            if (elapsed in 1..CHECK_INTERVAL_MS) {
                Log.i(UpdateRepository.TAG, "距上次检查 ${elapsed / 60000} 分钟，本次跳过")
                return@launch
            }
            runCatching { repository.checkForUpdate() }
                .onSuccess { update ->
                    prefs.setLastUpdateCheck(System.currentTimeMillis())
                    if (update == null) {
                        Log.i(UpdateRepository.TAG, "已是最新版本")
                        return@onSuccess
                    }
                    if (prefs.getSkippedVersion() == update.versionName) {
                        Log.i(UpdateRepository.TAG, "${update.versionName} 已被用户跳过")
                        return@onSuccess
                    }
                    _ui.value = UiState(visible = true, update = update)
                }
                .onFailure { e ->
                    Log.w(UpdateRepository.TAG, "启动检查失败：${e.message}")
                }
        }
    }

    /** 手动检查：不管有没有新版都给反馈；已跳过某版本时也会重新提示 */
    fun checkManually() {
        if (_ui.value.busy) return
        viewModelScope.launch {
            _ui.value = UiState(visible = true, checking = true)
            runCatching { repository.checkForUpdate() }
                .onSuccess { update ->
                    prefs.setLastUpdateCheck(System.currentTimeMillis())
                    _ui.value = if (update == null) {
                        UiState(visible = true, upToDate = true)
                    } else {
                        // 手动检查说明用户想要这个版本，顺手清掉跳过记录
                        prefs.setSkippedVersion(null)
                        UiState(visible = true, update = update)
                    }
                }
                .onFailure { e ->
                    _ui.value = UiState(visible = true, error = e.message ?: "检查更新失败")
                }
        }
    }

    // ── 下载 ─────────────────────────────────────────────────────

    fun download() {
        val update = _ui.value.update ?: return
        if (_ui.value.downloading) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                downloading = true,
                progress = 0,
                downloadedBytes = 0,
                totalBytes = update.sizeBytes,
                error = null,
                upToDate = false
            )
            runCatching {
                repository.download(update) { done, total ->
                    val pct = if (total > 0) ((done * 100) / total).toInt() else 0
                    _ui.value = _ui.value.copy(
                        progress = pct.coerceIn(0, 100),
                        downloadedBytes = done,
                        totalBytes = if (total > 0) total else 0
                    )
                }
            }.onSuccess { file ->
                _ui.value = _ui.value.copy(
                    downloading = false,
                    progress = 100,
                    apkFile = file,
                    needPermission = !ApkInstaller.canInstall(appContext)
                )
                install()
            }.onFailure { e ->
                Log.w(UpdateRepository.TAG, "下载失败：${e.message}")
                _ui.value = _ui.value.copy(
                    downloading = false,
                    error = e.message ?: "下载失败"
                )
            }
        }
    }

    // ── 安装 ─────────────────────────────────────────────────────

    fun install() {
        val file = _ui.value.apkFile ?: return
        if (!ApkInstaller.canInstall(appContext)) {
            _ui.value = _ui.value.copy(needPermission = true)
            return
        }
        ApkInstaller.install(appContext, file)
            .onSuccess { _ui.value = _ui.value.copy(needPermission = false) }
            .onFailure { e ->
                _ui.value = _ui.value.copy(error = "调不起安装程序：${e.message}")
            }
    }

    /** 用户允许了「安装未知应用」之后回到 App，再点一次安装即可 */
    fun openPermissionSettings() {
        ApkInstaller.openInstallPermissionSettings(appContext)
    }

    fun refreshPermissionState() {
        if (_ui.value.needPermission && ApkInstaller.canInstall(appContext)) {
            _ui.value = _ui.value.copy(needPermission = false)
        }
    }

    // ── 关闭 / 跳过 ──────────────────────────────────────────────

    fun dismiss() {
        // 下载中不允许误关，否则进度回调会写到已关闭的状态上
        if (_ui.value.downloading) return
        _ui.value = UiState()
    }

    /** 本次发布的版本不再提醒（手动检查会重新提示） */
    fun skipThisVersion() {
        val version = _ui.value.update?.versionName ?: return
        viewModelScope.launch {
            prefs.setSkippedVersion(version)
            _ui.value = UiState()
        }
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 15 * 60 * 1000L
    }
}
