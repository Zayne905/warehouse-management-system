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
fun RepackScannerScreen(
    onBack: () -> Unit,
    viewModel: RepackScannerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (state.repackType) {
                        "BREAKDOWN" -> "扫码拆包"
                        "CONSOLIDATE" -> "扫码合并"
                        else -> "扫码转包"
                    })
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
                },
                actions = {
                    if (state.orderStatus == 0 && state.mode != "init") {
                        IconButton(onClick = { viewModel.showCamera() }) {
                            Icon(Icons.Default.QrCodeScanner, "扫码")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 转包单信息
            if (state.orderNo.isNotBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("转包单", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row { Text("单号: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(state.orderNo, fontWeight = FontWeight.Medium) }
                            Row { Text("类型: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(typeLabel(state.repackType)) }
                            Row { Text("状态: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(state.orderStatusText, fontWeight = FontWeight.Bold) }
                            Row { Text("已添加: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${state.detailCount} 行 / ${state.totalQty} 件") }
                        }
                    }
                }
            }

            // 扫码或输入看板号 + 数量
            if (state.orderStatus == 0 && state.preview == null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("扫源包装二维码", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (state.repackType == "CONSOLIDATE") "逐个扫要合并的箱子，自动取全部剩余"
                                else "扫要转包的箱子",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.showCamera() }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Icon(Icons.Default.QrCodeScanner, null); Spacer(modifier = Modifier.width(8.dp))
                                Text("打开扫码")
                            }
                        }
                    }
                }
            }

            // 预览结果 + 数量输入
            if (state.preview != null && state.orderStatus == 0) {
                val p = state.preview!!
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("扫描结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row { Text("看板: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.kanbanNo.takeLast(20)) }
                            Row { Text("物料: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${p.partCode} ${p.partName}", fontWeight = FontWeight.Medium) }
                            Row { Text("数量: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${p.transferableQty} 件", fontWeight = FontWeight.Bold) }
                            Row { Text("库区: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.warehouseAreaName ?: "-") }
                            Row { Text("状态: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.statusText) }
                            if (p.hasTransferHistory) {
                                Row { Text("转包历史: ", color = MaterialTheme.colorScheme.primary); Text("转出${p.transferOutCount}次 / 转入${p.transferInCount}次") }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // 数量输入（带余量转包需手动输入，合并自动填满）
                            if (state.repackType != "CONSOLIDATE") {
                                Text("转出数量:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Slider(
                                        value = state.scanQty.toFloat(),
                                        onValueChange = { viewModel.setScanQty(it.toInt()) },
                                        valueRange = 1f..p.transferableQty.toFloat(),
                                        steps = p.transferableQty - 2,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${state.scanQty}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("自动取全部剩余: ${p.transferableQty} 件", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.hideCamera(); viewModel.reset() }, modifier = Modifier.weight(1f)) {
                                    Text("重扫")
                                }
                                Button(onClick = { viewModel.addDetail() }, modifier = Modifier.weight(1f)) {
                                    Text("确认添加")
                                }
                            }
                        }
                    }
                }
            }

            // 消息
            if (state.message.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = if (state.error) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(state.message, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // 确认继续
            if (state.needConfirm) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.confirmContinue() }, modifier = Modifier.weight(1f)) {
                            Text("继续添加")
                        }
                        Button(onClick = { viewModel.confirmRepack() }, modifier = Modifier.weight(1f)) {
                            Text("保存并执行转包")
                        }
                    }
                }
            }

            // 完成状态
            if (state.mode == "done") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("转包完成", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${state.detailCount} 行，合计 ${state.totalQty} 件", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { viewModel.reset() }) {
                                Text("开始新转包")
                            }
                        }
                    }
                }
            }
        }
    }

    // 相机扫码
    if (state.showCamera) {
        CameraScanDialog(
            onBarcodeScanned = { result -> viewModel.previewKanban(result) },
            onDismiss = { viewModel.hideCamera() }
        )
    }
}

private fun typeLabel(t: String) = when (t) {
    "BREAKDOWN" -> "向下拆包"
    "CONSOLIDATE" -> "向上合并"
    else -> "带余量转包"
}
