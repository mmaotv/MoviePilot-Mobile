package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moviepilot.app.data.model.BangumiCalendarItem
import com.moviepilot.app.ui.viewmodel.CalendarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val calendarItems by viewModel.calendarItems.collectAsState()
    val selectedWeekday by viewModel.selectedWeekday.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCalendar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bangumi日历", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCalendar() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1a1a2e))
            )
        },
        containerColor = Color(0xFF16213e)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Weekday selector
            WeekdaySelector(
                selectedWeekday = selectedWeekday,
                onWeekdaySelected = { viewModel.selectWeekday(it) }
            )

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is CalendarViewModel.UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF4ecca3))
                        }
                    }
                    is CalendarViewModel.UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.message, color = Color.Red)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadCalendar() }) {
                                    Text("重试")
                                }
                            }
                        }
                    }
                    is CalendarViewModel.UiState.Success -> {
                        val filteredItems = if (selectedWeekday == 0) {
                            calendarItems
                        } else {
                            calendarItems.filter { 
                                it.weekday?.toString() == selectedWeekday.toString() || 
                                it.airWeekday?.toString() == selectedWeekday.toString() 
                            }
                        }

                        if (filteredItems.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("当日无更新", color = Color.Gray, fontSize = 16.sp)
                                }
                            }
                        } else {
                            CalendarList(filteredItems)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekdaySelector(
    selectedWeekday: Int,
    onWeekdaySelected: (Int) -> Unit
) {
    val weekdays = listOf(
        0 to "全部",
        1 to "周一",
        2 to "周二",
        3 to "周三",
        4 to "周四",
        5 to "周五",
        6 to "周六",
        7 to "周日"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1a1a2e)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(weekdays) { (day, name) ->
            FilterChip(
                selected = selectedWeekday == day,
                onClick = { onWeekdaySelected(day) },
                label = {
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = if (selectedWeekday == day) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF16213e),
                    labelColor = Color.White,
                    selectedContainerColor = Color(0xFF4ecca3).copy(alpha = 0.3f),
                    selectedLabelColor = Color(0xFF4ecca3)
                )
            )
        }
    }
}

@Composable
fun CalendarList(items: List<BangumiCalendarItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            BangumiCalendarCard(item)
        }
    }
}

@Composable
fun BangumiCalendarCard(item: BangumiCalendarItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cover image
            AsyncImage(
                model = item.images?.large ?: item.image ?: item.thumb,
                contentDescription = item.nameCn ?: item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 80.dp, height = 110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title
                Text(
                    text = item.nameCn ?: item.name ?: "未知",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Original title
                item.name?.let { name ->
                    if (name != item.nameCn) {
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Air date and weekday
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item.airDate?.let { date ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = date.take(10),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    item.weekday?.let { weekday ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = weekday,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Episode count and duration
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item.eps?.let { eps ->
                        Text(
                            text = "共 $eps 集",
                            fontSize = 11.sp,
                            color = Color(0xFF4ecca3)
                        )
                    }

                    item.duration?.let { duration ->
                        Text(
                            text = duration,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Summary
                item.summary?.let { summary ->
                    if (summary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = summary.take(100) + if (summary.length > 100) "..." else "",
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
