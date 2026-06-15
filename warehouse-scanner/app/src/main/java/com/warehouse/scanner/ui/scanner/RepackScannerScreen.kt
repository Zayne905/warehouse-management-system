package com.warehouse.scanner.ui.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
                        "BREAKDOWN" -> "向下拆包"
                        "CONSOLIDATE" -> "向上合并"
                        else -> "带余量转包"
                    })
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
                },
                actions = {
                    if (state.orderStatus == 0 && state.mode != "init" && state.repackType != "BREAKDOWN") {
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

            // 第一步：选择转包类型（初始状态）
            if (state.mode == "init") {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("选择转包类型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            // 带余量转包
                            Card(modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (state.repackType == "REMAINDER") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                                onClick = { viewModel.setRepackType("REMAINDER") }) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CallSplit, null, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("带余量转包", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("扫1个源箱→输入转出数量→生成1个新包装，源箱保留余量", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // 向下拆包
                            Card(modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (state.repackType == "BREAKDOWN") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                                onClick = { viewModel.setRepackType("BREAKDOWN") }) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CallMade, null, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("向下拆包", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("扫1个源箱→设目标箱容量→自动均匀拆分为多箱", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // 向上合并
                            Card(modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (state.repackType == "CONSOLIDATE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                                onClick = { viewModel.setRepackType("CONSOLIDATE") }) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CallReceived, null, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("向上合并", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("逐个扫多个源箱→合并为1个新包装", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.showCamera() }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Icon(Icons.Default.QrCodeScanner, null); Spacer(modifier = Modifier.width(8.dp))
                                Text("开始扫码")
                            }
                        }
                    }
                }
            }

            // 扫码入口（已选类型，无预览时）
            if (state.mode != "init" && state.mode != "done" && state.mode != "generated"
                && state.preview == null && state.orderStatus == 0) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("扫源包装二维码", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (state.repackType == "BREAKDOWN") "向下拆包：扫一个源箱→设目标容量→自动拆为多箱"
                                else if (state.repackType == "CONSOLIDATE") "逐个扫要合并的箱子，自动取全部剩余"
                                else "扫要转包的箱子，拖动滑块设置转出数量",
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

            // 预览结果（扫描后，BREAKDOWN/REMAINDER/CONSOLIDATE 不同交互）
            if (state.preview != null && state.orderStatus == 0 && state.mode == "scanned") {
                val p = state.preview!!
                val isBreakdown = state.repackType == "BREAKDOWN"
                val isConsolidate = state.repackType == "CONSOLIDATE"

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("扫描结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row { Text("看板: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.kanbanNo.takeLast(24)) }
                            Row { Text("物料: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${p.partCode} ${p.partName}", fontWeight = FontWeight.Medium) }
                            Row { Text("库区: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.warehouseAreaName ?: "-") }
                            Row { Text("状态: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(p.statusText) }
                            Row { Text("可转数量: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${p.transferableQty} 件", fontWeight = FontWeight.Bold) }
                            if (p.hasTransferHistory) {
                                Row { Text("转包历史: ", color = MaterialTheme.colorScheme.primary); Text("转出${p.transferOutCount}次 / 转入${p.transferInCount}次") }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (isBreakdown) {
                                // 向下转包：显示全拆提示 + 目标箱容量预填
                                Text("源箱共 ${p.transferableQty} 件，将全部拆分", style = MaterialTheme.typography.bodyMedium)
                            } else if (!isConsolidate) {
                                // REMAINDER：滑动条选数量
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
                                OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.weight(1f)) {
                                    Text("重扫")
                                }
                                Button(onClick = { viewModel.addDetail() }, modifier = Modifier.weight(1f)) {
                                    Text(if (isBreakdown) "确认选择此源箱" else "确认添加")
                                }
                            }
                        }
                    }
                }
            }

            // BREAKDOWN：源箱已添加 → 设置目标箱容量 + 自动拆包
            if (state.repackType == "BREAKDOWN" && state.mode == "added") {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("拆包设置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("源箱已选择，请设置每箱的目标容量，系统将自动计算需要拆成几箱。", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = if (state.targetBoxCapacity > 0) state.targetBoxCapacity.toString() else "",
                                onValueChange = { v ->
                                    val n = v.filter { it.isDigit() }.take(6).toIntOrNull() ?: 0
                                    viewModel.setTargetBoxCapacity(n)
                                },
                                label = { Text("目标箱容量（件/箱）") },
                                placeholder = { Text("例如：50") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 预览拆分结果
                            if (state.targetBoxCapacity > 0) {
                                val total = state.preview?.transferableQty ?: 0
                                val boxCount = maxOf(2, (total + state.targetBoxCapacity - 1) / state.targetBoxCapacity)
                                val lastBoxQty = total - state.targetBoxCapacity * (boxCount - 1)
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("拆分预览", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Row { Text("总数量: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${total} 件") }
                                        Row { Text("拆分结果: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${boxCount} 箱") }
                                        val previewText = if (boxCount > 1) {
                                            "${state.targetBoxCapacity} ×${boxCount - 1} + $lastBoxQty ×1"
                                        } else "$total ×1"
                                        Row { Text("分配: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(previewText, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.weight(1f)) {
                                    Text("重新选择源箱")
                                }
                                Button(onClick = { viewModel.breakdownGenerate() }, modifier = Modifier.weight(1f),
                                    enabled = state.targetBoxCapacity > 0) {
                                    Text("自动拆包")
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

            // 确认按钮（REMAINDER/CONSOLIDATE 添加后，或 BREAKDOWN 生成后）
            if (state.needConfirm && state.repackType != "BREAKDOWN") {
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

            // BREAKDOWN 生成后的确认
            if (state.needConfirm && state.repackType == "BREAKDOWN" && state.mode == "generated") {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.weight(1f)) {
                            Text("重新来")
                        }
                        Button(onClick = { viewModel.confirmRepack() }, modifier = Modifier.weight(1f)) {
                            Text("确认执行拆包")
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
                            OutlinedButton(onClick = { viewModel.reset() }) { Text("开始新转包") }
                        }
                    }
                }
            }
        }
    }

    // 相机扫码
    if (state.showCamera) {
        CameraScanDialog(
            onBarcodeScanned = { result -> viewModel.onBarcodeScanned(result) },
            onDismiss = { viewModel.hideCamera() }
        )
    }
}

private fun typeLabel(t: String) = when (t) {
    "BREAKDOWN" -> "向下拆包"
    "CONSOLIDATE" -> "向上合并"
    else -> "带余量转包"
}
