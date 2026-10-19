package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * 服务器档案：一台 MoviePilot 服务器一条记录。
 * 账号单独存放在 [AccountProfile]，同一服务器可以有多个账号。
 *
 * 这里没有「内置服务器列表」的概念：地址完全由用户输入，
 * 列表只是登录过的历史记录，用来下次一键切回。
 */
data class ServerProfile(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("name") val name: String,
    @SerializedName("server_url") val serverUrl: String,
    @SerializedName("last_used_at") val lastUsedAt: Long = System.currentTimeMillis()
)

/**
 * 账号档案：服务器地址 + 用户名 唯一。
 *
 * encPassword 为 AES-GCM 加密后的密码，仅当 rememberPassword=true 时存储。
 */
data class AccountProfile(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("server_url") val serverUrl: String,
    @SerializedName("username") val username: String,
    @SerializedName("remember_password") val rememberPassword: Boolean = false,
    @SerializedName("enc_password") val encPassword: String = "",
    @SerializedName("last_used_at") val lastUsedAt: Long = System.currentTimeMillis()
)

private val IPV4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

/**
 * 解析用户随便输入的服务器地址，返回规范化 baseUrl（带协议、以 / 结尾）。
 * 解析不出来返回 null。
 *
 * 支持这些写法：
 *  - `192.168.1.10:3000`
 *  - `http://192.168.1.10:3000`
 *  - `https://mp.example.com`
 *  - `mp.example.com/moviepilot`（反向代理带子路径）
 *  - `http://ip:3000/#/dashboard`（从浏览器地址栏整条粘贴）
 */
fun parseServerUrl(raw: String): String? {
    var s = raw.trim().trim('"', '\'').replace(Regex("\\s+"), "")
    if (s.isEmpty()) return null
    // 从浏览器复制来的地址常带 query / hash，去掉
    s = s.substringBefore('#').substringBefore('?')
    if (s.isEmpty()) return null

    val hasScheme = s.startsWith("http://", true) || s.startsWith("https://", true)
    if (!hasScheme) s = "http://$s"

    val scheme = s.substringBefore("://").lowercase()
    if (scheme != "http" && scheme != "https") return null

    val rest = s.substringAfter("://")
    if (rest.isEmpty()) return null
    // IPv6 字面量暂不支持，避免把 [::1] 解析错
    if (rest.startsWith("[")) return null

    val hostPart = rest.substringBefore('/')
    if (hostPart.isEmpty()) return null
    val host = hostPart.substringBefore(':')
    if (host.isEmpty()) return null
    if (host.startsWith('.') || host.startsWith('-')) return null

    val portText = hostPart.substringAfter(':', "")
    if (portText.isNotEmpty()) {
        val port = portText.toIntOrNull() ?: return null
        if (port !in 1..65535) return null
    }

    val path = rest.substringAfter('/', "").trim('/')

    return buildString {
        append(scheme).append("://").append(hostPart.lowercase())
        if (path.isNotEmpty()) append('/').append(path)
        append('/')
    }
}

/** 规范化服务器地址，解析失败返回空串 */
fun normalizeServerUrl(raw: String): String = parseServerUrl(raw).orEmpty()

/**
 * 用户没写协议时，自动准备 http / https 两个候选，登录时依次尝试。
 * 内网地址（IP / localhost / 单段主机名 / .local / .lan）优先 http，
 * 公网域名优先 https——很多反代站点只开了 https。
 */
fun serverCandidates(raw: String): List<String> {
    val parsed = parseServerUrl(raw) ?: return emptyList()
    val trimmed = raw.trim()
    if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
        return listOf(parsed)
    }
    val host = parsed.substringAfter("://").substringBefore('/').substringBefore(':')
    val isLan = host.equals("localhost", true) ||
        host == "127.0.0.1" ||
        IPV4.matches(host) ||
        !host.contains('.') ||
        host.endsWith(".local") ||
        host.endsWith(".lan")
    val http = parsed.replaceFirst("https://", "http://")
    val https = parsed.replaceFirst("http://", "https://")
    return if (isLan) listOf(http, https) else listOf(https, http)
}

/** 界面展示：保留协议（用户得知道自己连的是 http 还是 https），去掉结尾斜杠 */
fun displayServerUrl(url: String): String {
    val normalized = normalizeServerUrl(url)
    return if (normalized.isEmpty()) "" else normalized.trimEnd('/')
}

/** 服务器显示名：优先用户命名，否则用地址 */
fun serverDisplayName(server: ServerProfile?): String {
    if (server == null) return ""
    if (server.name.isNotBlank()) return server.name
    return displayServerUrl(server.serverUrl)
}
