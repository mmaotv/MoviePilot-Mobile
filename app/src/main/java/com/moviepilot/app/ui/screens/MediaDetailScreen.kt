package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moviepilot.app.data.model.TmdbEpisode
import com.moviepilot.app.data.model.TmdbPerson
import com.moviepilot.app.data.model.TmdbSeasonDetail
import com.moviepilot.app.ui.viewmodel.MediaDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    mediaId: String,
    mediaType: String,
    title: String? = null,
    year: String? = null,
    viewModel: MediaDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSeason by remember { mutableIntStateOf(1) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(mediaId, mediaType) {
        viewModel.loadMediaDetail(mediaId, mediaType, title, year)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(when (val state = uiState) {
                    is MediaDetailViewModel.UiState.Success -> state.mediaDetail.title
                    else -> "媒体详情"
                }, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1a1a2e))
            )
        },
        containerColor = Color(0xFF16213e)
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (val state = uiState) {
                is MediaDetailViewModel.UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF4ecca3))
                    }
                }
                is MediaDetailViewModel.UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = Color.Red)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadMediaDetail(mediaId, mediaType, title, year) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                is MediaDetailViewModel.UiState.Success -> {
                    MediaDetailContent(
                        state = state,
                        selectedSeason = selectedSeason,
                        onSeasonSelected = { selectedSeason = it },
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            }
        }
    }
}

@Composable
fun MediaDetailContent(
    state: MediaDetailViewModel.UiState.Success,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val media = state.mediaDetail
    val seasons = state.seasons
    val episodes = state.episodes
    val credits = state.credits

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Backdrop and Poster
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                // Backdrop
                AsyncImage(
                    model = if (media.backdropPath?.startsWith("http") == true) {
                        media.backdropPath
                    } else {
                        "https://image.tmdb.org/t/p/original${media.backdropPath}"
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF16213e))
                            )
                        )
                )
                // Poster
                AsyncImage(
                    model = if (media.posterPath?.startsWith("http") == true) {
                        media.posterPath
                    } else {
                        "https://image.tmdb.org/t/p/w500${media.posterPath}"
                    },
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                        .size(width = 100.dp, height = 150.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }

        // Title and Info
        item {
            Column(modifier = Modifier.padding(start = 120.dp)) {
                Text(
                    text = media.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    media.year?.let {
                        Text(text = "$it", fontSize = 14.sp, color = Color.Gray)
                    }
                    media.voteAverage?.let {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "★ ${String.format("%.1f", it)}", fontSize = 14.sp, color = Color(0xFFffd93d))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (media.type == "TV") "电视剧" else "电影",
                        fontSize = 14.sp,
                        color = Color(0xFF4ecca3)
                    )
                }
            }
        }

        // Overview
        media.overview?.let { overview ->
            if (overview.isNotBlank()) {
                item {
                    Text(
                        text = overview,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 4
                    )
                }
            }
        }

        // Tabs for Seasons/Cast/Info
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton("季", selectedTab == 0) { onTabSelected(0) }
                TabButton("演职员", selectedTab == 1) { onTabSelected(1) }
                TabButton("详情", selectedTab == 2) { onTabSelected(2) }
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> {
                // Seasons list
                if (seasons.isNotEmpty()) {
                    item {
                        SeasonSelector(
                            seasons = seasons,
                            selectedSeason = selectedSeason,
                            onSeasonSelected = onSeasonSelected
                        )
                    }
                }
                // Episodes list
                if (episodes.isNotEmpty()) {
                    items(episodes) { episode ->
                        EpisodeCard(episode)
                    }
                } else if (seasons.isEmpty()) {
                    item {
                        Text("暂无分集信息", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
            1 -> {
                // Cast and Crew
                if (credits.isNotEmpty()) {
                    items(credits.take(20)) { person ->
                        PersonCard(person)
                    }
                } else {
                    item {
                        Text("暂无演职员信息", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
            2 -> {
                // Media Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("类型", media.type ?: media.mediaType ?: "未知")
                            media.popularity?.let { InfoRow("热度", String.format("%.1f", it)) }
                            media.releaseDate?.let { InfoRow("上映日期", it) }
                            media.firstAirDate?.let { InfoRow("首播日期", it) }
                            media.originalLanguage?.let { InfoRow("原语言", it.uppercase()) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color(0xFF1a1a2e),
            labelColor = Color.White,
            selectedContainerColor = Color(0xFF4ecca3).copy(alpha = 0.3f),
            selectedLabelColor = Color(0xFF4ecca3)
        )
    )
}

@Composable
fun SeasonSelector(
    seasons: List<TmdbSeasonDetail>,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        seasons.forEach { season ->
            FilterChip(
                selected = season.seasonNumber == selectedSeason,
                onClick = {
                    season.seasonNumber?.let { onSeasonSelected(it) }
                },
                label = { Text(season.name ?: "第${season.seasonNumber}季", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF1a1a2e),
                    labelColor = Color.White,
                    selectedContainerColor = Color(0xFF4ecca3).copy(alpha = 0.3f),
                    selectedLabelColor = Color(0xFF4ecca3)
                )
            )
        }
    }
}

@Composable
fun EpisodeCard(episode: TmdbEpisode) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Still image
            AsyncImage(
                model = if (episode.stillPath?.startsWith("http") == true) {
                    episode.stillPath
                } else {
                    "https://image.tmdb.org/t/p/w300${episode.stillPath}"
                },
                contentDescription = episode.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.name ?: ""}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                episode.airDate?.let {
                    Text(text = it, fontSize = 11.sp, color = Color.Gray)
                }
                episode.voteAverage?.let {
                    Text(
                        text = "★ ${String.format("%.1f", it)}",
                        fontSize = 11.sp,
                        color = Color(0xFFffd93d)
                    )
                }
                episode.overview?.let {
                    if (it.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PersonCard(person: TmdbPerson) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = if (person.profilePath?.startsWith("http") == true) {
                    person.profilePath
                } else {
                    "https://image.tmdb.org/t/p/w185${person.profilePath}"
                },
                contentDescription = person.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
            )
            Column {
                Text(
                    text = person.name ?: "",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                val role = person.character ?: person.job
                role?.let {
                    Text(text = it, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(text = value, fontSize = 13.sp, color = Color.White)
    }
}
