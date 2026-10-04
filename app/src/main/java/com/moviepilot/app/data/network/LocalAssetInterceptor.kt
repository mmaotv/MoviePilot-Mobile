package com.moviepilot.app.data.network

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * PWA 静态资源本地拦截器
 *
 * 将 WebView 请求的静态资源（图标、图片、CSS、manifest 等）从 APK assets 目录直接返回，
 * 避免每次都从远程服务器下载，大幅提升页面加载速度，使应用更接近原生体验。
 *
 * 拦截策略：
 * - 根目录资源：favicon.ico, logo.png, logo.svg, icon.png, manifest.webmanifest 等
 * - assets/ 子目录资源：图片(.png/.jpg/.webp/.svg)、CSS(.css)
 * - splash/ 子目录资源：启动画面图片
 * - API 请求（/api/）和主页面（text/html）不拦截，走网络
 *
 * 缓存控制：
 * - 本地资源返回 Cache-Control: max-age=86400（缓存 1 天）
 * - 携带 ETag 便于后续 304 校验
 */
object LocalAssetInterceptor {

    private const val ASSETS_ROOT = "pwa"

    // 支持的文件扩展名 → MIME 类型
    private val EXT_MIME = mapOf(
        "png"  to "image/png",
        "jpg"  to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif"  to "image/gif",
        "webp" to "image/webp",
        "svg"  to "image/svg+xml",
        "ico"  to "image/x-icon",
        "css"  to "text/css",
        "js"   to "application/javascript",
        "json" to "application/json",
        "webmanifest" to "application/manifest+json",
        "html" to "text/html",
        "xml"  to "application/xml"
    )

    // 拦截资源路径前缀（相对于服务器根目录）
    private val INTERCEPT_PREFIXES = listOf(
        "/favicon.ico",
        "/logo.", "/icon.", "/icon-black.", "/icon-white.",
        "/apple-touch-icon",
        "/android-chrome-",
        "/clock-icon-",
        "/cog-icon-",
        "/sparkles-icon-",
        "/user-icon-",
        "/manifest.webmanifest",
        "/offline.html",
        "/splash/",
        "/assets/"  // assets 子目录下的图片和 CSS
    )

    // 不拦截的路径前缀（API 和动态内容）
    private val SKIP_PREFIXES = listOf(
        "/api/",
        "/docs",
        "/openapi.json",
        "/system/setting/config"
    )

    /**
     * 判断是否应该拦截该请求
     */
    fun shouldIntercept(request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        // 只拦截 GET 请求
        if (request.method != "GET") return false

        // 不拦截 API 和动态内容
        val path = request.url.path ?: return false
        for (prefix in SKIP_PREFIXES) {
            if (path.startsWith(prefix)) return false
        }

        // 检查是否匹配拦截前缀
        for (prefix in INTERCEPT_PREFIXES) {
            if (path.startsWith(prefix)) return true
        }

        return false
    }

    /**
     * 从 assets 目录加载资源并返回 WebResourceResponse
     */
    fun intercept(context: Context, request: WebResourceRequest): WebResourceResponse? {
        val path = request.url.path ?: return null

        // 将 URL 路径映射到 assets 路径：去掉开头的 /
        val assetPath = "$ASSETS_ROOT$path"

        return try {
            val stream = context.assets.open(assetPath)
            val mime = guessMime(path)

            WebResourceResponse(
                mime,
                "UTF-8",
                stream
            ).apply {
                // 设置缓存头，减少重复请求
                responseHeaders = mapOf(
                    "Cache-Control" to "public, max-age=86400",
                    "ETag" to "\"local-${assetPath.hashCode()}\""
                )
            }
        } catch (e: Exception) {
            // assets 中不存在该文件，返回 null 让 WebView 走网络加载
            null
        }
    }

    /**
     * 根据 URL 路径推断 MIME 类型
     */
    private fun guessMime(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase()
        return EXT_MIME[ext]
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
    }
}
