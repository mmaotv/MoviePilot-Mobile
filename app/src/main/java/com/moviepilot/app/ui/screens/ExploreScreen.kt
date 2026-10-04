package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moviepilot.app.data.model.DoubanHotItem
import com.moviepilot.app.data.model.MediaInfo
import com.moviepilot.app.ui.viewmodel.ExploreSource
import com.moviepilot.app.ui.viewmodel.ExploreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel(),
    onMediaClick: (String, String) -> Unit = { _, _ -> }
) {
    val currentSource by viewModel.currentSource.collectAsState()
    val tmdbMoviesState by viewModel.tmdbMoviesState.collectAsState()
    val tmdbTVsState by viewModel.tmdbTVsState.collectAsState()
    val doubanMoviesState by viewModel.doubanMoviesState.collectAsState()
    val doubanTVsState by viewModel.doubanTVsState.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16213e))
    ) {
        // Source Tabs
        ExploreSourceTabs(
            currentSource = currentSource,
            onSourceSelected = { viewModel.setSource(it) }
        )

        // Filter Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentSource.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            FilterChip(
                selected = showFilterDialog,
                onClick = { showFilterDialog = true },
                label = { Text("筛选", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF1a1a2e),
                    labelColor = Color.White
                )
            )
        }

        // Content Grid
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentSource) {
                ExploreSource.TMDB_MOVIES -> {
                    when (val state = tmdbMoviesState) {
                        is ExploreViewModel.MediaExploreState.Loading -> {
                            LoadingIndicator()
                        }
                        is ExploreViewModel.MediaExploreState.Success -> {
                            if (state.data.isEmpty()) {
                                EmptyState("暂无内容")
                            } else {
                                MediaGrid(
                                    items = state.data,
                                    onItemClick = { media ->
                                        onMediaClick("movie", media.id?.toString() ?: "")
                                    }
                                ) { media ->
                                    ExploreMediaCard(media)
                                }
                            }
                        }
                        is ExploreViewModel.MediaExploreState.Error -> {
                            ErrorState(state.message) { viewModel.loadTmdbMovies() }
                        }
                        else -> Unit
                    }
                }
                ExploreSource.TMDB_TVS -> {
                    when (val state = tmdbTVsState) {
                        is ExploreViewModel.MediaExploreState.Loading -> {
                            LoadingIndicator()
                        }
                        is ExploreViewModel.MediaExploreState.Success -> {
                            if (state.data.isEmpty()) {
                                EmptyState("暂无内容")
                            } else {
                                MediaGrid(
                                    items = state.data,
                                    onItemClick = { media ->
                                        onMediaClick("tv", media.id?.toString() ?: "")
                                    }
                                ) { media ->
                                    ExploreMediaCard(media)
                                }
                            }
                        }
                        is ExploreViewModel.MediaExploreState.Error -> {
                            ErrorState(state.message) { viewModel.loadTmdbTVs() }
                        }
                        else -> Unit
                    }
                }
                ExploreSource.DOUBAN_MOVIES -> {
                    when (val state = doubanMoviesState) {
                        is ExploreViewModel.DoubanExploreState.Loading -> {
                            LoadingIndicator()
                        }
                        is ExploreViewModel.DoubanExploreState.Success -> {
                            if (state.data.isEmpty()) {
                                EmptyState("暂无内容")
                            } else {
                                MediaGrid(
                                    items = state.data,
                                    onItemClick = { item ->
                                        item.id?.let { onMediaClick("movie", it) }
                                    }
                                ) { item ->
                                    ExploreDoubanCard(item)
                                }
                            }
                        }
                        is ExploreViewModel.DoubanExploreState.Error -> {
                            ErrorState(state.message) { viewModel.loadDoubanMovies() }
                        }
                        else -> Unit
                    }
                }
                ExploreSource.DOUBAN_TVS -> {
                    when (val state = doubanTVsState) {
                        is ExploreViewModel.DoubanExploreState.Loading -> {
                            LoadingIndicator()
                        }
                        is ExploreViewModel.DoubanExploreState.Success -> {
                            if (state.data.isEmpty()) {
                                EmptyState("暂无内容")
                            } else {
                                MediaGrid(
                                    items = state.data,
                                    onItemClick = { item ->
                                        item.id?.let { onMediaClick("tv", it) }
                                    }
                                ) { item ->
                                    ExploreDoubanCard(item)
                                }
                            }
                        }
                        is ExploreViewModel.DoubanExploreState.Error -> {
                            ErrorState(state.message) { viewModel.loadDoubanTVs() }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        ExploreFilterDialog(
            currentSource = currentSource,
            onDismiss = { showFilterDialog = false },
            onApply = { filters ->
                viewModel.applyFilters(filters)
                showFilterDialog = false
            }
        )
    }
}

@Composable
fun ExploreSourceTabs(
    currentSource: ExploreSource,
    onSourceSelected: (ExploreSource) -> Unit
) {
    val tabIndex = ExploreSource.entries.indexOf(currentSource)
    ScrollableTabRow(
        selectedTabIndex = tabIndex,
        containerColor = Color(0xFF1a1a2e),
        contentColor = Color.White,
        edgePadding = 0.dp
    ) {
        ExploreSource.entries.forEach { source ->
            Tab(
                selected = currentSource == source,
                onClick = { onSourceSelected(source) },
                text = {
                    Text(
                        text = source.label,
                        fontSize = 14.sp,
                        color = if (currentSource == source) Color(0xFF4ecca3) else Color.Gray
                    )
                }
            )
        }
    }
}

@Composable
fun <T> MediaGrid(
    items: List<T>,
    onItemClick: (T) -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            Box(
                modifier = Modifier.clickable { onItemClick(item) }
            ) {
                itemContent(item)
            }
        }
    }
}

@Composable
fun ExploreMediaCard(media: MediaInfo) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Column {
            AsyncImage(
                model = if (media.posterPath?.startsWith("http") == true) {
                    media.posterPath
                } else {
                    "https://image.tmdb.org/t/p/w300${media.posterPath}"
                },
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )

            Text(
                text = media.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            media.voteAverage?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    media.year?.let { year ->
                        Text(text = "$year", fontSize = 9.sp, color = Color.Gray)
                    }
                    Text(
                        text = String.format("★ %.1f", it),
                        fontSize = 9.sp,
                        color = Color(0xFFffd93d)
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreDoubanCard(item: DoubanHotItem) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Column {
            AsyncImage(
                model = item.cover,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )

            Text(
                text = item.title ?: "",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                item.year?.let {
                    Text(text = it, fontSize = 9.sp, color = Color.Gray)
                }
                item.rate?.let {
                    Text(
                        text = String.format("★ %.1f", it),
                        fontSize = 9.sp,
                        color = Color(0xFFffd93d)
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreFilterDialog(
    currentSource: ExploreSource,
    onDismiss: () -> Unit,
    onApply: (ExploreViewModel.ExploreFilters) -> Unit
) {
    var tmdbSortBy by remember { mutableStateOf("popularity.desc") }
    var tmdbGenres by remember { mutableStateOf("") }
    var doubanSort by remember { mutableStateOf("R") }
    var doubanTags by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选条件", color = Color.White) },
        text = {
            Column {
                when (currentSource) {
                    ExploreSource.TMDB_MOVIES, ExploreSource.TMDB_TVS -> {
                        Text("排序方式", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = tmdbSortBy,
                            onValueChange = { tmdbSortBy = it },
                            label = { Text("排序", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4ecca3),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("类型ID (逗号分隔)", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = tmdbGenres,
                            onValueChange = { tmdbGenres = it },
                            label = { Text("类型", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4ecca3),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                    ExploreSource.DOUBAN_MOVIES, ExploreSource.DOUBAN_TVS -> {
                        Text("排序方式", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = doubanSort,
                            onValueChange = { doubanSort = it },
                            label = { Text("排序 (R=评分, T=热度)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4ecca3),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("标签 (逗号分隔)", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = doubanTags,
                            onValueChange = { doubanTags = it },
                            label = { Text("标签", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4ecca3),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val filters = ExploreViewModel.ExploreFilters(
                    tmdbSortBy = tmdbSortBy,
                    tmdbGenres = tmdbGenres,
                    doubanSort = doubanSort,
                    doubanTags = doubanTags
                )
                onApply(filters)
            }) {
                Text("应用", color = Color(0xFF4ecca3))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1a1a2e)
    )
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF4ecca3))
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = Color.Gray, fontSize = 16.sp)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = Color.Red)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}
