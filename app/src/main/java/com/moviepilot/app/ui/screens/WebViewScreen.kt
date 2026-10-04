package com.moviepilot.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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
                    loadStartTime = System.currentTimeMillis()
                    isPageLoaded = false
                    hasLoadError = false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    currentUrl = url ?: ""
                    isPageLoaded = true
                    injectOverlayFix(view)
                    injectAuthToken(view)
                    injectApiProbe(view)
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
    //
    // 服务器 PWA 的 axios baseURL 是相对路径 `api/v1/`，依赖文档自身的
    // location 解析。若这里只改 hash 让 SPA 内部跳转，页面基址与后续
    // 组件缓存状态可能不一致，导致部分页面出现「服务器连接失败」。
    // 因此统一整页加载，行为与浏览器地址栏输入完全一致，最稳妥。
    // 代价是切换时有一次加载过程，但服务器在同一局域网内，实测可接受。
    val loadPath: (String) -> Unit = { path ->
        val baseUrl = ApiClient.getBaseUrl().trimEnd('/')
        val url = baseUrl + (if (path.startsWith("/")) path else "/$path")
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
    // 配色与服务器 PWA 深色主题一致，避免 WebView 区域与原生侧边栏割裂
    val bgColor = androidx.compose.ui.graphics.Color(0xFF0E1116)
    val accentColor = androidx.compose.ui.graphics.Color(0xFF8D51F9)

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

    // 配色与服务器 PWA 深色主题一致，避免 WebView 区域与原生侧边栏割裂
    val bgColor = androidx.compose.ui.graphics.Color(0xFF0E1116)
    val accentColor = androidx.compose.ui.graphics.Color(0xFF8D51F9)

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
                                    loadStartTime = System.currentTimeMillis()
                            isPageLoaded = false
                            hasLoadError = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            currentUrl = url ?: targetUrl
                            isPageLoaded = true
                            injectOverlayFix(view)
                            injectAuthToken(view)
                            injectApiProbe(view)
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
 * 注入样式 + 诊断，修正弹层在原生容器中的定位并回报实际几何信息
 *
 * 现象：点击「资源搜索」的筛选维度时，只出现一层灰色遮罩，弹层内容不显示；
 *       而「综合筛选」入口可以正常弹出。
 *
 * 注意：CSS/JS 一律用 Base64 传输后由 atob 解码，
 *       避免多行字符串在 Kotlin 字符串模板里产生转义错误
 *       （曾导致注入脚本抛 Uncaught SyntaxError 而静默失效）。
 */
private const val OVERLAY_FIX_CSS_B64 =
    "LnYtb3ZlcmxheV9fY29udGVudCB7IG1heC1oZWlnaHQ6IDEwMCUgIWltcG9y" +
    "dGFudDsgfQoudi1vdmVybGF5X19jb250ZW50ID4gKiB7IG1heC1oZWlnaHQ6" +
    "IDk2JSAhaW1wb3J0YW50OyB9"

private fun injectOverlayFix(view: WebView?) {
    view ?: return
    runCatching {
        val js = """
            (function () {
              try {
                if (!document.getElementById('mp-overlay-fix')) {
                  var css = atob('$OVERLAY_FIX_CSS_B64');
                  var st = document.createElement('style');
                  st.id = 'mp-overlay-fix';
                  st.textContent = css;
                  (document.head || document.documentElement).appendChild(st);
                }
                if (!window.__mpObs && window.MutationObserver) {
                  window.__mpObs = new MutationObserver(function () {
                    var ov = document.querySelector('.v-overlay');
                    if (!ov) return;
                    var ct = document.querySelector('.v-overlay__content');
                    var r = ov.getBoundingClientRect();
                    var m = 'OV top=' + Math.round(r.top) + ' h=' + Math.round(r.height)
                          + ' vw=' + window.innerWidth + ' vh=' + window.innerHeight
                          + ' dpr=' + window.devicePixelRatio
                          + ' vvScale=' + (window.visualViewport ? window.visualViewport.scale : -1)
                          + ' vvH=' + (window.visualViewport ? Math.round(window.visualViewport.height) : -1);
                    if (ct) {
                      var r2 = ct.getBoundingClientRect();
                      var cs3 = getComputedStyle(ct);
                      m += ' | CT ' + Math.round(r2.top) + ',' + Math.round(r2.height)
                         + 'x' + Math.round(r2.width)
                         + ' disp=' + cs3.display + ' vis=' + cs3.visibility
                         + ' op=' + cs3.opacity + ' kids=' + ct.children.length
                         + ' html=' + ct.innerHTML.length;
                      for (var i = 0; i < ct.children.length && i < 3; i++) {
                        var k = ct.children[i];
                        var kr = k.getBoundingClientRect();
                        var kcs = getComputedStyle(k);
                        m += ' ||K' + i + ' ' + k.tagName + '.' + (k.className || '').slice(0, 60)
                           + ' r=' + Math.round(kr.top) + ',' + Math.round(kr.height) + 'x' + Math.round(kr.width)
                           + ' disp=' + kcs.display + ' op=' + kcs.opacity;
                      }
                    } else { m += ' | CT=NULL'; }
                    if (window.AndroidBridge && AndroidBridge.log) { AndroidBridge.log(m); }
                  });
                  window.__mpObs.observe(document.body, { childList: true, subtree: true });
                }
              } catch (e) {
                if (window.AndroidBridge && AndroidBridge.log) {
                  AndroidBridge.log('INJECT_ERR ' + e.message);
                }
              }
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }
}

/**
 * 诊断：在 WebView 上下文里实测 PWA 的 API 请求
 *
 * 现象：浏览器中「下载管理 / 媒体整理 / 文件管理」均正常，
 *       但 App 内点击后为空。
 * 需要确认：页面内 fetch 是否被拒（Cookie / CSRF / 相对路径解析）。
 */
private fun injectApiProbe(view: WebView?) {
    view ?: return
    runCatching {
        val js = """
            (function () {
              if (window.__mpProbe) return;
              window.__mpProbe = 1;
              var lsKeys = [];
              try {
                for (var i = 0; i < localStorage.length; i++) { lsKeys.push(localStorage.key(i)); }
              } catch (e) {}
              var authVal = '';
              try { authVal = (localStorage.getItem('auth') || 'NULL').slice(0, 120); } catch (e) {}
              var out = 'LOC=' + location.href
                + ' cookie=' + (document.cookie || 'EMPTY')
                + ' LS_KEYS=' + lsKeys.join(',')
                + ' AUTH=' + authVal;
              if (window.AndroidBridge && AndroidBridge.log) AndroidBridge.log(out);
              var tk = '';
              try {
                var a = JSON.parse(localStorage.getItem('auth') || '{}');
                tk = a.token || '';
              } catch (e) {}
              var log2 = 'AUTH_LEN=' + tk.length;
              if (window.AndroidBridge && AndroidBridge.log) AndroidBridge.log(log2);
              var paths = ['/api/v1/download/', '/api/v1/history/transfer'];
              paths.forEach(function (p) {
                fetch(p, {
                  credentials: 'include',
                  headers: { 'Accept': 'application/json', 'Authorization': 'Bearer ' + tk }
                })
                  .then(function (r) {
                    return r.text().then(function (t) {
                      var m = 'PROBE ' + p + ' -> ' + r.status + ' len=' + t.length
                            + ' body=' + t.slice(0, 100).replace(/\n/g, ' ');
                      if (window.AndroidBridge && AndroidBridge.log) AndroidBridge.log(m);
                    });
                  })
                  .catch(function (e) {
                    if (window.AndroidBridge && AndroidBridge.log) {
                      AndroidBridge.log('PROBE ' + p + ' -> ERR ' + e.message);
                    }
                  });
              });
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }
}

/**
 * JavaScript 接口，用于向 WebView 提供 Android 端保存的凭证
 */
class WebAppInterface {
    /**
     * 接收 PWA 侧诊断日志，转发到 logcat。
     * 弹层（v-overlay）定位异常时，用它把实际几何数据带出来。
     */
    @android.webkit.JavascriptInterface
    fun log(msg: String?) {
        android.util.Log.i("MPDIAG", msg ?: "")
    }

    @android.webkit.JavascriptInterface
    fun getUsername(): String = ApiClient.getCredentials()?.first ?: ""

    @android.webkit.JavascriptInterface
    fun getPassword(): String = ApiClient.getCredentials()?.second ?: ""
}

/**
 * 尝试自动登录：如果检测到登录页且有保存的凭证，则自动填充并提交
 */

/**
 * 把 App 已登录的 token 注入服务器 PWA
 *
 * 背景：服务器 PWA 用 Pinia store `auth` 持久化登录 token（localStorage），
 *       所有业务请求都带 `Authorization: Bearer <token>`。
 *       App 自己的登录走 `POST /api/v2/login/access-token`，token 只存在
 *       App 内存/加密存储里，WebView 内的 PWA 拿不到，
 *       于是页面接口全部 401 —— 表现为「下载管理 / 媒体整理 / 文件管理」
 *       在浏览器里正常，在 App 里空白。
 *
 * 做法：App 登录成功后，把 token 直接写进 PWA 的 localStorage 并触发
 *       hashchange 让其重新初始化，省去模拟填写登录表单（脆弱且易失效）。
 */
private fun injectAuthToken(view: WebView?) {
    val token = ApiClient.getAuthToken()
    if (view == null || token.isNullOrBlank()) return
    runCatching {
        // 服务器 PWA 用 pinia-persist 持久化登录态：persist:true 时
        // 存储键就是 store 的 $id（此处为 "auth"），值为整个 state 的 JSON。
        // 写入后需让页面重载一次，store 才会以新 token 完成 hydrate。
        val js = """
            (function () {
              var t = ${jsonString(token)};
              try {
                localStorage.setItem('auth', JSON.stringify({
                  token: t, remember: true, originalPath: null
                }));
                if (!sessionStorage.getItem('__mp_auth_done')) {
                  sessionStorage.setItem('__mp_auth_done', '1');
                  setTimeout(function () { location.reload(); }, 50);
                }
              } catch (e) {
                if (window.AndroidBridge && AndroidBridge.log) {
                  AndroidBridge.log('AUTH_ERR ' + e.message);
                }
              }
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }
}

/** 把字符串安全编码为 JS 字面量 */
private fun jsonString(raw: String): String {
    val sb = StringBuilder("\"")
    for (ch in raw) {
        when {
            ch == '\\' -> sb.append("\\\\")
            ch == '"' -> sb.append("\\\"")
            ch.code < 0x20 -> sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
            else -> sb.append(ch)
        }
    }
    return sb.append("\"").toString()
}
