package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moviepilot.app.data.model.MediaInfo
import com.moviepilot.app.data.model.ResourceFilter
import com.moviepilot.app.data.model.ResourceFilterDimension
import com.moviepilot.app.data.model.ResourceFilterEngine
import com.moviepilot.app.data.model.ResourceSort
import com.moviepilot.app.data.model.TorrentSearchResult
import com.moviepilot.app.ui.viewmodel.ResourceViewModel

/**
 * 资源搜索页（原生实现）
 *
 * 与其他页面的区别：本页不通过 WebView 加载服务器 PWA，
 * 而是直接调用后端 API 并在客户端完成筛选。
 * 这样筛选面板不会再出现「只有灰色遮罩」的问题。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceScreen(
    viewModel: ResourceViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }

    val trendingState by viewModel.trendingState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val downloadStateMap by viewModel.downloadState.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val filteredResults by viewModel.filteredResults.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(downloadStateMap) {
        downloadStateMap.values.forEach { state ->
            when (state) {
                is ResourceViewModel.DownloadState.Success ->
                    snackbarHostState.showSnackbar("已发送到服务器下载 ✓", duration = SnackbarDuration.Short)
                is ResourceViewModel.DownloadState.Error ->
                    snackbarHostState.showSnackbar("下载失败：${state.message}", duration = SnackbarDuration.Long)
                else -> {}
            }
        }
    }

    // 有搜索结果时才展示筛选入口
    val hasResults = searchState is ResourceViewModel.SearchState.Success

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF16213e)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF16213e))
                .padding(paddingValues)
        ) {
            // ── 搜索栏 ───────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索资源...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "清空", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1a1a2e),
                    unfocusedContainerColor = Color(0xFF1a1a2e),
                    focusedBorderColor = Color(0xFF4ecca3),
                    unfocusedBorderColor = Color(0xFF2a2a4a),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF4ecca3)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // ── 搜索按钮 + 筛选入口 ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.searchTorrents(searchQuery) },
                    enabled = searchQuery.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ecca3)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("搜索")
                }

                // 筛选按钮 —— 点击展开原生筛选面板
                if (hasResults) {
                    BadgedBox(
                        badge = {
                            if (filter.activeCount > 0) {
                                Badge { Text("${filter.activeCount}") }
                            }
                        }
                    ) {
                        FilledTonalButton(
                            onClick = { showFilterSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (filter.hasActiveFilter) Color(0xFF4ecca3) else Color(0xFF2a2a4a),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("筛选")
                        }
                    }
                }
            }

            // 结果统计条
            if (hasResults) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val total = (searchState as ResourceViewModel.SearchState.Success).torrents.size
                    Text(
                        text = if (filter.hasActiveFilter)
                            "筛选后 ${filteredResults.size} / 共 $total 条"
                        else "共 $total 条资源",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    if (filter.hasActiveFilter) {
                        TextButton(onClick = { viewModel.clearFilter() }) {
                            Text("清除筛选", color = Color(0xFF4ecca3), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── 内容区 ──────────────────────────────────────────────
            when {
                searchState is ResourceViewModel.SearchState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF4ecca3))
                    }
                }
                searchState is ResourceViewModel.SearchState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                (searchState as ResourceViewModel.SearchState.Error).message,
                                color = Color.Red
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.searchTorrents(searchQuery) }) { Text("重试") }
                        }
                    }
                }
                hasResults -> {
                    if (filteredResults.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.FilterAltOff,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text("没有符合筛选条件的资源", color = Color.Gray)
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = { viewModel.clearFilter() }) {
                                    Text("清除筛选条件", color = Color(0xFF4ecca3))
                                }
                            }
                        }
                    } else {
                        TorrentList(
                            torrents = filteredResults,
                            downloadStateMap = downloadStateMap,
                            onDownload = { viewModel.downloadTorrent(it) }
                        )
                    }
                }
                else -> {
                    when (val state = trendingState) {
                        is ResourceViewModel.TrendingState.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF4ecca3))
                            }
                        }
                        is ResourceViewModel.TrendingState.Success ->
                            TrendingContent(state.mediaList)
                        is ResourceViewModel.TrendingState.Error -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(state.message, color = Color.Red)
                                    Spacer(Modifier.height(16.dp))
                                    Button(onClick = { viewModel.loadTrending() }) { Text("重试") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 原生筛选面板 ─────────────────────────────────────────────
    if (showFilterSheet) {
        ResourceFilterSheet(
            allResults = (searchState as? ResourceViewModel.SearchState.Success)?.torrents ?: emptyList(),
            current = filter,
            onDismiss = { showFilterSheet = false },
            onApply = {
                viewModel.updateFilter(it)
                showFilterSheet = false
            },
            onReset = { viewModel.clearFilter() }
        )
    }
}

/**
 * 筛选面板（ModalBottomSheet）
 *
 * 所有选项从当前搜索结果中动态提取，选项旁的数字为该值命中的资源条数。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourceFilterSheet(
    allResults: List<TorrentSearchResult>,
    current: ResourceFilter,
    onDismiss: () -> Unit,
    onApply: (ResourceFilter) -> Unit,
    onReset: () -> Unit
) {
    // 本地草稿，点「应用」才生效
    var draft by remember { mutableStateOf(current) }
    var expandedDimension by remember { mutableStateOf<ResourceFilterDimension?>(null) }

    val dimensionOptions = remember(allResults) {
        ResourceFilterDimension.entries.associateWith { dim ->
            ResourceFilterEngine.extractOptions(allResults, dim)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1a1a2e),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .padding(horizontal = 20.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "筛选资源",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { draft = ResourceFilter.EMPTY }) {
                        Text("重置", color = Color.Gray, fontSize = 13.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.Gray)
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2a2a4a))

            // 维度手风琴
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                ResourceFilterDimension.entries.forEach { dim ->
                    val selected = selectedValuesOf(draft, dim)
                    val options = dimensionOptions[dim].orEmpty()
                    val headerBg = if (selected.isNotEmpty()) Color(0xFF4ecca3).copy(alpha = 0.15f)
                    else Color.Transparent

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(headerBg)
                            .clickable {
                                expandedDimension = if (expandedDimension == dim) null else dim
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    dim.label,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected.isNotEmpty()) Color(0xFF4ecca3) else Color.White
                                )
                                if (selected.isNotEmpty()) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("(${selected.size})", color = Color(0xFF4ecca3), fontSize = 12.sp)
                                }
                            }
                            Icon(
                                if (expandedDimension == dim) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    }

                    // 展开该维度的选项
                    if (expandedDimension == dim) {
                        if (options.isEmpty()) {
                            Text(
                                "  当前结果中无可用选项",
                                color = Color.Gray.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        } else {
                            FlowChips(
                                options = options,
                                selected = selected,
                                onToggle = { value ->
                                    draft = toggleSelection(draft, dim, value)
                                }
                            )
                        }
                    }
                }

                // ── 数值型条件 ───────────────────────────────────────
                NumericFilterSection(draft = draft, onChange = { draft = it })

                // ── 开关型条件 ───────────────────────────────────────
                Column(Modifier.padding(vertical = 8.dp)) {
                    SwitchRow(
                        label = "仅显示免费种",
                        checked = draft.onlyFree,
                        onChange = { draft = draft.copy(onlyFree = it) }
                    )
                    SwitchRow(
                        label = "仅显示促销种",
                        checked = draft.onlyPromotion,
                        onChange = { draft = draft.copy(onlyPromotion = it) }
                    )
                }

                // ── 排序 ─────────────────────────────────────────────
                Text(
                    "排序方式",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                )
                FlowChips(
                    options = ResourceSort.entries.map { it.label },
                    selected = setOf(draft.sortBy.label),
                    onToggle = { label ->
                        ResourceSort.entries.firstOrNull { it.label == label }?.let {
                            draft = draft.copy(sortBy = it)
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))
            }

            HorizontalDivider(color = Color(0xFF2a2a4a))

            // 底部操作
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("清除全部", color = Color.Gray)
                }
                Button(
                    onClick = { onApply(draft) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ecca3)),
                    modifier = Modifier.weight(1.4f)
                ) {
                    val preview = ResourceFilterEngine.apply(allResults, draft).size
                    Text(
                        if (draft.hasActiveFilter) "应用筛选（$preview）" else "应用",
                        color = Color.White
                    )
                }
            }
        }
    }
}

/** 数值与开关条件区 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumericFilterSection(
    draft: ResourceFilter,
    onChange: (ResourceFilter) -> Unit
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        // 体积区间
        Text("体积区间", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
        FlowChips(
            options = ResourceFilterEngine.SIZE_PRESETS.map { it.first },
            selected = setOf(currentSizeLabel(draft)),
            onToggle = { label ->
                val preset = ResourceFilterEngine.SIZE_PRESETS.firstOrNull { it.first == label }
                when (label) {
                    "不限" -> onChange(draft.copy(minSize = 0L, maxSize = Long.MAX_VALUE))
                    "< 1GB" -> onChange(draft.copy(minSize = 0L, maxSize = 1L * 1024 * 1024 * 1024))
                    "1-5GB" -> onChange(
                        draft.copy(minSize = 1L * 1024 * 1024 * 1024, maxSize = 5L * 1024 * 1024 * 1024)
                    )
                    "5-20GB" -> onChange(
                        draft.copy(minSize = 5L * 1024 * 1024 * 1024, maxSize = 20L * 1024 * 1024 * 1024)
                    )
                    "20-50GB" -> onChange(
                        draft.copy(minSize = 20L * 1024 * 1024 * 1024, maxSize = 50L * 1024 * 1024 * 1024)
                    )
                    "> 50GB" -> onChange(draft.copy(minSize = 50L * 1024 * 1024 * 1024, maxSize = Long.MAX_VALUE))
                }
            }
        )

        // 做种数下限
        Spacer(Modifier.height(12.dp))
        Text("最少做种数", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
        FlowChips(
            options = listOf("不限", "1", "5", "10", "20", "50"),
            selected = setOf(
                when (draft.minSeeders) {
                    0 -> "不限"; 1 -> "1"; 5 -> "5"; 10 -> "10"; 20 -> "20"; 50 -> "50"
                    else -> "不限"
                }
            ),
            onToggle = { label ->
                onChange(draft.copy(minSeeders = if (label == "不限") 0 else label.toIntOrNull() ?: 0))
            }
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4ecca3),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF2a2a4a),
                uncheckedBorderColor = Color.Gray
            )
        )
    }
}

/**
 * 多选标签组（支持横向滚动，避免选项过多时换行占满屏幕）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.take(40).forEach { option ->
            val isOn = option in selected
            FilterChip(
                selected = isOn,
                onClick = { onToggle(option) },
                label = { Text(option, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF2a2a4a),
                    labelColor = Color(0xFFcccccc),
                    selectedContainerColor = Color(0xFF4ecca3),
                    selectedLabelColor = Color.Black
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isOn,
                    borderColor = Color(0xFF3a3a5a),
                    selectedBorderColor = Color(0xFF4ecca3)
                )
            )
        }
    }
}

// ── 辅助函数 ────────────────────────────────────────────────────

private fun selectedValuesOf(
    filter: ResourceFilter,
    dim: ResourceFilterDimension
): Set<String> = when (dim) {
    ResourceFilterDimension.SITE -> filter.sites
    ResourceFilterDimension.RESOLUTION -> filter.resolutions
    ResourceFilterDimension.ENCODE -> filter.encodes
    ResourceFilterDimension.RESOURCE_TYPE -> filter.resourceTypes
    ResourceFilterDimension.EFFECT -> filter.effects
    ResourceFilterDimension.TEAM -> filter.teams
    ResourceFilterDimension.CATEGORY -> filter.categories
}

private fun toggleSelection(
    filter: ResourceFilter,
    dim: ResourceFilterDimension,
    value: String
): ResourceFilter {
    fun Set<String>.toggle(): Set<String> =
        if (contains(value)) this - value else this + value

    return when (dim) {
        ResourceFilterDimension.SITE -> filter.copy(sites = filter.sites.toggle())
        ResourceFilterDimension.RESOLUTION -> filter.copy(resolutions = filter.resolutions.toggle())
        ResourceFilterDimension.ENCODE -> filter.copy(encodes = filter.encodes.toggle())
        ResourceFilterDimension.RESOURCE_TYPE -> filter.copy(resourceTypes = filter.resourceTypes.toggle())
        ResourceFilterDimension.EFFECT -> filter.copy(effects = filter.effects.toggle())
        ResourceFilterDimension.TEAM -> filter.copy(teams = filter.teams.toggle())
        ResourceFilterDimension.CATEGORY -> filter.copy(categories = filter.categories.toggle())
    }
}

private fun currentSizeLabel(filter: ResourceFilter): String {
    val gb = 1024L * 1024 * 1024
    return when {
        filter.minSize == 0L && filter.maxSize == Long.MAX_VALUE -> "不限"
        filter.minSize == 0L && filter.maxSize == gb -> "< 1GB"
        filter.minSize == gb && filter.maxSize == 5 * gb -> "1-5GB"
        filter.minSize == 5 * gb && filter.maxSize == 20 * gb -> "5-20GB"
        filter.minSize == 20 * gb && filter.maxSize == 50 * gb -> "20-50GB"
        filter.minSize == 50 * gb && filter.maxSize == Long.MAX_VALUE -> "> 50GB"
        else -> "不限"
    }
}

// ══════════════════════════════════════════════════════════════
//  以下为保留的既有组件（TrendingContent / MediaCard / TorrentList /
//  TorrentCard / TorrentInfoItem / formatFileSize）
// ══════════════════════════════════════════════════════════════

@Composable
fun TrendingContent(mediaList: List<MediaInfo>) {
    if (mediaList.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无推荐内容", color = Color.Gray, fontSize = 16.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mediaList) { media -> MediaCard(media) }
        }
    }
}

@Composable
fun MediaCard(media: MediaInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = if (media.posterPath?.startsWith("http") == true) media.posterPath
                else "https://image.tmdb.org/t/p/w200${media.posterPath}",
                contentDescription = media.title,
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    media.year?.let { Text(text = "$it", fontSize = 12.sp, color = Color.Gray) }
                    media.voteAverage?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = String.format("★ %.1f", it),
                            fontSize = 12.sp,
                            color = Color(0xFFffd93d)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TorrentList(
    torrents: List<TorrentSearchResult>,
    downloadStateMap: Map<String, ResourceViewModel.DownloadState> = emptyMap(),
    onDownload: (TorrentSearchResult) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(torrents) { result ->
            TorrentCard(
                result = result,
                downloadStateMap = downloadStateMap,
                onDownload = onDownload
            )
        }
    }
}

@Composable
fun TorrentCard(
    result: TorrentSearchResult,
    downloadStateMap: Map<String, ResourceViewModel.DownloadState> = emptyMap(),
    onDownload: (TorrentSearchResult) -> Unit = {}
) {
    val torrent = result.torrentInfo
    val meta = result.metaInfo
    val key = torrent.enclosure ?: torrent.title
    val dlState = downloadStateMap[key]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = meta?.title ?: torrent.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            meta?.subtitle?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TorrentInfoItem("站点", torrent.siteName)
                TorrentInfoItem("做种", "${torrent.seeders}", Color(0xFF4ecca3))
                TorrentInfoItem("下载", "${torrent.peers}", Color(0xFFff6b6b))
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TorrentInfoItem("大小", formatFileSize(torrent.size))
                meta?.resourcePix?.let { TorrentInfoItem("分辨率", it) }
                meta?.videoEncode?.let { TorrentInfoItem("编码", it) }
            }

            if (!torrent.volumeFactor.isNullOrBlank() && torrent.volumeFactor != "100%") {
                Spacer(Modifier.height(4.dp))
                TorrentInfoItem("促销", torrent.volumeFactor, Color(0xFFffd93d))
            }

            if (!torrent.freeDateDiff.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                TorrentInfoItem("免费", torrent.freeDateDiff, Color(0xFF4ecca3))
            }

            Spacer(Modifier.height(8.dp))

            when (dlState) {
                is ResourceViewModel.DownloadState.Loading -> {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("提交中...", fontSize = 12.sp)
                    }
                }
                is ResourceViewModel.DownloadState.Success -> {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2d8a5e))
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("已添加下载", fontSize = 12.sp)
                    }
                }
                is ResourceViewModel.DownloadState.Error -> {
                    Button(
                        onClick = { onDownload(result) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFcc4444))
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("下载失败，点击重试", fontSize = 12.sp)
                    }
                }
                else -> {
                    Button(
                        onClick = { onDownload(result) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ecca3))
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("下载到服务器", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TorrentInfoItem(label: String, value: String, color: Color = Color.Gray) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "$label:", fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

fun formatFileSize(size: Long): String {
    return when {
        size >= 1024L * 1024 * 1024 * 1024 ->
            String.format("%.2f TB", size / 1024.0 / 1024 / 1024 / 1024)
        size >= 1024 * 1024 * 1024 ->
            String.format("%.2f GB", size / 1024.0 / 1024 / 1024)
        size >= 1024 * 1024 ->
            String.format("%.2f MB", size / 1024.0 / 1024)
        else -> String.format("%.2f KB", size / 1024.0)
    }
}
