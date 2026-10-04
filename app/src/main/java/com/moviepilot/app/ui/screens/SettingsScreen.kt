package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

/**
 * PWA 风格设置页面
 * 所有设置都跳转到 PWA WebView 页面
 */
@Composable
fun SettingsScreen(
    onNavigateToDetail: (String, String) -> Unit = { _, _ -> }
) {
    // PWA 设置项 - 直接跳转到 PWA WebView 页面
    val settingsItems = listOf(
        SettingsItem("系统设置", Icons.Default.Settings, "基础、AI、媒体、网络等设置"),
        SettingsItem("目录设置", Icons.Default.Home, "存储、整理目录配置"),
        SettingsItem("站点设置", Icons.Default.Star, "CookieCloud、站点管理"),
        SettingsItem("用户管理", Icons.Default.Person, "用户账号管理"),
        SettingsItem("插件设置", Icons.Default.Build, "插件配置"),
        SettingsItem("通知设置", Icons.Default.Notifications, "通知渠道、模板")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16213e))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "设置",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            Text(
                text = "PWA 设置",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                letterSpacing = 1.sp
            )
        }

        items(settingsItems.size) { index ->
            SettingsItemCard(settingsItems[index]) { item ->
                // 跳转到 PWA 设置页面：传递标题和路径
                onNavigateToDetail(item.title, item.path)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

data class SettingsItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
) {
    // 根据标题映射到 PWA 路由
    val path: String
        get() = when (title) {
            "系统设置" -> "/#/setting"
            "目录设置" -> "/#/directory"
            "站点设置" -> "/#/site"
            "用户管理" -> "/#/user"
            "插件设置" -> "/#/plugin"
            "通知设置" -> "/#/notification"
            else -> "/#/setting"
        }
}

@Composable
private fun SettingsItemCard(item: SettingsItem, onClick: (SettingsItem) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color(0xFF4ecca3),
                modifier = Modifier.size(28.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "进入",
                tint = Color.Gray
            )
        }
    }
}
