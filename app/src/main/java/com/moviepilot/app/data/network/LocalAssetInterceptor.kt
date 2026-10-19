package com.moviepilot.app.data.network

import android.content.Context
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

/**
 * 只拦截网页版根目录的几个图标/Logo，从 APK assets 直接返回，
 * 省掉每次启动的重复下载。
 *
 * 刻意不再拦截 /assets/ 下的前端构建产物：
 * 那些文件名带 hash，会随前端升级失效，留着只会占体积、并让图标长期不更新。
 */
object LocalAssetInterceptor {

    private const val ASSETS_ROOT = "pwa"

    private val ALLOWED_PATHS = setOf(
        "/favicon.ico",
        "/favicon-16x16.png",
        "/favicon-32x32.png",
        "/logo.png",
        "/logo.svg",
        "/apple-touch-icon.png",
        "/apple-touch-icon-180x180.png",
        "/android-chrome-192x192.png",
        "/android-chrome-512x512.png"
    )

    private val EXT_MIME = mapOf(
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "webp" to "image/webp",
        "svg" to "image/svg+xml",
        "ico" to "image/x-icon",
        "css" to "text/css",
        "js" to "application/javascript"
    )

    fun shouldIntercept(request: WebResourceRequest): Boolean {
        if (request.method != "GET") return false
        // 只替换当前连接的服务器，避免把别的服务器的图标也换成本地图
        val host = ApiClient.getBaseUrlHost()
        if (host.isEmpty() || !request.url.host.equals(host, true)) return false
        val path = request.url.path ?: return false
        return ALLOWED_PATHS.contains(path)
    }

    fun intercept(context: Context, request: WebResourceRequest): WebResourceResponse? {
        val host = ApiClient.getBaseUrlHost()
        if (host.isEmpty() || !request.url.host.equals(host, true)) return null
        val path = request.url.path ?: return null
        if (!ALLOWED_PATHS.contains(path)) return null
        val assetPath = "$ASSETS_ROOT$path"

        return try {
            val stream = context.assets.open(assetPath)
            WebResourceResponse(guessMime(path), null, stream).apply {
                responseHeaders = mapOf(
                    "Cache-Control" to "public, max-age=604800",
                    "ETag" to "\"mp-${assetPath.hashCode()}\""
                )
            }
        } catch (e: Exception) {
            // assets 里没有就交给网络
            null
        }
    }

    private fun guessMime(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase()
        return EXT_MIME[ext]
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
    }
}
