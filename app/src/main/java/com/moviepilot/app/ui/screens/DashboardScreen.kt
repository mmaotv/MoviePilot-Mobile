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
import com.moviepilot.app.data.repository.DashboardData
import com.moviepilot.app.ui.viewmodel.DashboardViewModel
import java.text.DecimalFormat

// 品牌强调色（与 Theme.kt 中 tertiary 保持一致）
private val BrandGreen = Color(0xFF4ecca3)
private val BrandRed   = Color(0xFFff6b6b)
private val BrandYellow = Color(0xFFffd93d)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val dashboardState by viewModel.dashboardState.collectAsState()

    when (val state = dashboardState) {
        is DashboardViewModel.DashboardState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
            }
        }
        is DashboardViewModel.DashboardState.Success -> {
            DashboardContent(state.data)
        }
        is DashboardViewModel.DashboardState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadDashboardData() }) {
                        Text("重试")
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardContent(data: DashboardData) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        data.storage?.let { storage ->
            item { StorageCard(storage) }
        }

        data.statistics?.let { stats ->
            item { StatisticsCard(stats) }
        }

        data.downloaderInfo?.let { info ->
            item { DownloaderInfoCard(info) }
        }

        if (data.cpuUsage != null || data.memoryUsage != null) {
            item { SystemResourceCard(data.cpuUsage, data.memoryUsage) }
        }

        data.transferStats?.let { stats ->
            item { TransferStatsCard(stats) }
        }

        data.schedule?.let { scheduleList ->
            if (scheduleList.isNotEmpty()) {
                item { ScheduleCard(scheduleList) }
            }
        }
    }
}

@Composable
fun StorageCard(storage: com.moviepilot.app.data.model.StorageInfo) {
    val df = DecimalFormat("#.##")
    val totalGB = storage.totalStorage?.let { it / 1024.0 / 1024 / 1024 / 1024 } ?: 0.0
    val usedGB = storage.usedStorage?.let { it / 1024.0 / 1024 / 1024 / 1024 } ?: 0.0
    val percentage = if (storage.totalStorage != null && storage.totalStorage > 0) {
        ((storage.usedStorage ?: 0.0) / storage.totalStorage * 100).toInt()
    } else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "存储空间",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = percentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = BrandGreen,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "已用: ${df.format(usedGB)} TB",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "总计: ${df.format(totalGB)} TB",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$percentage%",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BrandGreen,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun StatisticsCard(stats: com.moviepilot.app.data.model.MediaStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "媒体统计",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("电影", "${stats.movieCount}", BrandGreen)
                StatItem("电视剧", "${stats.tvCount}", BrandRed)
                StatItem("用户", "${stats.userCount}", BrandYellow)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TransferItem(label: String, speed: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = speed,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SystemStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DownloaderInfoCard(info: com.moviepilot.app.data.model.DownloaderInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "下载器状态",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TransferItem("下载", formatDoubleSpeed(info.downloadSpeed ?: 0.0), BrandGreen)
                TransferItem("上传", formatDoubleSpeed(info.uploadSpeed ?: 0.0), BrandRed)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                info.downloadSize?.let {
                    StatItem("下载量", formatDoubleSize(it), BrandGreen)
                }
                info.uploadSize?.let {
                    StatItem("上传量", formatDoubleSize(it), BrandRed)
                }
                info.freeSpace?.let {
                    StatItem("剩余空间", formatDoubleSize(it), BrandYellow)
                }
            }
        }
    }
}

@Composable
fun SystemResourceCard(cpuUsage: Float?, memoryUsage: List<Int>?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "系统资源",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                cpuUsage?.let {
                    SystemStatItem("CPU", String.format("%.1f%%", it), BrandGreen)
                }
                memoryUsage?.let { mem ->
                    if (mem.size >= 2) {
                        SystemStatItem("内存", "${mem[1]}%", BrandYellow)
                    }
                }
            }
        }
    }
}

@Composable
fun TransferStatsCard(stats: List<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "传输统计",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Display transfer stats as a simple list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stats.forEachIndexed { index, value ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "项目 ${index + 1}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$value",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleCard(scheduleList: List<com.moviepilot.app.data.model.ScheduleInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "计划任务",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            scheduleList.take(5).forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.name ?: "未知任务",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        task.nextRun?.let {
                            Text(
                                text = "下次运行: $it",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    task.status?.let {
                        Text(
                            text = it,
                            fontSize = 11.sp,
                            color = BrandGreen
                        )
                    }
                }
            }
        }
    }
}

fun formatDoubleSpeed(speed: Double): String {
    return when {
        speed >= 1024 * 1024 -> String.format("%.2f MB/s", speed / 1024 / 1024)
        speed >= 1024 -> String.format("%.2f KB/s", speed / 1024)
        else -> String.format("%.0f B/s", speed)
    }
}

fun formatDoubleSize(size: Double): String {
    return when {
        size >= 1024 * 1024 * 1024 * 1024 -> String.format("%.2f TB", size / 1024 / 1024 / 1024 / 1024)
        size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / 1024 / 1024 / 1024)
        size >= 1024 * 1024 -> String.format("%.2f MB", size / 1024 / 1024)
        size >= 1024 -> String.format("%.2f KB", size / 1024)
        else -> String.format("%.0f B", size)
    }
}
