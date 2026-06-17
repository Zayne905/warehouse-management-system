package com.warehouse.scanner.ui.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundScannerScreen(
    onBack: () -> Unit,
    outboundVM: OutboundScannerViewModel = viewModel()
) {
    val state by outboundVM.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("看板扫码出库") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { outboundVM.showCamera() }) {
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
            // 出库单选择区域
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("选择出库单", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.orderNoInput,
                                onValueChange = { outboundVM.updateOrderNo(it) },
                                label = { Text("出库单号") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                enabled = !state.loadingOrder
                            )
                            Button(
                                onClick = { outboundVM.queryOrder() },
                                enabled = !state.loadingOrder && state.orderNoInput.isNotBlank()
                            ) { Text("查询") }
                        }
                        // 已选择的出库单信息
                        if (state.selectedOrderNo != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("当前: ${state.selectedOrderNo}", fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium)
                                        state.orderInfo?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                    TextButton(onClick = { outboundVM.clearOrder() }) { Text("清除") }
                                }
                            }
                        }
                    }
                }
            }

            // 扫码区域
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("扫描看板标签二维码", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (state.orderId != null) "已选单，可扫任意在库看板出库" else "请先查询出库单，再扫描看板出库",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { outboundVM.showCamera() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Icon(Icons.Default.QrCodeScanner, null); Spacer(modifier = Modifier.width(8.dp))
                            Text("打开扫码", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            if (state.message.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = if (state.error) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(state.message, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            if (state.progress != null) {
                                val p = state.progress!!
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("出库信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row { Text("零件号: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.partCode, fontWeight = FontWeight.Medium) }
                                        Row { Text("零件名: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.partName) }
                                        Row { Text("供应商: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.supplierName) }
                                        Row { Text("库区: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.warehouseArea) }
                                        Row { Text("数量: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${p.quantity}", fontWeight = FontWeight.Bold) }
                                        Row { Text("箱号: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("C-${p.boxSeq}") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.needConfirm) {
                item {
                    Button(onClick = outboundVM::confirmContinue, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Icon(Icons.Default.CheckCircle, null); Spacer(modifier = Modifier.width(8.dp))
                        Text("确认，继续出库", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // 非FIFO确认提示
            if (state.needsConfirmNonFifo) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("⚠️ 非FIFO出库确认", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.message, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { outboundVM.cancelNonFifoScan() },
                                    modifier = Modifier.weight(1f)
                                ) { Text("取消") }
                                Button(
                                    onClick = { outboundVM.confirmNonFifoScan() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) { Text("确认出库") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showCamera) {
        QrScanDialog(
            onScanned = { outboundVM.onQrScanned(it) },
            onDismiss = { outboundVM.hideCamera() }
        )
    }
}
