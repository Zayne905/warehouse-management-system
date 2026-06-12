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
                title = { Text(if (state.mode == ScanMode.INBOUND) "看板扫码入库" else "看板扫码出库") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    Text(
                        if (state.mode == ScanMode.INBOUND) "入库" else "出库",
                        color = if (state.mode == ScanMode.INBOUND)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = state.mode == ScanMode.OUTBOUND,
                        onCheckedChange = { viewModel.toggleMode() }
                    )
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

            // 状态消息
            if (state.message.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = if (state.error) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium)
                            // 看板详情卡片
                            if (state.progress != null) {
                                val p = state.progress!!
                                Spacer(modifier = Modifier.height(12.dp))
                                // 订单信息
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("入库单信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row { Text("单号: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.inboundOrderNo, fontWeight = FontWeight.Medium) }
                                        Row { Text("供应商: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.supplierName) }
                                        Row { Text("状态: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.orderStatusText, color = if(p.orderStatus==2) androidx.compose.ui.graphics.Color(0xFF67C23A) else MaterialTheme.colorScheme.primary) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                // 零件信息
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("零件信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row { Text("零件号: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.partCode, fontWeight = FontWeight.Medium) }
                                        Row { Text("零件名: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.partName) }
                                        Row { Text("库区: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.warehouseArea) }
                                        Row { Text("本箱数量: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${p.quantity} ${p.unit}", fontWeight = FontWeight.Bold) }
                                        Row { Text("箱号: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("C-${p.boxSeq}") }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row { Text("计划总数: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(fmt(p.plannedQty)) }
                                        Row { Text("已收合计: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(fmt(p.actualQty)) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                // 收货进度
                                val pct = if (p.boxTotal > 0) p.boxScanned.toFloat() / p.boxTotal else 0f
                                Text("收货进度: ${p.boxScanned}/${p.boxTotal} 箱",
                                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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

private fun fmt(v: Double) = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
