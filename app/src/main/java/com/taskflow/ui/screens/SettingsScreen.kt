package com.taskflow.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.taskflow.BuildConfig
import com.taskflow.update.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateManager.UpdateResult?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "应用信息") {
                SettingsItem(
                    title = "应用名称",
                    subtitle = "TaskFlow",
                    icon = null
                )
                SettingsItem(
                    title = "当前版本",
                    subtitle = BuildConfig.VERSION_NAME,
                    icon = null
                )
            }

            SettingsSection(title = "更新") {
                SettingsItem(
                    title = "检查更新",
                    subtitle = if (isChecking) "正在检查..." else "点击检查新版本",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        if (isChecking) return@SettingsItem
                        scope.launch {
                            isChecking = true
                            val result = UpdateManager.checkForUpdate(context)
                            updateResult = result
                            isChecking = false
                            if (result.hasUpdate) {
                                showUpdateDialog = true
                            }
                        }
                    }
                )
            }

            SettingsSection(title = "关于") {
                SettingsItem(
                    title = "关于软件",
                    subtitle = "TaskFlow - 高效任务管理工具",
                    icon = Icons.Default.Info
                )
                SettingsItem(
                    title = "开发者信息",
                    subtitle = "TaskFlow Team",
                    icon = null
                )
                SettingsItem(
                    title = "GitHub 仓库",
                    subtitle = "github.com/your-username/TaskFlow",
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/your-username/TaskFlow")
                        )
                        context.startActivity(intent)
                    }
                )
                SettingsItem(
                    title = "开源协议",
                    subtitle = "MIT License",
                    icon = null,
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/your-username/TaskFlow/blob/main/LICENSE")
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }

    if (showUpdateDialog && updateResult != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("发现新版本 ${updateResult!!.versionName}") },
            text = {
                Column {
                    Text("更新内容：")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        updateResult!!.changelog,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        scope.launch {
                            val file = UpdateManager.downloadApk(
                                context,
                                updateResult!!.downloadUrl,
                                updateResult!!.apkHash
                            )
                            file?.let { UpdateManager.installApk(context, it) }
                        }
                    }
                ) {
                    Text("立即下载")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("稍后")
                }
            }
        )
    }

    if (!showUpdateDialog && isChecking.not() && updateResult != null && !updateResult!!.hasUpdate) {
        LaunchedEffect(updateResult) {
            // Could show a "no update" snackbar here
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Divider(
        modifier = Modifier.padding(start = if (icon != null) 56.dp else 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
