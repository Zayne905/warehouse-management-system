package com.warehouse.scanner.ui.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun TraceScreen(
    onBack: () -> Unit,
    viewModel: TraceViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫码溯源") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
                },
                actions = {
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

            // 扫码卡片
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TravelExplore, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("扫描任意看板二维码", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("追溯完整生命周期：入库→转包→出库", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.showCamera() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Icon(Icons.Default.QrCodeScanner, null); Spacer(modifier = Modifier.width(8.dp))
                            Text("打开扫码", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // 消息
            if (state.message.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = if (state.error) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )) {
                        Text(state.message, modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // 当前看板
            val r = state.result
            if (r != null && r.currentKanban != null) {
                val k = r.currentKanban!!
                item {
                    Text("当前看板", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row { Text("看板号: ", fontWeight = FontWeight.Bold); Text(k.kanbanNo.takeLast(20)) }
                            Row { Text("物料: "); Text("${k.partCode} ${k.partName}", fontWeight = FontWeight.Bold) }
                            Row { Text("数量: "); Text("${k.quantity} 件") }
                            Row { Text("库区: "); Text(k.warehouseAreaName ?: "-") }
                            Row { Text("状态: "); Text(k.statusText, fontWeight = FontWeight.Bold) }
                            Row { Text("入库单: "); Text(k.inboundOrderNo ?: "-") }
                            Row { Text("供应商: "); Text(k.supplierName ?: "-") }
                        }
                    }
                }

                // 向上追溯（来源）
                if (!r.parentChain.isNullOrEmpty()) {
                    item {
                        Text("⬆ 向上追溯（来源）: ${r.parentChain.size} 条",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    items(r.parentChain) { node ->
                        Card {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("层级${node.level}: ${node.parentKanbanNo.takeLast(16)} → ${node.childKanbanNo.takeLast(16)}",
                                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Text("转出: ${node.transferQty} 件 | 转包单: ${node.repackOrderNo ?: "-"} | ${node.repackTime ?: ""}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 向下追溯（去往）
                if (!r.childChain.isNullOrEmpty()) {
                    item {
                        Text("⬇ 向下追溯（去往）: ${r.childChain.size} 条",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    items(r.childChain) { node ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("层级${node.level}: → ${node.childKanbanNo.takeLast(16)}",
                                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Text("转出: ${node.transferQty} 件 | 转包单: ${node.repackOrderNo ?: "-"} | ${node.repackTime ?: ""}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
            onBarcodeScanned = { result -> viewModel.traceByKanban(result) },
            onDismiss = { viewModel.hideCamera() }
        )
    }
}
