package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.moviepilot.app.data.model.DownloadTask
import com.moviepilot.app.data.model.TransferHistory
import com.moviepilot.app.ui.viewmodel.DownloadViewModel

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("下载任务", "历史记录")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16213e))
    ) {
        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1a1a2e),
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // Content
        when (selectedTab) {
            0 -> DownloadTasksTab(viewModel)
            1 -> HistoryTab(viewModel)
        }
    }
}

@Composable
fun DownloadTasksTab(viewModel: DownloadViewModel) {
    val downloadState by viewModel.downloadState.collectAsState()

    when (val state = downloadState) {
        is DownloadViewModel.DownloadState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4ecca3))
            }
        }
        is DownloadViewModel.DownloadState.Success -> {
            if (state.tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无下载任务", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.tasks) { task ->
                        DownloadTaskCard(task)
                    }
                }
            }
        }
        is DownloadViewModel.DownloadState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadDownloads() }) {
                        Text("重试")
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadTaskCard(task: DownloadTask) {
    val title = task.title ?: task.name ?: "未知任务"
    val seasonEpisode = task.seasonEpisode ?: ""
    val displayTitle = if (seasonEpisode.isNotEmpty()) "$title $seasonEpisode" else title

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Poster thumbnail
            if (!task.media?.image.isNullOrEmpty()) {
                AsyncImage(
                    model = task.media?.image,
                    contentDescription = title,
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = displayTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                task.progress?.let { progress ->
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Color(0xFF4ecca3),
                        trackColor = Color.Gray.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format("%.1f%%", progress),
                            fontSize = 12.sp,
                            color = Color(0xFF4ecca3),
                            fontWeight = FontWeight.Bold
                        )

                        task.size?.let {
                            Text(
                                text = formatFileSize(it),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    task.dlspeed?.let {
                        Text(
                            text = "↓ $it/s",
                            fontSize = 11.sp,
                            color = Color(0xFF4ecca3)
                        )
                    }
                    task.upspeed?.let {
                        Text(
                            text = "↑ $it/s",
                            fontSize = 11.sp,
                            color = Color(0xFFff6b6b)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    task.downloader?.let {
                        Text(
                            text = it,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    task.leftTime?.let {
                        Text(
                            text = it,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                task.state?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = translateState(it),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryTab(viewModel: DownloadViewModel) {
    val historyState by viewModel.historyState.collectAsState()

    when (val state = historyState) {
        is DownloadViewModel.HistoryState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4ecca3))
            }
        }
        is DownloadViewModel.HistoryState.Success -> {
            if (state.history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无历史记录", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.history) { history ->
                        HistoryCard(history)
                    }
                }
            }
        }
        is DownloadViewModel.HistoryState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadHistory() }) {
                        Text("重试")
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(history: TransferHistory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = history.title ?: "未知",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${history.type ?: ""}",
                    fontSize = 12.sp,
                    color = Color(0xFF4ecca3)
                )
                history.date?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val seasonEpisodeText = listOfNotNull(
                    history.seasons,
                    history.episodes
                ).joinToString(" ")
                if (seasonEpisodeText.isNotEmpty()) {
                    Text(
                        text = seasonEpisodeText,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            history.status?.let { status ->
                Text(
                    text = if (status) "成功" else "失败",
                    fontSize = 11.sp,
                    color = if (status) Color(0xFF4ecca3) else Color(0xFFff6b6b)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val ids = listOfNotNull(
                history.tmdbId?.let { "TMDB: $it" },
                history.imdbId?.let { "IMDB: $it" },
                history.doubanId?.let { "豆瓣: $it" }
            ).joinToString(" | ")
            if (ids.isNotEmpty()) {
                Text(
                    text = ids,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

fun translateState(state: String): String {
    return when (state.lowercase()) {
        "downloading" -> "下载中"
        "seeding" -> "做种中"
        "paused" -> "已暂停"
        "completed" -> "已完成"
        "error" -> "错误"
        else -> state
    }
}
