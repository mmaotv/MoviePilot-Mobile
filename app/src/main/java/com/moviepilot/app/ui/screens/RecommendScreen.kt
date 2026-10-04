package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import com.moviepilot.app.data.model.TrendingItem
import com.moviepilot.app.ui.viewmodel.RecommendViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendScreen(
    viewModel: RecommendViewModel = hiltViewModel(),
    onMediaClick: (String, String) -> Unit = { _, _ -> }
) {
    val doubanHotMovies by viewModel.doubanHotMoviesState.collectAsState()
    val doubanHotTVs by viewModel.doubanHotTVsState.collectAsState()
    val tmdbTrending by viewModel.tmdbTrendingState.collectAsState()
    val doubanTop250 by viewModel.doubanTop250State.collectAsState()
    val doubanWeeklyChinese by viewModel.doubanWeeklyChineseState.collectAsState()
    val doubanWeeklyGlobal by viewModel.doubanWeeklyGlobalState.collectAsState()
    val doubanAnimation by viewModel.doubanAnimationState.collectAsState()
    val nowPlaying by viewModel.nowPlayingState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16213e))
    ) {
        // TMDB Trending
        item {
            when (val state = tmdbTrending) {
                is RecommendViewModel.TrendingState.Success -> {
                    if (state.data.isNotEmpty()) {
                        RecommendSection(
                            title = "TMDB 趋势",
                            items = state.data,
                            onRefresh = { viewModel.loadTmdbTrending() }
                        ) { item ->
                            TrendingCard(item, onClick = {
                                val type = if (item.mediaType == "movie") "movie" else "tv"
                                onMediaClick(type, item.id?.toString() ?: "")
                            })
                        }
                    }
                }
                else -> Unit
            }
        }

        // Douban Hot Movies
        item {
            when (val state = doubanHotMovies) {
                is RecommendViewModel.DoubanState.Success -> {
                    if (state.data.isNotEmpty()) {
                        RecommendSection(
                            title = "豆瓣热门电影",
                            items = state.data,
                            onRefresh = { viewModel.loadDoubanHotMovies() }
                        ) { item ->
                            DoubanCard(item, onClick = {
                                item.id?.let { onMediaClick("movie", it) }
                            })
                        }
                    }
                }
                else -> Unit
            }
        }

        // Douban Hot TVs
        item {
            when (val state = doubanHotTVs) {
                is RecommendViewModel.DoubanState.Success -> {
                    if (state.data.isNotEmpty()) {
                        RecommendSection(
                            title = "豆瓣热门电视剧",
                            items = state.data,
                            onRefresh = { viewModel.loadDoubanHotTVs() }
                        ) { item ->
                            DoubanCard(item, onClick = {
                                item.id?.let { onMediaClick("tv", it) }
                            })
                        }
                    }
                }
                else -> Unit
            }
        }

        // Now Playing
        item {
            when (val state = nowPlaying) {
                is RecommendViewModel.MediaInfoState.Success -> {
                    if (state.data.isNotEmpty()) {
                        RecommendSection(
                            title = "正在热映",
                            items = state.data,
                            onRefresh = { viewModel.loadNowPlaying() }
                        ) { item ->
                            MediaPosterCard(item, onClick = {
                                val type = if (item.mediaType == "电影" || item.mediaType == "movie") "movie" else "tv"
                                onMediaClick(type, item.id?.toString() ?: "")
                            })
                        }
                    }
                }
                else -> Unit
            }
        }

        // Douban Top250
        item {
            when (val state = doubanTop250) {
                is RecommendViewModel.DoubanState.Success -> {
                    if (state.data.isNotEmpty()) {
                        RecommendSection(
                            title = "豆瓣 Top250",
                            items = state.data,
                            onRefresh = { viewModel.loadDoubanTop250() }
                        ) { item ->
                            DoubanCard(item, onClick = {
                                item.id?.let { onMediaClick("movie", it) }
                            })
                        }
                    }
                }
                else -> Unit
            }
        }

        // Douban Weekly Chinese
        item {
            when (val state = doubanWeeklyChinese) {
                is RecommendViewModel.DoubanState.Success -> {
                    if (state.data.isNotEmpty()) {
                        RecommendSection(
                            title = "华语口碑周榜",
                            items = state.data,
                            onRefresh = { viewModel.loadDoubanWeeklyChinese() }
                        ) { item ->
                            DoubanCard(item, onClick = {
                                val type = if (item.isTv == true) "tv" else "movie"
                                item.id?.let { onMediaClick(type, it) }
                            })
                        }
                    }
                }
                else -> Unit
            }
        }

        // Douban Weekly Global
        item {
            when (val state = doubanWeeklyGlobal) {
                is RecommendViewModel.DoubanState.Success -> {
                    if (state.data.isNotEmpty()) {
                        RecommendSection(
                            title = "全球口碑周榜",
                            items = state.data,
                            onRefresh = { viewModel.loadDoubanWeeklyGlobal() }
                        ) { item ->
                            DoubanCard(item, onClick = {
                                val type = if (item.isTv == true) "tv" else "movie"
                                item.id?.let { onMediaClick(type, it) }
                            })
                        }
                    }
                }
                else -> Unit
            }
        }

        // Douban Animation
        item {
            when (val state = doubanAnimation) {
                is RecommendViewModel.DoubanState.Success -> {
                    if (state.data.isNotEmpty()) {
                        RecommendSection(
                            title = "豆瓣动漫",
                            items = state.data,
                            onRefresh = { viewModel.loadDoubanAnimation() }
                        ) { item ->
                            DoubanCard(item, onClick = {
                                item.id?.let { onMediaClick("tv", it) }
                            })
                        }
                    }
                }
                else -> Unit
            }
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun <T> RecommendSection(
    title: String,
    items: List<T>,
    onRefresh: () -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新",
                    tint = Color(0xFF4ecca3),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                itemContent(item)
            }
        }
    }
}

@Composable
fun TrendingCard(item: TrendingItem, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = if (item.posterPath?.startsWith("http") == true) {
                    item.posterPath
                } else {
                    "https://image.tmdb.org/t/p/w300${item.posterPath}"
                },
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = item.title ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    item.releaseDate?.take(4)?.let {
                        Text(text = it, fontSize = 10.sp, color = Color.Gray)
                    }
                    item.voteAverage?.let {
                        Text(
                            text = String.format("★ %.1f", it),
                            fontSize = 10.sp,
                            color = Color(0xFFffd93d)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoubanCard(item: DoubanHotItem, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = item.cover,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = item.title ?: "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    item.year?.let {
                        Text(text = it, fontSize = 10.sp, color = Color.Gray)
                    }
                    item.rate?.let {
                        Text(
                            text = String.format("★ %.1f", it),
                            fontSize = 10.sp,
                            color = Color(0xFFffd93d)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MediaPosterCard(media: MediaInfo, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(210.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = media.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    media.year?.let {
                        Text(text = "$it", fontSize = 10.sp, color = Color.Gray)
                    }
                    media.voteAverage?.let {
                        Text(
                            text = String.format("★ %.1f", it),
                            fontSize = 10.sp,
                            color = Color(0xFFffd93d)
                        )
                    }
                }
            }
        }
    }
}
