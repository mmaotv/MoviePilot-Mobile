package com.moviepilot.app.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.gson.Gson
import com.moviepilot.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 更新通道。
 *
 * 版本信息来自 GitHub Release：
 *   - 主通道：`api.github.com/repos/{repo}/releases/latest`（JSON，能拿到说明正文与附件大小）
 *   - 备用通道：`github.com/{repo}/releases/latest` 的重定向（只拿得到 tag，说明为空）
 *   - 再备用：API 与下载链接套一层国内可达的加速前缀
 *
 * 之所以做多通道：这台手机上 api.github.com / github.com 都能直连，
 * 但用户换成移动网络或其它网络环境时不一定，宁可多试几个入口也不要「检查更新失败」。
 *
 * APK 下载后会做两道校验（ZIP 魔数 + 包名），避免把错误页当成安装包存下来。
 */
@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** 发布仓库，和 release.py 里 `repo = mmaotv/MoviePilot-Mobile` 保持一致 */
        private const val REPO = "mmaotv/MoviePilot-Mobile"

        const val TAG = "MP_UPDATE"

        /**
         * 唯一实测可用的 GitHub 加速前缀（2026-11-02 在真机 Android 14 / 局域网实测）：
         *   - 能代理 GitHub 网页的 302，从而在 API 不通时仍能拿到 tag
         *   - 能代理 release 附件下载（APK）
         *   - 直接代理 api.github.com 会返回 403，所以 API 不走它
         * 其它常见镜像（gh-proxy.com / ghproxy.net / gh.llkk.cc / github.moeyy.xyz / ghproxy.cc）
         * 当时全部连接失败，加进去只会白白多等 8 秒，故只保留这一个。
         */
        private const val MIRROR_HOST = "https://ghfast.top/"

        private const val API_LATEST = "https://api.github.com/repos/$REPO/releases/latest"
        private const val HTML_LATEST = "https://github.com/$REPO/releases/latest"
        private const val RELEASES_DOWNLOAD = "https://github.com/$REPO/releases/download"

        /** 空串 = 直连；其余是 GitHub 加速前缀（仅在该网络直连不通时才会用到） */
        private val MIRRORS = listOf("", MIRROR_HOST)

        private const val UA = "MoviePilot-Android/${BuildConfig.VERSION_NAME}"
    }

    private val gson = Gson()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** 释放重定向用：只在解析 tag 的兜底通道里用到 */
    private val noRedirectClient: OkHttpClient = client.newBuilder()
        .followRedirects(false)
        .build()

    /**
     * 下载用客户端固定走 HTTP/1.1。
     * 实测在这台手机的网络下，直连下载会因为 HTTP/2 连接被中途掐断而报
     * `Required SETTINGS preface not received`，换 1.1 后同一地址就稳定了。
     */
    private val downloadClient: OkHttpClient = client.newBuilder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .build()

    // ── 检查 ─────────────────────────────────────────────────────

    /**
     * 查最近一次正式发布。
     * @return 有比当前安装版本更新的版本时返回它，否则 null。
     * @throws IOException 所有通道都失败时抛出，由上层决定是否提示。
     */
    suspend fun checkForUpdate(): AppUpdate? = withContext(Dispatchers.IO) {
        val release = fetchLatestRelease()
        val latest = pickVersion(release)
        if (latest.isBlank()) throw IOException("发布信息里没有版本号")

        val cmp = Version.compare(latest, BuildConfig.VERSION_NAME)
        Log.i(TAG, "远端最新=$latest 本机=${BuildConfig.VERSION_NAME} 比较=$cmp")
        if (cmp <= 0) null else release.toUpdate(latest)
    }

    private fun pickVersion(release: GithubRelease): String {
        val fromTag = Version.normalize(release.tagName)
        if (fromTag.isNotBlank()) return fromTag
        // 个别 release 的 tag 和标题不一致时退回标题里的第一段版本号
        return Regex("""(\d+(?:\.\d+)+)""").find(release.name)?.value.orEmpty()
    }

    private fun fetchLatestRelease(): GithubRelease {
        var lastError: Throwable? = null

        // ① 官方 API 直连（最完整：有说明正文与附件大小）。偶发连接失败时重试一次
        runCatching { retryOnce { apiJson(API_LATEST) } }
            .onSuccess { Log.i(TAG, "通道1 官方API 成功"); return it }
            .onFailure { lastError = it; Log.w(TAG, "通道1 失败: ${it.message}") }

        // ② 官方网页的 302：能拿到 tag，说明正文为空、附件地址按命名规则拼
        runCatching { releaseByRedirect("") }
            .onSuccess { Log.i(TAG, "通道2 重定向 成功 tag=${it.tagName}"); return it }
            .onFailure { lastError = it; Log.w(TAG, "通道2 失败: ${it.message}") }

        // ③ 同样的 302 走加速前缀
        runCatching { releaseByRedirect(MIRROR_HOST) }
            .onSuccess { Log.i(TAG, "通道3 镜像重定向 成功 tag=${it.tagName}"); return it }
            .onFailure { lastError = it; Log.w(TAG, "通道3 失败: ${it.message}") }

        throw IOException("检查更新失败：${lastError?.message ?: "网络不可用"}")
    }

    /** 连接层的瞬时失败很常见（实测同一网络内时通时断），读失败不重试、连不上才重试一次 */
    private inline fun <T> retryOnce(block: () -> T): T = try {
        block()
    } catch (e: IOException) {
        Log.w(TAG, "首次请求失败，1 秒后重试：${e.message}")
        Thread.sleep(1000)
        block()
    }

    private fun apiJson(url: String): GithubRelease {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IOException("空响应")
            val release = gson.fromJson(body, GithubRelease::class.java)
                ?: throw IOException("响应解析失败")
            if (release.draft) throw IOException("最新发布是草稿")
            return release
        }
    }

    /** 只取 `releases/latest` 的 302：Location 形如 `.../releases/tag/v3.0.2`（镜像前缀会一并带上） */
    private fun releaseByRedirect(prefix: String): GithubRelease {
        val request = Request.Builder()
            .url(prefix + HTML_LATEST)
            .header("User-Agent", UA)
            .build()

        noRedirectClient.newCall(request).execute().use { response ->
            val location = response.header("Location").orEmpty()
            val tag = location.substringAfterLast("/releases/tag/", "").trim('/')
            if (tag.isBlank()) throw IOException("HTTP ${response.code} 未返回 tag")
            return GithubRelease(
                tagName = tag,
                htmlUrl = "https://github.com/$REPO/releases/tag/$tag"
            )
        }
    }

    private fun GithubRelease.toUpdate(version: String): AppUpdate {
        val apk = assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true) || it.contentType.contains("android.package")
        }
        return AppUpdate(
            versionName = version,
            tagName = tagName.ifBlank { "v$version" },
            notes = body.trim(),
            apkUrl = apk?.url?.ifBlank { null } ?: guessApkUrl(tagName.ifBlank { "v$version" }, version),
            pageUrl = htmlUrl.ifBlank { "https://github.com/$REPO/releases/tag/v$version" },
            sizeBytes = apk?.size ?: 0L,
            publishedAt = publishedAt
        )
    }

    /** 附件命名固定为 `MoviePilot-{version}.apk`（见 release.config 的 asset_name） */
    private fun guessApkUrl(tag: String, version: String) =
        "$RELEASES_DOWNLOAD/$tag/MoviePilot-$version.apk"

    // ── 下载 ─────────────────────────────────────────────────────

    /**
     * 下载 APK 到缓存目录。
     *
     * @param onProgress (已下载字节, 总字节)；总字节未知时第二项为 0。
     * @return 校验通过的 APK 文件。
     */
    suspend fun download(
        update: AppUpdate,
        onProgress: (done: Long, total: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "update").apply { mkdirs() }
        // 清掉旧包，避免空间越攒越大、也避免误装上一个版本
        dir.listFiles()?.forEach { it.delete() }

        val target = File(dir, "MoviePilot-${update.versionName}.apk")
        val direct = update.apkUrl ?: guessApkUrl(update.tagName, update.versionName)

        val candidates = buildList {
            add(direct)
            MIRRORS.filter { it.isNotBlank() }.forEach { add(it + direct) }
        }

        var lastError: Throwable? = null
        for ((index, url) in candidates.withIndex()) {
            val tmp = File(dir, "download.tmp")
            try {
                Log.i(TAG, "下载尝试 ${index + 1}/${candidates.size}: $url")
                fetchTo(url, tmp, onProgress)
                verify(tmp)
                if (target.exists()) target.delete()
                if (!tmp.renameTo(target)) throw IOException("无法保存安装包")
                Log.i(TAG, "下载完成 ${target.length()} B")
                return@withContext target
            } catch (e: Throwable) {
                lastError = e
                tmp.delete()
                Log.w(TAG, "下载失败(${e.message})，换下一个通道")
            }
        }
        throw IOException("下载失败：${lastError?.message ?: "所有通道都不可用"}")
    }

    private fun fetchTo(url: String, dest: File, onProgress: (Long, Long) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Accept", "application/octet-stream")
            .build()

        downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("空响应")
            val total = body.contentLength()
            // 加速站出错时也会回 200，但内容是几百字节的 HTML，先按大小挡一层
            if (total in 1..1024) throw IOException("响应过小（${total}B），不是安装包")

            var done = 0L
            var lastReport = 0L
            dest.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        out.write(buf, 0, read)
                        done += read
                        // 每 256KB 回报一次，够顺滑又不至于把主线程刷爆
                        if (done - lastReport >= 256 * 1024) {
                            lastReport = done
                            onProgress(done, total)
                        }
                    }
                }
            }
            onProgress(done, total)
            if (total > 0 && done != total) throw IOException("下载不完整（$done/$total）")
        }
    }

    /** ZIP 魔数 + 包名双重校验，确保拿到的确实是本应用的安装包 */
    private fun verify(file: File) {
        if (!file.exists() || file.length() < 1024 * 100) {
            throw IOException("安装包不完整")
        }
        val head = ByteArray(4)
        FileInputStream(file).use { it.read(head) }
        if (head[0] != 'P'.code.toByte() || head[1] != 'K'.code.toByte()) {
            throw IOException("下载到的不是 APK")
        }

        val info = context.packageManager
            .getPackageArchiveInfo(file.absolutePath, PackageManager.GET_ACTIVITIES)
            ?: throw IOException("安装包无法解析")
        if (info.packageName != context.packageName) {
            throw IOException("安装包包名不匹配：${info.packageName}")
        }
    }
}
