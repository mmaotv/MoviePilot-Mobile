package com.moviepilot.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.JsonParser
import com.moviepilot.app.data.network.LocalAssetInterceptor
import com.moviepilot.app.ui.theme.MpBackground
import com.moviepilot.app.ui.theme.MpIsDark
import com.moviepilot.app.ui.theme.MpPrimary
import com.moviepilot.app.ui.theme.MpSurface
import com.moviepilot.app.ui.theme.MpTextPrimary
import com.moviepilot.app.ui.theme.MpTextSecondary
import com.moviepilot.app.ui.theme.setSystemBarIcons
import com.moviepilot.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 主界面 = 服务器网页版，全屏显示，无原生顶栏、无导航入口。
 * 唯一的原生控件是悬浮球，点开只有「切换服务器 / 切换账号 / 退出登录」。
 *
 * 布局上永远避让系统栏（内容不会被状态栏压住），
 * 状态栏/导航栏预留出来的那条区域用网页自己的颜色填充并同步图标明暗，
 * 看起来仍然是通栏，但不会出现内容与状态栏重叠。
 */
@Composable
fun PwaScreen(
    state: AuthViewModel.UiState,
    viewModel: AuthViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity
    val appDark = MpIsDark
    val density = LocalDensity.current

    // 网页顶栏 / 页面底色，由网页里的脚本回报
    var pageTheme by remember(state.sessionEpoch) { mutableStateOf<PageTheme?>(null) }

    val statusBarInset = WindowInsets.statusBars.getTop(density)
    val navigationBarInset = WindowInsets.navigationBars.getBottom(density)

    val statusBarColor = pageTheme?.topColor?.let { Color(it) } ?: MpBackground
    val navigationBarColor = pageTheme?.bodyColor?.let { Color(it) } ?: MpBackground

    // 状态栏图标的明暗：网页顶栏浅色就用深色图标，反之用浅色图标。
    // 网页还没回报时先跟 App 主题走，避免冷启动瞬间出现白底白字。
    val lightStatusIcons = pageTheme?.let { !it.topDark } ?: !appDark
    val lightNavigationIcons = pageTheme?.let { !it.bodyDark } ?: !appDark

    LaunchedEffect(lightStatusIcons, lightNavigationIcons) {
        activity ?: return@LaunchedEffect
        setSystemBarIcons(
            activity = activity,
            lightStatusBar = lightStatusIcons,
            lightNavigationBar = lightNavigationIcons
        )
    }

    var hasError by remember(state.sessionEpoch) { mutableStateOf(false) }
    var loading by remember(state.sessionEpoch) { mutableStateOf(true) }
    var firstLoad by remember(state.sessionEpoch) { mutableStateOf(true) }
    val webRef = remember(state.sessionEpoch) { mutableStateOf<WebView?>(null) }

    // 自建服务器常见自签名证书：拦下来交给用户确认，而不是直接白屏
    var sslErrorHandler by remember(state.sessionEpoch) {
        mutableStateOf<android.webkit.SslErrorHandler?>(null)
    }

    // 网页版里的「上传头像 / 选择文件」用它接住
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pendingFileCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        pendingFileCallback = null
    }

    // ── 悬浮球位置 ───────────────────────────────────────────────
    var fabX by remember { mutableStateOf(0.87f) }
    var fabY by remember { mutableStateOf(0.78f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        val saved = viewModel.fabPosition()
        if (saved != null) {
            fabX = saved.first
            fabY = saved.second
        }
    }

    // ── 返回键 ──────────────────────────────────────────────────
    var exitHint by remember { mutableStateOf(false) }
    BackHandler {
        when {
            state.panelVisible -> viewModel.setPanelVisible(false)
            webRef.value?.canGoBack() == true -> webRef.value?.goBack()
            exitHint -> activity?.finish()
            else -> {
                exitHint = true
                Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
                scope.launch {
                    delay(2000)
                    exitHint = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MpBackground)
    ) {
        key(state.sessionEpoch) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    // 永远避让系统栏：无论网页怎么排版，内容都不会被状态栏压住
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .consumeWindowInsets(WindowInsets.systemBars)
                    .imePadding(),
                factory = { ctx ->
                    buildWebView(
                        context = ctx,
                        startUrl = state.serverUrl,
                        token = state.token,
                        userSeed = state.webUserSeed,
                        backgroundColor = if (appDark) 0xFF0E1116.toInt() else 0xFFF4F5FA.toInt(),
                        onLoading = { isLoading ->
                            loading = isLoading
                            if (isLoading) hasError = false else firstLoad = false
                        },
                        onError = { hasError = true; loading = false },
                        onPageTheme = { theme -> pageTheme = theme },
                        onSslError = { handler -> sslErrorHandler = handler },
                        onPickFile = { callback, mime ->
                            pendingFileCallback?.onReceiveValue(null)
                            pendingFileCallback = callback
                            filePicker.launch(mime)
                        }
                    ).also { webRef.value = it }
                }
            )
        }

        // 首屏加载指示（纯色底 + 转圈，无动画组件）
        if (loading && firstLoad && !hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MpBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MpPrimary, strokeWidth = 2.dp)
            }
        }

        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MpBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("无法打开网页版", color = MpTextPrimary, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.serverUrl,
                        color = MpTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(18.dp))
                    Surface(
                        shape = CircleShape,
                        color = MpPrimary
                    ) {
                        Text(
                            text = "重新加载",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    hasError = false
                                    loading = true
                                    webRef.value?.reload()
                                }
                                .padding(horizontal = 22.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // ── 系统栏色块：把网页顶栏/底色铺到状态栏与导航栏后面 ──────────
        if (statusBarInset > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { statusBarInset.toDp() })
                    .background(statusBarColor)
                    .align(Alignment.TopCenter)
            )
        }
        if (navigationBarInset > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { navigationBarInset.toDp() })
                    .background(navigationBarColor)
                    .align(Alignment.BottomCenter)
            )
        }

        // ── 悬浮球 ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { containerSize = it.size }
        ) {
            if (containerSize != IntSize.Zero) {
                val fabSizePx = with(density) { 48.dp.toPx() }
                val posX = (fabX * containerSize.width - fabSizePx / 2)
                    .coerceIn(0f, containerSize.width - fabSizePx)
                val posY = (fabY * containerSize.height - fabSizePx / 2)
                    .coerceIn(0f, containerSize.height - fabSizePx)

                Surface(
                    modifier = Modifier
                        .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
                        .size(48.dp)
                        .pointerInput(containerSize) {
                            detectDragGestures(
                                onDragEnd = { viewModel.saveFabPosition(fabX, fabY) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    fabX = (fabX + dragAmount.x / containerSize.width)
                                        .coerceIn(0.04f, 0.96f)
                                    fabY = (fabY + dragAmount.y / containerSize.height)
                                        .coerceIn(0.04f, 0.96f)
                                }
                            )
                        },
                    shape = CircleShape,
                    color = MpPrimary.copy(alpha = 0.92f),
                    shadowElevation = 6.dp
                ) {
                    IconButton(
                        onClick = { viewModel.setPanelVisible(true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "切换服务器或账号",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        sslErrorHandler?.let { handler ->
            AlertDialog(
                onDismissRequest = {
                    handler.cancel()
                    sslErrorHandler = null
                },
                title = { Text("HTTPS 证书有问题", color = MpTextPrimary, fontSize = 16.sp) },
                text = {
                    Text(
                        text = "服务器证书校验没过，通常是自签名证书。\n只有在你确认这台服务器是自己搭的、可信的情况下才继续。",
                        color = MpTextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        handler.proceed()
                        sslErrorHandler = null
                    }) {
                        Text("继续访问", color = MpPrimary, fontSize = 14.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        handler.cancel()
                        sslErrorHandler = null
                    }) {
                        Text("取消", color = MpTextSecondary, fontSize = 14.sp)
                    }
                },
                containerColor = MpSurface
            )
        }

        if (state.panelVisible) {
            SwitchPanel(
                state = state,
                viewModel = viewModel,
                onDismiss = { viewModel.setPanelVisible(false) }
            )
        }
    }
}

// ── 网页主题上报 ─────────────────────────────────────────────────

/**
 * 网页顶部与底部的实际颜色。
 *
 * 状态栏那条区域用 [topColor] 填充、图标按 [topDark] 取反，
 * 这样不管网页是浅色还是深色、也不管用户怎么切主题，都不会出现
 * 「深色背景配白色图标」「浅色背景配白色图标」这类看不清的情况。
 */
private data class PageTheme(
    val topColor: Int,
    val topDark: Boolean,
    val bodyColor: Int,
    val bodyDark: Boolean
)

private const val THEME_PREFIX = "MP_THEME:"

// ── WebView 构建 ─────────────────────────────────────────────────

/**
 * 伪装成 iOS Safari。
 *
 * 服务器前端的移动端布局与 PWA 行为对 iOS Safari 的支持最完整
 * （V3 的 index.html 里连 apple-touch-startup-image 都做了），
 * 用 iOS UA 能拿到最贴近设计稿的渲染结果。
 */
private const val IOS_SAFARI_UA =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) " +
        "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"

@SuppressLint("SetJavaScriptEnabled")
private fun buildWebView(
    context: Context,
    startUrl: String,
    token: String,
    userSeed: String,
    backgroundColor: Int,
    onLoading: (Boolean) -> Unit,
    onError: () -> Unit,
    onPageTheme: (PageTheme) -> Unit,
    onSslError: (android.webkit.SslErrorHandler) -> Unit,
    onPickFile: (ValueCallback<Array<Uri>>, String) -> Unit
): WebView {
    return WebView(context).apply {
        setBackgroundColor(backgroundColor)

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = IOS_SAFARI_UA
            allowFileAccess = false
            allowContentAccess = false
        }

        isVerticalScrollBarEnabled = false
        overScrollMode = WebView.OVER_SCROLL_NEVER
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

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (request != null && LocalAssetInterceptor.shouldIntercept(request)) {
                    LocalAssetInterceptor.intercept(context, request)?.let { return it }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                onLoading(true)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                onLoading(false)
                view ?: return
                injectOverlayFix(view)
                injectThemeWatcher(view)
                if (token.isNotBlank()) injectAuthToken(view, token, userSeed)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    val code = error?.errorCode ?: -1
                    if (code == ERROR_CONNECT || code == ERROR_HOST_LOOKUP ||
                        code == ERROR_TIMEOUT || code == ERROR_BAD_URL
                    ) {
                        onError()
                    }
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                if (request?.isForMainFrame == true &&
                    (errorResponse?.statusCode ?: 0) in 500..599
                ) {
                    onError()
                }
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView?,
                handler: android.webkit.SslErrorHandler?,
                error: android.net.http.SslError?
            ) {
                if (handler == null) {
                    super.onReceivedSslError(view, handler, error)
                    return
                }
                // 交给界面弹窗确认，确认后 proceed
                onSslError(handler)
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: android.webkit.RenderProcessGoneDetail?
            ): Boolean {
                onError()
                return true
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                if (filePathCallback == null) return false
                val accept = fileChooserParams?.acceptTypes?.firstOrNull { it.isNotBlank() }
                val mime = if (accept != null && accept.contains("/")) accept else "*/*"
                onPickFile(filePathCallback, mime)
                return true
            }

            /**
             * 只关心网页主题上报，其余网页日志不往 logcat 里倒。
             */
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                val text = message.message() ?: return true
                if (text.startsWith(THEME_PREFIX)) {
                    parseTheme(text.removePrefix(THEME_PREFIX))?.let(onPageTheme)
                }
                return true
            }
        }

        loadUrl(startUrl)
    }
}

private fun parseTheme(json: String): PageTheme? = runCatching {
    val o = JsonParser.parseString(json).asJsonObject
    PageTheme(
        topColor = AndroidColor.parseColor(o.get("top").asString),
        topDark = o.get("topDark").asBoolean,
        bodyColor = AndroidColor.parseColor(o.get("body").asString),
        bodyDark = o.get("body").asBoolean
    )
}.getOrNull()

/**
 * 把 App 的登录态写进网页版 localStorage，然后重新以站点根地址加载一次。
 *
 * 两个必须同时写的 store：
 *  - `auth`：{ token, remember, originalPath }，网页版据此认为已登录
 *  - `user`：{ superUser, userID, userName, avatar, level, permissions }
 *
 * 只写 `auth` 的话，网页版根路由 `/` 看到 superUser 还是 false，
 * 会把超管也重定向到「应用中心 /apps」——表现就是只有订阅等少数入口、没有主页。
 * 另外网页版是 hash 路由（`#/login`、`#/dashboard`），注入后必须整体重载，
 * 否则 Pinia store 不会重新 hydrate，还会停在 `#/login` 上。
 */
private fun injectAuthToken(view: WebView, token: String, userSeed: String) {
    // 用 JSON.parse 而不是直接拼对象字面量：用户名/avatar 里可能出现
    // 会破坏 JS 字面量的字符，包成字符串再解析最稳。
    val seedInit = if (userSeed.isBlank()) "null" else "JSON.parse(${jsString(userSeed)})"
    val js = """
        (function () {
          try {
            var token = ${jsString(token)};
            var seed = $seedInit;
            var justReloaded = location.search.indexOf('__mp_r=1') >= 0;

            // 清掉我们自己加的临时参数，地址栏保持干净
            if (justReloaded) {
              try { history.replaceState(null, '', location.pathname + location.hash); } catch (e) {}
            }

            function mergeUser() {
              if (!seed) return;
              try {
                var cur = null;
                try { var raw = localStorage.getItem('user'); if (raw) cur = JSON.parse(raw); } catch (e) {}
                var next = (cur && typeof cur === 'object') ? cur : {};
                for (var k in seed) {
                  if (Object.prototype.hasOwnProperty.call(seed, k)) next[k] = seed[k];
                }
                localStorage.setItem('user', JSON.stringify(next));
              } catch (e) {}
            }

            function rootPath() {
              try {
                var b = document.querySelector('base');
                if (b && b.getAttribute('href')) {
                  return new URL(b.getAttribute('href'), location.href).pathname;
                }
              } catch (e) {}
              var p = location.pathname || '/';
              p = p.replace(/\/(login|initialize|service-status)\/?${'$'}/i, '/');
              if (p.charAt(p.length - 1) !== '/') p = p.substring(0, p.lastIndexOf('/') + 1);
              return p || '/';
            }

            // 超管却落在「应用中心」时纠正一次，之后用户自己点「应用中心」不再干预
            function ensureHome() {
              if (!seed || !seed.superUser || window.__mpHomeFixed) return;
              setTimeout(function () {
                try {
                  if (location.hash.indexOf('#/apps') === 0) {
                    window.__mpHomeFixed = true;
                    location.hash = '#/dashboard';
                  }
                } catch (e) {}
              }, 800);
            }

            var current = null;
            try {
              var rawAuth = localStorage.getItem('auth');
              if (rawAuth) current = (JSON.parse(rawAuth) || {}).token || null;
            } catch (e) {}

            mergeUser();

            if (current === token) {
              ensureHome();
              return;
            }

            localStorage.setItem('auth', JSON.stringify({
              token: token, remember: true, originalPath: null
            }));

            if (!justReloaded) {
              var base = rootPath();
              setTimeout(function () {
                location.replace(base + '?__mp_r=1#/');
              }, 20);
            }
          } catch (e) {}
        })();
    """.trimIndent()
    runCatching { view.evaluateJavascript(js, null) }
}

/**
 * 修正 Vuetify 弹层在 WebView 中高度被算成 0 的问题。
 * v2 / v3 都还在用 .v-overlay__content。
 */
private fun injectOverlayFix(view: WebView) {
    val js = """
        (function () {
          try {
            if (document.getElementById('mp-overlay-fix')) return;
            var st = document.createElement('style');
            st.id = 'mp-overlay-fix';
            st.textContent = '.v-overlay__content{max-height:100%!important;}' +
                             '.v-overlay__content>*{max-height:96%!important;}';
            (document.head || document.documentElement).appendChild(st);
          } catch (e) {}
        })();
    """.trimIndent()
    runCatching { view.evaluateJavascript(js, null) }
}

/**
 * 持续回报网页顶部（顶栏）与底部的实际背景色。
 *
 * 用 `console.log` 而不是 `addJavascriptInterface`：服务器地址是用户自己填的，
 * 万一连到不信任的站点，JS 桥会成为一个可被利用的入口；console 只单向传数据。
 */
private fun injectThemeWatcher(view: WebView) {
    val js = """
        (function () {
          if (window.__mpThemeTimer) return;
          var last = '';

          function rgbOf(str) {
            var m = (str || '').match(/rgba?\(\s*([\d.]+)[,\s]+([\d.]+)[,\s]+([\d.]+)(?:[,\s\/]+([\d.]+))?/);
            if (!m) return null;
            var a = (m[4] === undefined) ? 1 : parseFloat(m[4]);
            if (a <= 0.5) return null;
            return { r: parseInt(m[1], 10), g: parseInt(m[2], 10), b: parseInt(m[3], 10) };
          }
          function hexOf(c) {
            return '#' + ((1 << 24) + (c.r << 16) + (c.g << 8) + c.b).toString(16).slice(1);
          }
          function lumOf(c) { return (c.r * 299 + c.g * 587 + c.b * 114) / 1000; }

          function topColor() {
            var el = document.elementFromPoint(Math.floor(window.innerWidth / 2), 2);
            // 有弹层/菜单盖住顶部时，取真正的顶栏，避免状态栏跟着变暗
            if (el && el.closest && el.closest('.v-overlay-container')) {
              el = document.querySelector('.v-app-bar, .v-toolbar, header') || document.body;
            }
            for (var i = 0; el && i < 14; i++) {
              var c = rgbOf(getComputedStyle(el).backgroundColor);
              if (c) return c;
              el = el.parentElement;
            }
            return bodyColor();
          }
          function bodyColor() {
            return rgbOf(getComputedStyle(document.body).backgroundColor)
                || rgbOf(getComputedStyle(document.documentElement).backgroundColor)
                || { r: 244, g: 245, b: 250 };
          }

          function report() {
            try {
              var t = topColor(), b = bodyColor();
              var sig = hexOf(t) + '|' + hexOf(b);
              if (sig === last) return;
              last = sig;
              console.log('$THEME_PREFIX' + JSON.stringify({
                top: hexOf(t), topDark: lumOf(t) < 128,
                body: hexOf(b), bodyDark: lumOf(b) < 128
              }));
            } catch (e) {}
          }

          report();
          window.__mpThemeTimer = setInterval(report, 600);
        })();
    """.trimIndent()
    runCatching { view.evaluateJavascript(js, null) }
}

/** 转义为 JS 字符串字面量 */
private fun jsString(raw: String): String {
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
