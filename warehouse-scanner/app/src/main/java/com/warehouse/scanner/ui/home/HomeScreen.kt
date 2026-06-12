package com.warehouse.scanner.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.warehouse.scanner.network.TokenProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScanner: () -> Unit,
    onNavigateToOutboundScanner: () -> Unit,
    onNavigateToBlockUnblock: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("仓储管理") },
                actions = {
                    TextButton(onClick = {
                        TokenProvider.clear()
                        onLogout()
                    }) {
                        Text("退出登录")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 欢迎信息
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "你好, ${TokenProvider.nickname ?: "用户"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "角色: ${if (TokenProvider.role == "admin") "管理员" else "操作员"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("功能菜单", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // 扫码入库入口
            MenuCard(
                icon = Icons.Default.QrCodeScanner,
                title = "扫码入库",
                subtitle = "扫描入库单二维码看板，快速完成入库",
                onClick = onNavigateToScanner
            )

            // 扫码出库入口
            MenuCard(
                icon = Icons.Default.ExitToApp,
                title = "扫码出库",
                subtitle = "扫描看板二维码，FIFO自动出库",
                onClick = onNavigateToOutboundScanner
            )

            // 扫码封存/解封入口
            MenuCard(
                icon = Icons.Default.Lock,
                title = "扫码封存/解封",
                subtitle = "在库可用自动封存，已封存自动解封",
                onClick = onNavigateToBlockUnblock
            )
        }
    }
}

@Composable
private fun MenuCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Default.ChevronRight, "进入",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
