package com.warehouse.scanner.model

import com.google.gson.annotations.SerializedName

// ==================== 通用响应 ====================

data class ApiResult<T>(
    val code: Int,
    val message: String,
    val data: T?
)

// ==================== 登录 ====================

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginData(
    val token: String,
    val nickname: String,
    val role: String
)

// ==================== 扫码 ====================

data class ScanCheckRequest(
    val inboundOrderId: Long,
    val partCode: String,
    val batchNo: String
)

data class ScanSubmitRequest(
    val inboundOrderId: Long,
    val inboundOrderNo: String,
    val partCode: String,
    val batchNo: String,
    val scanQty: Double,
    val operatorId: Long
)

// ==================== 看板扫码 ====================

data class KanbanScanRequest(
    val kanbanNo: String,
    val partCode: String,
    val partName: String,
    val supplierName: String,
    val quantity: Int,
    val warehouseArea: String,
    val inboundOrderNo: String,
    val boxSeq: Int,
    val operatorId: Long
)

data class KanbanScanResult(
    val success: Boolean,
    val kanbanNo: String,
    val partCode: String,
    val partName: String,
    val quantity: Int,
    val boxSeq: Int,
    val boxScanned: Int,
    val boxTotal: Int,
    val plannedQty: Double,
    val actualQty: Double,
    val unit: String?,
    val inboundOrderNo: String?,
    val supplierName: String?,
    val warehouseArea: String?,
    val orderStatus: Int,
    val orderStatusText: String
)

// ==================== 出库看板扫码 ====================

data class OutboundScanResult(
    val success: Boolean,
    val kanbanNo: String,
    val partCode: String,
    val partName: String,
    val quantity: Int,
    val plannedQty: Double?,
    val actualQty: Double?
)

data class ToggleBlockResult(
    val kanbanNo: String,
    val partCode: String,
    val partName: String,
    val action: String,
    val previousStatus: Int,
    val previousStatusText: String,
    val newStatus: Int
)

data class ScanFeedbackRequest(
    val orderNo: String,
    val partCode: String? = null
)

data class ScanCheckResult(
    val isDuplicate: Boolean,
    val message: String,
    val remainingQty: Double,
    val plannedQty: Double,
    val scannedTotal: Double
)

data class ScanSubmitResult(
    val scannedTotal: Double,
    val plannedQty: Double,
    val remainingQty: Double,
    val orderStatus: Int,
    val orderStatusText: String
)

// ==================== 入库单 ====================

data class InboundOrderVO(
    val id: Long,
    val orderNo: String,
    val supplierName: String,
    val orderNumber: String?,
    val status: Int,
    val statusText: String,
    val remark: String?,
    val createTime: String,
    val details: List<InboundDetailVO>? = null
)

data class InboundDetailVO(
    val id: Long,
    val partId: Long,
    val partCode: String,
    val partName: String,
    val unit: String?,
    val plannedQty: Double,
    val actualQty: Double,
    val warehouseAreaName: String?,
    val batchNo: String?,
    val lineNo: Int
)

data class ScanRecordVO(
    val id: Long,
    val inboundOrderId: Long,
    val inboundOrderNo: String,
    val partId: Long,
    val partCode: String,
    val partName: String,
    val batchNo: String?,
    val scanQty: Double,
    val scanTime: String
)

// ==================== 扫描条目（本地暂存） ====================

data class ScanItem(
    val orderId: Long,
    val orderNo: String,
    val partCode: String,
    val partName: String,
    val batchNo: String,
    val scanQty: Double,
    val plannedQty: Double,
    var submitted: Boolean = false
)

// ==================== 转包 ====================

/** 预览看板 */
data class RepackPreviewData(
    val kanbanNo: String,
    val partId: Long,
    val partCode: String,
    val partName: String,
    val quantity: Int,
    val warehouseAreaId: Long?,
    val warehouseAreaName: String?,
    val supplierName: String?,
    val status: Int,
    val statusText: String,
    val inboundOrderNo: String?,
    val transferableQty: Int,
    val hasTransferHistory: Boolean,
    val transferOutCount: Int,
    val transferInCount: Int
)

/** 转包单 */
data class RepackOrderData(
    val id: Long,
    val orderNo: String,
    val status: Int,
    val statusText: String,
    val repackType: String,
    val repackTypeText: String,
    val partId: Long?,
    val partCode: String?,
    val partName: String?,
    val warehouseAreaId: Long?,
    val warehouseAreaName: String?,
    val detailCount: Int,
    val totalTransferQty: Int,
    val remark: String?,
    val details: List<RepackDetailData>?
)

data class RepackDetailData(
    val id: Long,
    val sourceKanbanNo: String,
    val transferQty: Int,
    val targetKanbanNo: String?,
    val partCode: String?,
    val partName: String?,
    val sourceRemainingQty: Int?,
    val lineNo: Int
)

// ==================== 溯源 ====================

data class TraceData(
    val kanbanNo: String,
    val currentKanban: TraceKanbanInfo?,
    val parentChain: List<TraceNode>?,
    val childChain: List<TraceNode>?
)

data class TraceKanbanInfo(
    val kanbanNo: String,
    val partCode: String,
    val partName: String,
    val quantity: Int,
    val warehouseAreaName: String?,
    val supplierName: String?,
    val statusText: String,
    val inboundOrderNo: String?,
    val createTime: String?
)

data class TraceNode(
    val relationId: Long,
    val parentKanbanNo: String,
    val childKanbanNo: String,
    val repackOrderNo: String?,
    val transferQty: Int,
    val partCode: String?,
    val partName: String?,
    val repackTime: String?,
    val level: Int
)
