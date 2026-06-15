package com.warehouse.scanner.ui.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraceScreen(onBack: () -> Unit, viewModel: TraceViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("看板生命周期查询") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
            actions = { IconButton(onClick = viewModel::showCamera) { Icon(Icons.Default.QrCodeScanner, "扫码") } }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.kanbanNo,
                    onValueChange = viewModel::setKanbanNo,
                    label = { Text("看板号") },
                    placeholder = { Text("输入或扫描看板号") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.query() }, enabled = !state.loading, modifier = Modifier.weight(1f)) {
                        Text(if (state.loading) "查询中" else "查询")
                    }
                    OutlinedButton(onClick = viewModel::showCamera, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.QrCodeScanner, null); Spacer(Modifier.width(6.dp)); Text("扫码")
                    }
                }
            }
            if (state.message.isNotEmpty()) item {
                Text(state.message, color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            state.result?.let { result ->
                val k = result.kanban
                item {
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("看板信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Info("看板号", k.kanbanNo)
                            Info("零件", "${k.partCode} ${k.partName}")
                            Info("状态", k.statusText)
                            Info("数量", k.quantity.toString())
                            Info("入库单", k.inboundOrderNo ?: "-")
                            Info("供应商", k.supplierName ?: "-")
                            Info("仓库/库位", "${k.warehouseName ?: "-"} / ${k.warehouseAreaName ?: "-"}")
                            Info("器具型号", k.containerModel ?: "-")
                            Info("创建/入库", "${k.createTime ?: "-"} / ${k.inboundTime ?: "-"}")
                            Info("出库单/时间", "${k.outboundOrderNo ?: "-"} / ${k.outboundTime ?: "-"}")
                            Info("转包记录", "${k.repackRecords?.size ?: 0} 条")
                        }
                    }
                }
                item { Text("生命周期记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(result.events) { event ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text(event.title, fontWeight = FontWeight.Bold)
                            Text(event.orderNo ?: "-", style = MaterialTheme.typography.bodySmall)
                            Text(event.time ?: "-", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    if (state.showCamera) {
        CameraScanDialog(
            onBarcodeScanned = viewModel::onBarcodeScanned,
            onDismiss = viewModel::hideCamera
        )
    }
}

@Composable
private fun Info(label: String, value: String) {
    Row { Text("$label: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value) }
}
