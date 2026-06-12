package com.warehouse.scanner.ui.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    viewModel: ScannerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("看板扫码入库") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 手动输入兜底
                    IconButton(onClick = { viewModel.showCamera() }) {
                        Icon(Icons.Default.QrCodeScanner, "扫码")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 扫码按钮
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("扫描看板标签二维码",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("每箱一个码，扫码即入库",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.showCamera() },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("打开扫码", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // 手动输入兜底
            item {
                OutlinedTextField(
                    value = state.orderNo,
                    onValueChange = viewModel::onOrderNoChange,
                    label = { Text("或手动输入入库单号") },
                    placeholder = { Text("R20260611001") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (state.orderNo.isNotBlank() && !state.loading) {
                            IconButton(onClick = viewModel::loadOrder) {
                                Icon(Icons.Default.Search, "查询")
                            }
                        }
                    }
                )
                if (state.orderInfo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(state.orderInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 状态消息
            if (state.message.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = if (state.error) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            // 显示扫描进度
                            if (state.progress != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val p = state.progress!!
                                val pct = if (p.boxTotal > 0) p.boxScanned.toFloat() / p.boxTotal else 0f
                                Text("${p.partName}: ${p.boxScanned}/${p.boxTotal} 箱已入库",
                                    style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 确认按钮（需确认后才能继续）
            if (state.needConfirm) {
                item {
                    Button(
                        onClick = viewModel::confirmContinue,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("确认，继续扫下一箱", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    // 相机扫码对话框
    if (state.showCamera) {
        QrScanDialog(
            onScanned = { viewModel.onQrScanned(it) },
            onDismiss = { viewModel.hideCamera() }
        )
    }
}
