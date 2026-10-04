package com.moviepilot.app.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moviepilot.app.data.local.PreferencesManager
import com.moviepilot.app.ui.screens.*
import com.moviepilot.app.ui.theme.ThemeManager
import com.moviepilot.app.ui.viewmodel.AuthViewModel
import com.moviepilot.app.ui.viewmodel.PreferencesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGestures
import kotlin.math.roundToInt

// Navigation drawer item
data class NavDrawerItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val category: String = "",
    val path: String = ""   // PWA 路径，如 /#/dashboard
)

object NavRoutes {
    // ── 侧边栏入口（全部映射到服务器 PWA 页面）────────────────────
    const val DASHBOARD        = "dashboard"
    const val RECOMMEND        = "recommend"
    const val DISCOVER         = "discover"
    const val RESOURCE         = "resource"
    const val SUBSCRIBE_MOVIE  = "subscribe_movie"
    const val SUBSCRIBE_TV     = "subscribe_tv"
    const val CALENDAR         = "calendar"
    const val DOWNLOADING      = "downloading"
    const val HISTORY          = "history"
    const val FILE_MANAGER     = "file_manager"
    const val PLUGINS          = "plugins"
    const val APPS             = "apps"
    const val SITE             = "site"
    const val USER_MANAGEMENT  = "user_management"
    const val SETTINGS         = "settings_pwa"

    // ── 原生页面（仅用于 PWA 无法覆盖的详情页）───────────────────
    const val RANKING          = "ranking"

    // Media detail (原生)
    const val MEDIA_DETAIL = "media_detail/{mediaType}/{mediaId}"
    fun mediaDetail(mediaType: String, mediaId: String) = "media_detail/$mediaType/$mediaId"
}

// 原生页面路由（需要显示 TopAppBar 的页面）
private val nativeRoutes = setOf(
    NavRoutes.MEDIA_DETAIL.substringBefore("{")
)

@Composable
fun AppNavigation(
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel(),
    themeManager: ThemeManager? = null,
    preferencesManager: PreferencesManager? = null
) {
    var loggedIn by remember { mutableStateOf(isLoggedIn) }

    LaunchedEffect(isLoggedIn) { loggedIn = isLoggedIn }

    val switchServer: () -> Unit = {
        authViewModel.logout()
        loggedIn = false
    }

    val handleConnectionLost: () -> Unit = {
        authViewModel.handleConnectionLost("无法连接到服务器，请检查网络或服务器地址")
        loggedIn = false
    }

    if (!loggedIn) {
        LoginScreen(
            onLoginSuccess = {
                loggedIn = true
                authViewModel.loadCurrentUser()
            },
            viewModel = authViewModel
        )
    } else {
        MainScreen(
            navController = navController,
            onLogout = {
                loggedIn = false
                onLogout()
            },
            onSwitchServer = switchServer,
            onConnectionLost = handleConnectionLost,
            themeManager = themeManager,
            preferencesManager = preferencesManager
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    onLogout: () -> Unit,
    onSwitchServer: () -> Unit = {},
    onConnectionLost: () -> Unit = {},
    themeManager: ThemeManager? = null,
    // 注意：PreferencesManager 是 @Singleton，不能用 hiltViewModel() 获取
    // （那会尝试无参构造并抛 NoSuchMethodException，导致登录成功后必闪退）。
    // 由 MainActivity 通过 @Inject 注入后传入；为 null 时仅跳过悬浮按钮位置持久化。
    preferencesManager: PreferencesManager? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // ── 悬浮按钮位置状态 ─────────────────────────────────────────
    var fabOffsetX by remember { mutableStateOf(0f) }
    var fabOffsetY by remember { mutableStateOf(0f) }
    var isFabCustomized by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    // density 在 Composition 时获取，避免在条件块内访问 LocalDensity
    val fabDensity = LocalDensity.current

    // 启动时读取保存的位置
    LaunchedEffect(Unit) {
        val savedPos = preferencesManager?.getFabPosition()
        if (savedPos != null) {
            isFabCustomized = true
            fabOffsetX = savedPos.first
            fabOffsetY = savedPos.second
        } else {
            // 默认位置
            fabOffsetX = 0.85f
            fabOffsetY = 0.85f
        }
    }

    // ── 共享 WebView 实例 ──────────────────────────────────────────
    val (sharedWebView, loadPath, reloadPath) = rememberWebView(
        onConnectionLost = onConnectionLost
    )

    // 当前选中的菜单路由
    var selectedRoute by remember { mutableStateOf(NavRoutes.DASHBOARD) }

    // 侧边栏 = 服务器 PWA 页面的薄入口层
    //
    // 设计原则：主体界面一律由服务器 PWA 渲染，侧边栏只负责跳转到
    // 服务器实际存在的页面。这样服务器前端升级时，手机端无需重新适配，
    // 也不会出现「原生页 / PWA 页 / 网页」三套渲染逻辑混用导致的
    // 操作体验割裂。
    //
    // 入口清单对齐 MoviePilot v3.1.0 的实际路由（从服务端前端产物核对得出）。
    val navItems = listOf(
        NavDrawerItem(NavRoutes.DASHBOARD,       "仪表盘",     Icons.Filled.Home,          "首页", "/#/dashboard"),
        NavDrawerItem(NavRoutes.RECOMMEND,       "推荐",       Icons.Filled.Star,          "首页", "/#/recommend"),
        NavDrawerItem(NavRoutes.DISCOVER,        "发现",       Icons.Filled.Explore,       "首页", "/#/discover"),
        NavDrawerItem(NavRoutes.RESOURCE,        "资源搜索",   Icons.Filled.Search,        "媒体", "/#/resource"),
        NavDrawerItem(NavRoutes.SUBSCRIBE_MOVIE, "电影订阅",   Icons.Filled.PlayArrow,     "媒体", "/#/subscribe/movie"),
        NavDrawerItem(NavRoutes.SUBSCRIBE_TV,    "剧集订阅",   Icons.Filled.Subscriptions, "媒体", "/#/subscribe/tv"),
        NavDrawerItem(NavRoutes.CALENDAR,        "Bangumi日历", Icons.Filled.DateRange,    "媒体", "/#/calendar"),
        NavDrawerItem(NavRoutes.DOWNLOADING,     "下载管理",   Icons.Filled.ArrowForward,   "管理", "/#/downloading"),
        NavDrawerItem(NavRoutes.HISTORY,         "媒体整理",   Icons.Filled.History,       "管理", "/#/history"),
        NavDrawerItem(NavRoutes.FILE_MANAGER,    "文件管理",   Icons.Filled.Folder,        "管理", "/#/filemanager"),
        NavDrawerItem(NavRoutes.PLUGINS,         "插件管理",   Icons.Filled.Extension,     "系统", "/#/plugins"),
        NavDrawerItem(NavRoutes.APPS,            "应用中心",   Icons.Filled.Apps,          "系统", "/#/apps"),
        NavDrawerItem(NavRoutes.SITE,            "站点管理",   Icons.Filled.Language,      "系统", "/#/site"),
        NavDrawerItem(NavRoutes.USER_MANAGEMENT, "用户管理",   Icons.Filled.People,        "系统", "/#/user"),
        NavDrawerItem(NavRoutes.SETTINGS,        "系统设置",   Icons.Filled.Settings,      "系统", "/#/setting")
    )

    // 路由 → PWA 路径映射（与 navItems 一一对应）
    val routeToPath = remember {
        mapOf(
            NavRoutes.DASHBOARD to "/#/dashboard",
            NavRoutes.RECOMMEND to "/#/recommend",
            NavRoutes.DISCOVER to "/#/discover",
            NavRoutes.RESOURCE to "/#/resource",
            NavRoutes.SUBSCRIBE_MOVIE to "/#/subscribe/movie",
            NavRoutes.SUBSCRIBE_TV to "/#/subscribe/tv",
            NavRoutes.CALENDAR to "/#/calendar",
            NavRoutes.DOWNLOADING to "/#/downloading",
            NavRoutes.HISTORY to "/#/history",
            NavRoutes.FILE_MANAGER to "/#/filemanager",
            NavRoutes.PLUGINS to "/#/plugins",
            NavRoutes.APPS to "/#/apps",
            NavRoutes.SITE to "/#/site",
            NavRoutes.USER_MANAGEMENT to "/#/user",
            NavRoutes.SETTINGS to "/#/setting"
        )
    }

    val currentTitle = when {
        currentRoute?.startsWith("media_detail") == true -> "媒体详情"
        else -> navItems.find { it.route == selectedRoute }?.title ?: "MoviePilot"
    }

    // 判断当前是否为 PWA WebView 页面
    val isWebViewRoute = selectedRoute in routeToPath

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor    = MaterialTheme.colorScheme.surface
    val accentColor     = MaterialTheme.colorScheme.primary
    val textPrimary     = MaterialTheme.colorScheme.onSurface
    val textSecondary   = MaterialTheme.colorScheme.onSurfaceVariant

    // 返回键状态：是否显示"再按一次退出"提示
    var showExitHint by remember { mutableStateOf(false) }

    // 获取 Activity 用于退出应用
    val activity = androidx.compose.ui.platform.LocalContext.current as? Activity

    // 返回键处理
    BackHandler {
        when {
            // 不在仪表盘 → 返回仪表盘
            selectedRoute != NavRoutes.DASHBOARD -> {
                selectedRoute = NavRoutes.DASHBOARD
                loadPath("/#/dashboard")
            }
            // 在仪表盘且已显示提示 → 退出应用
            showExitHint -> {
                // 真正退出应用
                activity?.finish()
            }
            // 在仪表盘但未显示提示 → 显示提示，2秒后重置
            else -> {
                showExitHint = true
                scope.launch {
                    delay(2000)
                    showExitHint = false
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = surfaceColor,
                drawerContentColor = textPrimary
            ) {
                // ——— Header ———
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Logo",
                                    tint = accentColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "MoviePilot",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "媒体库自动化管理",
                                fontSize = 11.sp,
                                color = textSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Divider(color = textSecondary.copy(alpha = 0.08f))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    var lastCategory = ""
                    items(navItems) { item ->
                        if (item.category != lastCategory && item.category.isNotEmpty()) {
                            lastCategory = item.category
                            if (lastCategory != "首页") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = textSecondary.copy(alpha = 0.06f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.category.uppercase(),
                                fontSize = 10.sp,
                                color = textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }

                        val selected = selectedRoute == item.route
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (selected) accentColor else textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 14.sp,
                                    color = if (selected) accentColor else textPrimary.copy(alpha = 0.85f),
                                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
                                // 共享 WebView：只改 URL，不重建实例 → 瞬间切换，无白屏
                                selectedRoute = item.route
                                loadPath(item.path)
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = accentColor.copy(alpha = 0.12f),
                                unselectedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp)
                        )
                    }
                }

                // ——— Footer ———
                Divider(color = textSecondary.copy(alpha = 0.08f))

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "切换服务器",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "切换服务器",
                            fontSize = 14.sp,
                            color = textPrimary.copy(alpha = 0.85f)
                        )
                    },
                    selected = false,
                    onClick = onSwitchServer,
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "退出",
                            tint = Color(0xFFe57373).copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "退出登录",
                            fontSize = 14.sp,
                            color = Color(0xFFe57373).copy(alpha = 0.95f)
                        )
                    },
                    selected = false,
                    onClick = onLogout,
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    ) {
        // Toast 提示显示"再按一次退出"
        val context = androidx.compose.ui.platform.LocalContext.current
        LaunchedEffect(showExitHint) {
            if (showExitHint) {
                android.widget.Toast.makeText(
                    context,
                    "再按一次返回仪表盘",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        Scaffold(
            topBar = {
                // 极简顶栏，高度 48dp，无按钮无标题，只保留阴影
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    // 双击顶栏打开侧边栏
                                    scope.launch { drawerState.open() }
                                }
                            )
                        },
                    color = backgroundColor,
                    shadowElevation = 1.dp
                ) {}
            },
            containerColor = backgroundColor
        ) { paddingValues ->
            var dragStartX = 0f
            var isDraggingFromLeftEdge = false
            var isDraggingFromRightEdge = false

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(drawerState) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                dragStartX = offset.x
                                // 检测是从左边缘还是右边缘开始滑动
                                isDraggingFromLeftEdge = offset.x < size.width * 0.15f
                                isDraggingFromRightEdge = offset.x > size.width * 0.85f
                            },
                            onDragEnd = {
                                isDraggingFromLeftEdge = false
                                isDraggingFromRightEdge = false
                            },
                            onDragCancel = {
                                isDraggingFromLeftEdge = false
                                isDraggingFromRightEdge = false
                            },
                            onHorizontalDrag = { change, _ ->
                                // 从左边缘向右滑 → 打开侧边栏
                                if (isDraggingFromLeftEdge && drawerState.isClosed) {
                                    val totalDrag = change.position.x - dragStartX
                                    if (totalDrag > 60f) {
                                        isDraggingFromLeftEdge = false
                                        scope.launch { drawerState.open() }
                                    }
                                }
                                // 从右边缘向左滑 → 打开侧边栏
                                if (isDraggingFromRightEdge && drawerState.isClosed) {
                                    val totalDrag = dragStartX - change.position.x
                                    if (totalDrag > 60f) {
                                        isDraggingFromRightEdge = false
                                        scope.launch { drawerState.open() }
                                    }
                                }
                            }
                        )
                    }
            ) {
                // ── 共享 WebView 容器 ──────────────────────────────────────────
                if (isWebViewRoute) {
                    SharedWebViewContainer(
                        webView = sharedWebView,
                        hasError = false,
                        errorUrl = "",
                        onReload = {
                            val path = routeToPath[selectedRoute] ?: "/#/dashboard"
                            reloadPath(path)
                        }
                    )
                } else {
                    // 原生页面走 NavHost
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.MEDIA_DETAIL,
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable(NavRoutes.MEDIA_DETAIL) { backStackEntry ->
                            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                            val mediaId   = backStackEntry.arguments?.getString("mediaId") ?: ""
                            MediaDetailScreen(
                                mediaId = mediaId,
                                mediaType = mediaType,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }

                // ── 可拖动悬浮菜单按钮 ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            containerSize = coordinates.size
                        }
                ) {
                    if (containerSize != IntSize.Zero) {
                        val fabSizePx = with(fabDensity) { 48.dp.toPx() }

                        // 计算像素位置
                        val posX = (fabOffsetX * containerSize.width - fabSizePx / 2).roundToInt()
                        val posY = (fabOffsetY * containerSize.height - fabSizePx / 2).roundToInt()

                        Surface(
                            modifier = Modifier
                                .offset { IntOffset(posX.coerceAtLeast(0), posY.coerceAtLeast(0)) }
                                .size(48.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {},
                                        onDragEnd = {
                                            // 拖动结束，保存位置
                                            scope.launch {
                                                preferencesManager?.saveFabPosition(fabOffsetX, fabOffsetY)
                                            }
                                        },
                                        onDragCancel = {},
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val newX = fabOffsetX + dragAmount.x / containerSize.width
                                            val newY = fabOffsetY + dragAmount.y / containerSize.height
                                            fabOffsetX = newX.coerceIn(0f, 1f)
                                            fabOffsetY = newY.coerceIn(0f, 1f)
                                        }
                                    )
                                },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            shadowElevation = 6.dp
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "打开菜单",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 首次进入时加载默认页面
    LaunchedEffect(sharedWebView) {
        if (sharedWebView != null) {
            loadPath("/#/dashboard")
        }
    }

    // 退出时销毁 WebView
    DisposableEffect(Unit) {
        onDispose {
            sharedWebView?.stopLoading()
            sharedWebView?.destroy()
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFF4ecca3),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, fontSize = 13.sp, color = Color(0xFFB0B0B0))
        }
    }
}

// URL 编码/解码扩展函数
fun String.encodeUrl(): String = java.net.URLEncoder.encode(this, "UTF-8")
fun String.decodeUrl(): String = java.net.URLDecoder.decode(this, "UTF-8")

