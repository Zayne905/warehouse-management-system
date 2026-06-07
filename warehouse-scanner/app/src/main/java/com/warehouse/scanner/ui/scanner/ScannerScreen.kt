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
fun ScannerScreen(
    onBack: () -> Unit,
    viewModel: ScannerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫码入库") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 步骤1: 扫描 / 输入入库单号
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("步骤 1: 选择入库单",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.orderNo,
                            onValueChange = viewModel::onOrderNoChange,
                            label = { Text("入库单号") },
                            placeholder = { Text("扫描看板二维码或手动输入") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.showCamera() }) {
                                    Icon(Icons.Default.QrCodeScanner, "扫描")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::loadOrder,
                            enabled = state.orderNo.isNotBlank() && !state.loading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("加载入库单")
                        }
                    }
                }
            }

            // 提示 / 错误信息
            if (state.message.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = if (state.error) MaterialTheme.colorScheme.errorContainer
                        else if (state.completed) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    )) {
                        Text(text = state.message, modifier = Modifier.padding(16.dp))
                    }
                }
            }

            // 步骤2: 订单信息
            if (state.orderInfo.isNotEmpty()) {
                item {
                    Text("步骤 2: 确认入库数量",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                item {
                    Card {
                        Text(text = state.orderInfo, modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // 零件清单
                items(state.parts) { part ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${part.partCode} ${part.partName}", fontWeight = FontWeight.Medium)
                                    Text("计划: ${fmt(part.plannedQty)} ${part.unit}  |  已收: ${fmt(part.alreadyReceived)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedTextField(
                                    value = part.receiveQty,
                                    onValueChange = { viewModel.onReceiveQtyChange(part.partCode, it) },
                                    label = { Text("入库数") },
                                    singleLine = true,
                                    modifier = Modifier.width(100.dp)
                                )
                            }
                        }
                    }
                }

                // 提交按钮
                item {
                    Button(
                        onClick = viewModel::submitAll,
                        enabled = !state.submitting && state.parts.any {
                            (it.receiveQty.toDoubleOrNull() ?: 0.0) > 0 },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        if (state.submitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("确认入库", style = MaterialTheme.typography.titleMedium)
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
