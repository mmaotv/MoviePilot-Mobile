package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moviepilot.app.data.model.MediaInfo
import com.moviepilot.app.data.model.TorrentSearchResult
import com.moviepilot.app.ui.viewmodel.ResourceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceScreen(
    viewModel: ResourceViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val trendingState by viewModel.trendingState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val downloadStateMap by viewModel.downloadState.collectAsState()

    // 全局下载提示 Snackbar
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 监听下载状态变化，弹出提示
    LaunchedEffect(downloadStateMap) {
        downloadStateMap.values.forEach { state ->
            when (state) {
                is ResourceViewModel.DownloadState.Success -> {
                    snackbarHostState.showSnackbar("已发送到服务器下载 ✓", duration = SnackbarDuration.Short)
                }
                is ResourceViewModel.DownloadState.Error -> {
                    snackbarHostState.showSnackbar("下载失败：${state.message}", duration = SnackbarDuration.Long)
                }
                else -> {}
            }
        }
    }

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
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索资源...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1a1a2e),
                    unfocusedContainerColor = Color(0xFF1a1a2e),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Button(
                onClick = { viewModel.searchTorrents(searchQuery) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ecca3)),
                enabled = searchQuery.isNotBlank()
            ) {
                Text("搜索", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            when {
                searchState is ResourceViewModel.SearchState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4ecca3))
                    }
                }
                searchState is ResourceViewModel.SearchState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                (searchState as ResourceViewModel.SearchState.Error).message,
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.searchTorrents(searchQuery) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                searchState is ResourceViewModel.SearchState.Success -> {
                    val torrents = (searchState as ResourceViewModel.SearchState.Success).torrents
                    TorrentList(
                        torrents = torrents,
                        downloadStateMap = downloadStateMap,
                        onDownload = { torrent ->
                            viewModel.downloadTorrent(torrent)
                        }
                    )
                }
                else -> {
                    // Show trending content
                    when (val state = trendingState) {
                        is ResourceViewModel.TrendingState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF4ecca3))
                            }
                        }
                        is ResourceViewModel.TrendingState.Success -> {
                            TrendingContent(state.mediaList)
                        }
                        is ResourceViewModel.TrendingState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(state.message, color = Color.Red)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.loadTrending() }) {
                                        Text("重试")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingContent(mediaList: List<MediaInfo>) {
    if (mediaList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("暂无推荐内容", color = Color.Gray, fontSize = 16.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mediaList) { media ->
                MediaCard(media)
            }
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
            // Poster
            AsyncImage(
                model = if (media.posterPath?.startsWith("http") == true) {
                    media.posterPath
                } else {
                    "https://image.tmdb.org/t/p/w200${media.posterPath}"
                },
                contentDescription = media.title,
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    media.year?.let {
                        Text(text = "$it", fontSize = 12.sp, color = Color.Gray)
                    }
                    media.voteAverage?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("★ %.1f", it),
                            fontSize = 12.sp,
                            color = Color(0xFFffd93d)
                        )
                    }
                }
                media.overview?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
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
    if (torrents.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("未找到资源", color = Color.Gray, fontSize = 16.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
}

@Composable
fun TorrentCard(
    result: TorrentSearchResult,
    downloadStateMap: Map<String, ResourceViewModel.DownloadState> = emptyMap(),
    onDownload: (TorrentSearchResult) -> Unit = {}
) {
    val torrent = result.torrentInfo
    val meta    = result.metaInfo

    // 获取当前种子下载状态
    val key = torrent.enclosure ?: torrent.title
    val dlState = downloadStateMap[key]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title from meta_info or torrent_info
            Text(
                text = meta?.title ?: torrent.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2
            )

            // Subtitle
            meta?.subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Site and seeders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TorrentInfoItem("站点", torrent.siteName)
                TorrentInfoItem("做种", "${torrent.seeders}", Color(0xFF4ecca3))
                TorrentInfoItem("下载", "${torrent.peers}", Color(0xFFff6b6b))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Size and resolution
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TorrentInfoItem("大小", formatFileSize(torrent.size))
                meta?.resourcePix?.let { TorrentInfoItem("分辨率", it) }
                meta?.videoEncode?.let { TorrentInfoItem("编码", it) }
            }

            // Volume factor
            if (torrent.volumeFactor != null && torrent.volumeFactor.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                TorrentInfoItem("促销", torrent.volumeFactor, Color(0xFFffd93d))
            }

            // Free date
            if (torrent.freeDateDiff != null) {
                Spacer(modifier = Modifier.height(2.dp))
                TorrentInfoItem("免费", torrent.freeDateDiff, Color(0xFF4ecca3))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Download button with state feedback
            when (dlState) {
                is ResourceViewModel.DownloadState.Loading -> {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("提交中...", fontSize = 12.sp)
                    }
                }
                is ResourceViewModel.DownloadState.Success -> {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2d8a5e))
                    ) {
                        Icon(
                            Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("已添加下载", fontSize = 12.sp)
                    }
                }
                is ResourceViewModel.DownloadState.Error -> {
                    Button(
                        onClick = { onDownload(result) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFcc4444))
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("下载失败，点击重试", fontSize = 12.sp)
                    }
                }
                else -> {
                    Button(
                        onClick = { onDownload(result) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ecca3))
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
        size >= 1024L * 1024 * 1024 * 1024 -> String.format("%.2f TB", size / 1024.0 / 1024 / 1024 / 1024)
        size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / 1024.0 / 1024 / 1024)
        size >= 1024 * 1024 -> String.format("%.2f MB", size / 1024.0 / 1024)
        else -> String.format("%.2f KB", size / 1024.0)
    }
}

