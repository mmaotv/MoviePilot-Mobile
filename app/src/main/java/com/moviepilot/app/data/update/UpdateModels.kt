package com.moviepilot.app.data.update

import com.google.gson.annotations.SerializedName

/**
 * 一个可用的新版本。
 *
 * 数据全部来自 GitHub Release：
 * tag 形如 `v3.0.3`、附件形如 `MoviePilot-3.0.3.apk`（由发布脚本 release.py 生成），
 * 所以 [versionName] 是去掉前缀 v 的纯版本号，和 BuildConfig.VERSION_NAME 可直接比较。
 */
data class AppUpdate(
    val versionName: String,
    val tagName: String,
    val notes: String = "",
    /** Release 附件里的 APK 直链；拿不到时为 null，下载时会按命名规则拼一个 */
    val apkUrl: String? = null,
    val pageUrl: String = "",
    val sizeBytes: Long = 0L,
    val publishedAt: String = ""
)

// ── GitHub API 原始结构（只声明用得到的字段）─────────────────────────

internal data class GithubAsset(
    @SerializedName("name") val name: String = "",
    @SerializedName("size") val size: Long = 0L,
    @SerializedName("browser_download_url") val url: String = "",
    @SerializedName("content_type") val contentType: String = ""
)

internal data class GithubRelease(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("body") val body: String = "",
    @SerializedName("draft") val draft: Boolean = false,
    @SerializedName("prerelease") val prerelease: Boolean = false,
    @SerializedName("published_at") val publishedAt: String = "",
    @SerializedName("html_url") val htmlUrl: String = "",
    @SerializedName("assets") val assets: List<GithubAsset> = emptyList()
)

/**
 * 版本号比较。
 *
 * 只按数字段比，`3.0.10` > `3.0.9`（字符串比较会得出相反结果）。
 * 段数不一致时缺的位按 0 处理。
 */
object Version {

    /** `v3.0.3` / `V3.0.3` / `3.0.3` → `3.0.3` */
    fun normalize(raw: String): String =
        raw.trim().trimStart('v', 'V').trim()

    /** a 比 b 新返回正数，相同返回 0，旧返回负数 */
    fun compare(a: String, b: String): Int {
        val pa = segments(a)
        val pb = segments(b)
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return if (x > y) 1 else -1
        }
        return 0
    }

    private fun segments(v: String): List<Int> =
        normalize(v)
            .split('.', '-', '+', ' ')
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
}
