package com.wiotracker.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiotracker.R
import com.wiotracker.data.database.AppDatabase
import com.wiotracker.data.repository.WifiScanRepository
import com.wiotracker.util.DebugLogManager
import com.wiotracker.util.LogLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    viewModel: LogViewModel? = null
) {
    val context = LocalContext.current
    val actualViewModel = viewModel ?: remember {
        val database = AppDatabase.getDatabase(context)
        LogViewModel(WifiScanRepository(database.wifiScanDao()))
    }
    val uiState by actualViewModel.uiState.collectAsState()
    val debugLogs by actualViewModel.debugLogs.collectAsState()

    // Show detail dialog when a session is selected
    uiState.selectedSession?.let { session ->
        ScanSessionDetailDialog(
            session = session,
            onDismiss = { actualViewModel.clearSelectedSession() },
            formatTimestamp = { actualViewModel.formatTimestamp(it) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.log)) },
                actions = {
                    if (uiState.currentTab == LogTab.DEBUG_LOG) {
                        IconButton(onClick = { actualViewModel.clearDebugLogs() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton(
                    text = "扫描记录",
                    selected = uiState.currentTab == LogTab.SCAN_RECORDS,
                    onClick = { actualViewModel.setCurrentTab(LogTab.SCAN_RECORDS) },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "调试日志",
                    selected = uiState.currentTab == LogTab.DEBUG_LOG,
                    onClick = { actualViewModel.setCurrentTab(LogTab.DEBUG_LOG) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Content based on selected tab
            when (uiState.currentTab) {
                LogTab.SCAN_RECORDS -> {
                    ScanRecordsContent(
                        uiState = uiState,
                        viewModel = actualViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                LogTab.DEBUG_LOG -> {
                    DebugLogContent(
                        logs = debugLogs,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(text)
    }
}

@Composable
private fun ScanRecordsContent(
    uiState: LogUiState,
    viewModel: LogViewModel,
    modifier: Modifier = Modifier
) {
    if (uiState.scanSessions.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = stringResource(R.string.no_records),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "扫描记录将显示在这里",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.scanSessions) { session ->
                ScanSessionItem(
                    session = session,
                    timestamp = viewModel.formatTimestamp(session.timestamp),
                    onClick = { viewModel.selectSession(session) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DebugLogContent(
    logs: List<com.wiotracker.util.DebugLogEntry>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }
    
    if (logs.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "暂无调试日志",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "后台扫描执行时会显示调试信息",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(logs.size) { index ->
                val log = logs[index]
                DebugLogItem(
                    log = log,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DebugLogItem(
    log: com.wiotracker.util.DebugLogEntry,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (log.level) {
        LogLevel.DEBUG -> MaterialTheme.colorScheme.surface
        LogLevel.INFO -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        LogLevel.WARN -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        LogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    }
    
    val textColor = when (log.level) {
        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurface
        LogLevel.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
        LogLevel.WARN -> MaterialTheme.colorScheme.onErrorContainer
        LogLevel.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = log.formatMessage(),
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = textColor,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun ScanSessionItem(
    session: ScanSession,
    timestamp: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "扫描时间: $timestamp",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = when (session.scanType) {
                                "manual" -> MaterialTheme.colorScheme.secondaryContainer
                                "periodic" -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (session.scanType == "manual") "手动" else "定时",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (session.scanType) {
                                    "manual" -> MaterialTheme.colorScheme.onSecondaryContainer
                                    "periodic" -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "匹配关键词: ${session.matchedKeyword}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${session.wifiCount}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "个WiFi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ScanSessionDetailDialog(
    session: ScanSession,
    onDismiss: () -> Unit,
    formatTimestamp: (Long) -> String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("扫描详情")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "扫描时间: ${formatTimestamp(session.timestamp)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "扫描类型:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = when (session.scanType) {
                            "manual" -> MaterialTheme.colorScheme.secondaryContainer
                            "periodic" -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (session.scanType == "manual") "手动" else "定时",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (session.scanType) {
                                "manual" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "periodic" -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Text(
                    text = "匹配关键词: ${session.matchedKeyword}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "匹配数量: ${session.wifiCount} 个",
                    style = MaterialTheme.typography.bodyMedium
                )
                Divider()
                Text(
                    text = "匹配的WiFi网络:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                session.wifiNames.forEach { wifiName ->
                    Text(
                        text = "• $wifiName",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
