package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moviepilot.app.data.model.Subscribe
import com.moviepilot.app.ui.viewmodel.SubscribeViewModel

@Composable
fun SubscribeScreen(
    onMediaClick: (String, String) -> Unit = { _, _ -> },
    viewModel: SubscribeViewModel = hiltViewModel()
) {
    val subscribeState by viewModel.subscribeState.collectAsState()
    var selectedType by remember { mutableStateOf<String?>(null) }
    var editingSubscription by remember { mutableStateOf<Subscribe?>(null) }
    var showRefreshDialog by remember { mutableStateOf(false) }

    // Show edit dialog when editingSubscription is not null
    editingSubscription?.let { subscription ->
        SubscribeEditDialog(
            subscription = subscription,
            onDismiss = { editingSubscription = null },
            onSave = { request ->
                viewModel.updateSubscription(request)
                editingSubscription = null
            }
        )
    }

    if (showRefreshDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshDialog = false },
            title = { Text("刷新订阅") },
            text = { Text("确定要刷新所有订阅吗？这将检查所有订阅的最新状态。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.refreshSubscribes()
                    showRefreshDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefreshDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16213e))
    ) {
        // Header with refresh button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showRefreshDialog = true }) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("刷新订阅")
            }
        }

        // Type Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == null,
                onClick = {
                    selectedType = null
                    viewModel.loadSubscriptions(null)
                },
                label = { Text("全部") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4ecca3)
                )
            )

            FilterChip(
                selected = selectedType == "电影",
                onClick = {
                    selectedType = "电影"
                    viewModel.loadSubscriptions("电影")
                },
                label = { Text("电影") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4ecca3)
                )
            )

            FilterChip(
                selected = selectedType == "电视剧",
                onClick = {
                    selectedType = "电视剧"
                    viewModel.loadSubscriptions("电视剧")
                },
                label = { Text("电视剧") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4ecca3)
                )
            )
        }

        // Content
        when (val state = subscribeState) {
            is SubscribeViewModel.SubscribeState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4ecca3))
                }
            }
            is SubscribeViewModel.SubscribeState.Success -> {
                if (state.subscriptions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Notifications,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("暂无订阅", color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "点击右下角添加订阅",
                                color = Color.Gray.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    SubscribeList(
                        subscriptions = state.subscriptions,
                        onDelete = { id -> viewModel.deleteSubscription(id) },
                        onEdit = { subscription -> editingSubscription = subscription },
                        onClick = { subscription ->
                            val mediaId = subscription.tmdbId?.toString()
                                ?: subscription.doubanId
                                ?: subscription.bangumiId?.toString()
                                ?: return@SubscribeList
                            val mediaType = if (subscription.type == "电影") "movie" else "tv"
                            onMediaClick(mediaType, mediaId)
                        }
                    )
                }
            }
            is SubscribeViewModel.SubscribeState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadSubscriptions(selectedType) }) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscribeList(
    subscriptions: List<Subscribe>,
    onDelete: (Int) -> Unit,
    onEdit: (Subscribe) -> Unit,
    onClick: (Subscribe) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(subscriptions) { subscription ->
            SubscribeCard(
                subscription = subscription,
                onDelete = onDelete,
                onEdit = onEdit,
                onClick = onClick
            )
        }
    }
}

@Composable
fun SubscribeCard(
    subscription: Subscribe,
    onDelete: (Int) -> Unit,
    onEdit: (Subscribe) -> Unit,
    onClick: (Subscribe) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除订阅 \"${subscription.name}\" 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(subscription.id)
                    showDeleteConfirm = false
                }) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(subscription) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Poster
            AsyncImage(
                model = if (subscription.poster?.startsWith("http") == true) {
                    subscription.poster
                } else {
                    "https://image.tmdb.org/t/p/w200${subscription.poster}"
                },
                contentDescription = subscription.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = subscription.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = if (subscription.type == "电影") "电影" else "剧集",
                                        fontSize = 10.sp
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFF4ecca3).copy(alpha = 0.2f),
                                    labelColor = Color(0xFF4ecca3)
                                ),
                                modifier = Modifier.height(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row {
                            subscription.yearValue?.let {
                                Text(
                                    text = it,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            subscription.season?.let {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "第${it}季",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4ecca3)
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { onEdit(subscription) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                "编辑",
                                tint = Color(0xFF4ecca3),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                "删除",
                                tint = Color.Red,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress - use lackEpisode if available
                if (subscription.lackEpisode != null && subscription.totalEpisode != null) {
                    val downloaded = subscription.totalEpisode - subscription.lackEpisode
                    val progress = if (subscription.totalEpisode > 0) {
                        downloaded.toFloat() / subscription.totalEpisode.toFloat()
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Color(0xFF4ecca3),
                        trackColor = Color.Gray.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${downloaded}/${subscription.totalEpisode} 集",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                subscription.lastUpdate?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "更新: $it",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                subscription.state?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    val stateColor = when (it) {
                        "R" -> Color(0xFF4ecca3) // Running
                        "P" -> Color(0xFFffc107) // Paused
                        "D" -> Color.Red // Disabled
                        else -> Color.Gray
                    }
                    Text(
                        text = "状态: $it",
                        fontSize = 11.sp,
                        color = stateColor
                    )
                }
            }
        }
    }
}
