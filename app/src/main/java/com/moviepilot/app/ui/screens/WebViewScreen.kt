package com.moviepilot.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.MotionEvent
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.moviepilot.app.data.network.ApiClient
import com.moviepilot.app.data.network.LocalAssetInterceptor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * 通用 WebView 页面，嵌入展示 MoviePilot PWA/Web 界面
 *
 * ★ 共享 WebView 模式：
 * - 使用 rememberWebView 创建/复用 WebView 实例
 * - 侧边栏切换时只改 URL，不重建 WebView → 瞬间切换，无白屏
 * - 自动登录、滑动修复、本地资源拦截、连接超时检测均内置
 */

/**
 * NestedScrollConnection：把纵向滚动消耗权交还给 WebView，
 * 让父级 Compose（Scaffold/Column 等）不再拦截竖向 fling/drag。
 */
val WebViewScrollNestedConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = Offset.Zero
    override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Velocity.Zero
}

/**
 * 创建或复用 WebView 实例
 * - 首次调用时创建并配置 WebView
 * - 后续调用复用同一实例（key 不变时）
 * - 返回 WebView 引用，供外部调用 loadUrl() 等方法
 */
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
fun rememberWebView(
    onConnectionLost: (() -> Unit)? = null
): Triple<WebView?, (String) -> Unit, (String) -> Unit> {
    val context = androidx.compose.ui.platform.LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    // 连接超时检测状态
    var loadStartTime by remember { mutableStateOf(0L) }
    var isPageLoaded by remember { mutableStateOf(false) }
    var hasLoadError by remember { mutableStateOf(false) }
    var timeoutNotified by remember { mutableStateOf(false) }
    var autoLoginAttempted by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf("") }

    val CONNECTION_TIMEOUT_MS = 5_000L

    // 超时检测协程（使用安全协程，不依赖 ViewModel）
    val scope = rememberCoroutineScope()
    var timeoutJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    LaunchedEffect(loadStartTime) {
        if (loadStartTime > 0) {
            isPageLoaded = false
            hasLoadError = false
            timeoutNotified = false
            timeoutJob?.cancel()
            timeoutJob = scope.launch {
                delay(CONNECTION_TIMEOUT_MS)
                if (!isPageLoaded && !hasLoadError && !timeoutNotified) {
                    timeoutNotified = true
                    runCatching { onConnectionLost?.invoke() }
                }
            }
        }
    }

    // 创建 WebView（只执行一次）
    LaunchedEffect(Unit) {
        if (webView != null) return@LaunchedEffect

        val wv = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                userAgentString = "$userAgentString MoviePilotAndroid/1.0"
                databaseEnabled = true
                allowFileAccess = true
            }

            isVerticalScrollBarEnabled = true
            overScrollMode = android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            addJavascriptInterface(WebAppInterface(), "AndroidBridge")

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    if (request != null && LocalAssetInterceptor.shouldIntercept(request)) {
                        val localResponse = LocalAssetInterceptor.intercept(context, request)
                        if (localResponse != null) return localResponse
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    hasError = false
                    currentUrl = url ?: ""
                    autoLoginAttempted = false
                    loadStartTime = System.currentTimeMillis()
                    isPageLoaded = false
                    hasLoadError = false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    currentUrl = url ?: ""
                    isPageLoaded = true
                    if (!autoLoginAttempted) {
                        autoLoginAttempted = true
                        attemptAutoLogin(view)
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        hasError = true
                        hasLoadError = true
                        val errorCode = error?.errorCode ?: -1
                        if (errorCode == WebViewClient.ERROR_CONNECT ||
                            errorCode == WebViewClient.ERROR_HOST_LOOKUP ||
                            errorCode == WebViewClient.ERROR_TIMEOUT ||
                            errorCode == WebViewClient.ERROR_TOO_MANY_REQUESTS
                        ) {
                            runCatching { onConnectionLost?.invoke() }
                        }
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    if (request?.isForMainFrame == true) {
                        val statusCode = errorResponse?.statusCode ?: 0
                        if (statusCode in 500..599) {
                            runCatching { onConnectionLost?.invoke() }
                        }
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean = false
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {}
            }
        }

        webView = wv
    }

    // 加载 URL 的函数
    val loadPath: (String) -> Unit = { path ->
        val baseUrl = ApiClient.getBaseUrl().trimEnd('/')
        val url = "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
        webView?.loadUrl(url)
    }

    // 重新加载当前 URL 的函数
    val reloadCurrent: (String) -> Unit = { path ->
        val baseUrl = ApiClient.getBaseUrl().trimEnd('/')
        val url = "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
        hasError = false
        webView?.loadUrl(url)
    }

    return Triple(webView, loadPath, reloadCurrent)
}

/**
 * 显示共享 WebView 的 Composable
 * - 只负责渲染 WebView 视图 + 错误页面
 * - 不创建/销毁 WebView，由外部通过 rememberWebView 管理
 */
@Composable
fun SharedWebViewContainer(
    webView: WebView?,
    hasError: Boolean,
    errorUrl: String,
    onReload: () -> Unit,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    val bgColor = androidx.compose.ui.graphics.Color(0xFFF8F8FF)
    val accentColor = androidx.compose.ui.graphics.Color(0xFF6200EA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .nestedScroll(WebViewScrollNestedConnection)
    ) {
        // 渲染 WebView
        if (webView != null) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 加载失败提示
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("⚠", fontSize = 40.sp, color = accentColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("页面加载失败", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorUrl, color = androidx.compose.ui.graphics.Color(0xFF808080), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onReload,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("重新加载", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }

        // 返回按钮（部分页面需要）
        if (showBackButton) {
            IconButton(
                onClick = { onBack?.invoke() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp, start = 4.dp)
                    .statusBarsPadding()
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 独立 WebView 页面（用于设置子页面等独立场景）
 * - 保留兼容性，供非共享场景使用
 */
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
fun WebViewScreen(
    path: String,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
    onConnectionLost: (() -> Unit)? = null
) {
    val baseUrl = ApiClient.getBaseUrl().trimEnd('/')
    val targetUrl = "$baseUrl${if (path.startsWith("/")) path else "/$path"}"

    var hasError by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(targetUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var autoLoginAttempted by remember { mutableStateOf(false) }

    var loadStartTime by remember { mutableStateOf(0L) }
    var isPageLoaded by remember { mutableStateOf(false) }
    var hasLoadError by remember { mutableStateOf(false) }
    var timeoutNotified by remember { mutableStateOf(false) }

    val CONNECTION_TIMEOUT_MS = 5_000L

    LaunchedEffect(loadStartTime) {
        if (loadStartTime > 0) {
            isPageLoaded = false
            hasLoadError = false
            timeoutNotified = false
            delay(CONNECTION_TIMEOUT_MS)
            if (!isPageLoaded && !hasLoadError && !timeoutNotified) {
                timeoutNotified = true
                onConnectionLost?.invoke()
            }
        }
    }

    val bgColor = androidx.compose.ui.graphics.Color(0xFFF8F8FF)
    val accentColor = androidx.compose.ui.graphics.Color(0xFF6200EA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .nestedScroll(WebViewScrollNestedConnection)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                        userAgentString = "$userAgentString MoviePilotAndroid/1.0"
                        databaseEnabled = true
                        allowFileAccess = true
                    }

                    isVerticalScrollBarEnabled = true
                    overScrollMode = android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS

                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    addJavascriptInterface(WebAppInterface(), "AndroidBridge")

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?, request: WebResourceRequest?
                        ): WebResourceResponse? {
                            if (request != null && LocalAssetInterceptor.shouldIntercept(request)) {
                                val localResponse = LocalAssetInterceptor.intercept(context, request)
                                if (localResponse != null) return localResponse
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            hasError = false
                            currentUrl = url ?: targetUrl
                            autoLoginAttempted = false
                            loadStartTime = System.currentTimeMillis()
                            isPageLoaded = false
                            hasLoadError = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            currentUrl = url ?: targetUrl
                            isPageLoaded = true
                            if (!autoLoginAttempted) {
                                autoLoginAttempted = true
                                attemptAutoLogin(view)
                            }
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                hasLoadError = true
                                val errorCode = error?.errorCode ?: -1
                                if (errorCode == WebViewClient.ERROR_CONNECT ||
                                    errorCode == WebViewClient.ERROR_HOST_LOOKUP ||
                                    errorCode == WebViewClient.ERROR_TIMEOUT ||
                                    errorCode == WebViewClient.ERROR_TOO_MANY_REQUESTS
                                ) {
                                    onConnectionLost?.invoke()
                                }
                            }
                        }

                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                            if (request?.isForMainFrame == true) {
                                val statusCode = errorResponse?.statusCode ?: 0
                                if (statusCode in 500..599) {
                                    onConnectionLost?.invoke()
                                }
                            }
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {}
                    }

                    webViewRef = this
                    loadUrl(targetUrl)
                }
            },
            update = { webView -> webViewRef = webView },
            modifier = Modifier.fillMaxSize()
        )

        if (hasError) {
            Box(
                modifier = Modifier.fillMaxSize().background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("⚠", fontSize = 40.sp, color = accentColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("页面加载失败", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(targetUrl, color = androidx.compose.ui.graphics.Color(0xFF808080), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { hasError = false; webViewRef?.loadUrl(targetUrl) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = androidx.compose.ui.graphics.Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("重新加载", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }

        if (showBackButton) {
            IconButton(
                onClick = { onBack?.invoke() },
                modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp, start = 4.dp).statusBarsPadding()
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回", tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { webViewRef?.stopLoading() }
    }
}

/**
 * JavaScript 接口，用于向 WebView 提供 Android 端保存的凭证
 */
class WebAppInterface {
    @android.webkit.JavascriptInterface
    fun getUsername(): String = ApiClient.getCredentials()?.first ?: ""

    @android.webkit.JavascriptInterface
    fun getPassword(): String = ApiClient.getCredentials()?.second ?: ""
}

/**
 * 尝试自动登录：如果检测到登录页且有保存的凭证，则自动填充并提交
 */
private fun attemptAutoLogin(webView: WebView?) {
    if (webView == null) return
    val credentials = ApiClient.getCredentials() ?: return
    val (username, password) = credentials
    if (username.isEmpty() || password.isEmpty()) return

    val autoLoginScript = """
        (function(username, password) {
            var MAX_WAIT = 8000;
            var INTERVAL = 400;
            var elapsed = 0;

            function tryLogin() {
                var userField = document.querySelector('input[name="username"]')
                    || document.querySelector('input[id="username"]')
                    || document.querySelector('input[placeholder*="用户名"]')
                    || document.querySelector('input[placeholder*="账号"]')
                    || document.querySelector('input[autocomplete="username"]')
                    || document.querySelector('input[type="text"]');

                var passField = document.querySelector('input[name="password"]')
                    || document.querySelector('input[id="password"]')
                    || document.querySelector('input[type="password"]')
                    || document.querySelector('input[placeholder*="密码"]')
                    || document.querySelector('input[autocomplete="current-password"]');

                if (!userField || !passField) {
                    if (elapsed < MAX_WAIT) {
                        elapsed += INTERVAL;
                        setTimeout(tryLogin, INTERVAL);
                    }
                    return;
                }

                userField.value = username;
                passField.value = password;
                userField.dispatchEvent(new Event('input', { bubbles: true }));
                passField.dispatchEvent(new Event('input', { bubbles: true }));

                setTimeout(function() {
                    var btn = document.querySelector('button[type="submit"]')
                        || document.querySelector('input[type="submit"]');
                    if (!btn) {
                        var allBtns = document.querySelectorAll('button');
                        for (var i = 0; i < allBtns.length; i++) {
                            var t = allBtns[i].textContent || '';
                            if (t.includes('登录') || t.includes('Login') || t.includes('登 录')) {
                                btn = allBtns[i];
                                break;
                            }
                        }
                    }
                    if (btn) btn.click();
                }, 200);
            }

            tryLogin();
        })('${username.replace("\\", "\\\\").replace("'", "\\'")}', '${password.replace("\\", "\\\\").replace("'", "\\'")}');
    """.trimIndent()

    webView.evaluateJavascript(autoLoginScript, null)
}
