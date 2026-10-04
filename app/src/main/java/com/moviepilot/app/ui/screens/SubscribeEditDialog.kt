package com.moviepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.moviepilot.app.data.model.Subscribe
import com.moviepilot.app.data.model.SubscribeRequest

@Composable
fun SubscribeEditDialog(
    subscription: Subscribe?,
    onDismiss: () -> Unit,
    onSave: (SubscribeRequest) -> Unit
) {
    var keyword by remember { mutableStateOf(subscription?.name ?: "") }
    var quality by remember { mutableStateOf("") }
    var resolution by remember { mutableStateOf("") }
    var sites by remember { mutableStateOf("") }
    var savePath by remember { mutableStateOf("") }
    var bestVersion by remember { mutableStateOf(false) }
    var include by remember { mutableStateOf("") }
    var exclude by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1a1a2e)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("编辑订阅", style = MaterialTheme.typography.titleLarge, color = Color.White)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("搜索关键字", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF16213e),
                        unfocusedContainerColor = Color(0xFF16213e)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = quality,
                    onValueChange = { quality = it },
                    label = { Text("质量过滤", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF16213e),
                        unfocusedContainerColor = Color(0xFF16213e)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = resolution,
                    onValueChange = { resolution = it },
                    label = { Text("分辨率过滤", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF16213e),
                        unfocusedContainerColor = Color(0xFF16213e)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = savePath,
                    onValueChange = { savePath = it },
                    label = { Text("保存路径", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF16213e),
                        unfocusedContainerColor = Color(0xFF16213e)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("洗版模式", color = Color.White)
                    Checkbox(
                        checked = bestVersion,
                        onCheckedChange = { bestVersion = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = include,
                    onValueChange = { include = it },
                    label = { Text("包含关键字（正则）", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF16213e),
                        unfocusedContainerColor = Color(0xFF16213e)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = exclude,
                    onValueChange = { exclude = it },
                    label = { Text("排除关键字（正则）", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF16213e),
                        unfocusedContainerColor = Color(0xFF16213e)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消", color = Color.White)
                    }

                    Button(
                        onClick = {
                            val request = SubscribeRequest(
                                name = keyword,
                                year = subscription?.yearValue?.toIntOrNull(),
                                type = subscription?.type ?: "电影",
                                tmdbId = subscription?.tmdbId,
                                doubanId = subscription?.doubanId,
                                season = subscription?.season,
                                quality = quality.ifBlank { null },
                                resolution = resolution.ifBlank { null },
                                include = include.ifBlank { null },
                                exclude = exclude.ifBlank { null },
                                savePath = savePath.ifBlank { null }
                            )
                            onSave(request)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ecca3))
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}
