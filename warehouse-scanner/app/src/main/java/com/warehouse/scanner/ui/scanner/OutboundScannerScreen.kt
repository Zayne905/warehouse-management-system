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
    viewModel: OutboundScannerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

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
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("扫描看板标签二维码", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("扫箱码即出库，FIFO自动选最早入库的箱", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.showCamera() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
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
                    Button(onClick = viewModel::confirmContinue, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Icon(Icons.Default.CheckCircle, null); Spacer(modifier = Modifier.width(8.dp))
                        Text("确认，继续出库", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (state.showCamera) {
        QrScanDialog(onScanned = { viewModel.onQrScanned(it) }, onDismiss = { viewModel.hideCamera() })
    }
}
