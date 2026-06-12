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
fun BlockUnblockScreen(
    onBack: () -> Unit,
    viewModel: BlockUnblockViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫码封存/解封") },
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
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("扫描看板标签二维码",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("在库可用自动封存，已封存自动解封",
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
                            if (state.result != null) {
                                val r = state.result!!
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("操作结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row { Text("零件: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${r.partName}(${r.partCode})", fontWeight = FontWeight.Medium) }
                                        Row { Text("操作: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(r.action, fontWeight = FontWeight.Bold) }
                                        Row { Text("原状态: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(r.previousStatusText) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.needConfirm) {
                item {
                    Button(
                        onClick = viewModel::confirmContinue,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("确认，继续扫描", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (state.showCamera) {
        QrScanDialog(
            onScanned = { viewModel.onQrScanned(it) },
            onDismiss = { viewModel.hideCamera() }
        )
    }
}
