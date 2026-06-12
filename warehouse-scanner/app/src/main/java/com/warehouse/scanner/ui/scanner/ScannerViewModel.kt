package com.warehouse.scanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.warehouse.scanner.model.*
import com.warehouse.scanner.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 看板QR码JSON解析
data class KanbanQrData(
    val kanbanNo: String = "",
    val partCode: String = "",
    val partName: String = "",
    val supplierName: String = "",
    val quantity: Int = 0,
    val warehouseArea: String = "",
    val inboundOrderNo: String = "",
    val boxSeq: Int = 0
)

data class ScanProgress(
    val partCode: String,
    val partName: String,
    val boxScanned: Int,
    val boxTotal: Int,
    val quantity: Int = 0,
    val plannedQty: Double = 0.0,
    val actualQty: Double = 0.0,
    val unit: String = "",
    val inboundOrderNo: String = "",
    val supplierName: String = "",
    val warehouseArea: String = "",
    val boxSeq: Int = 0,
    val orderStatus: Int = 0,
    val orderStatusText: String = ""
)

data class ScannerState(
    val orderNo: String = "",
    val orderInfo: String = "",
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val message: String = "",
    val error: Boolean = false,
    val progress: ScanProgress? = null,
    val lastScannedKanban: String = "",
    val showCamera: Boolean = false,
    val needConfirm: Boolean = false
)

class ScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state
    private val gson = Gson()

    fun onOrderNoChange(v: String) {
        _state.value = _state.value.copy(orderNo = v, message = "", error = false)
    }

    fun showCamera() { _state.value = _state.value.copy(showCamera = true) }
    fun hideCamera() { _state.value = _state.value.copy(showCamera = false) }

    /** 确认后继续扫下一箱 */
    fun confirmContinue() {
        _state.value = _state.value.copy(
            needConfirm = false, message = "", progress = null, lastScannedKanban = ""
        )
    }

    /**
     * 扫码回调 — 智能识别：看板JSON自动入库，普通文本按单号查询
     */
    fun onQrScanned(text: String) {
        val trimmed = text.trim()
        _state.value = _state.value.copy(showCamera = false)

        // 尝试解析为看板JSON
        try {
            val qr = gson.fromJson(trimmed, KanbanQrData::class.java)
            if (qr.kanbanNo.isNotBlank() && qr.partCode.isNotBlank()) {
                // 看板模式：自动入库
                scanKanban(qr)
                return
            }
        } catch (_: Exception) { /* 不是JSON，作为单号处理 */ }

        // 兼容旧版：纯文本单号走原流程
        _state.value = _state.value.copy(orderNo = trimmed)
        loadOrder(trimmed)
    }

    /**
     * 看板扫码入库
     */
    private fun scanKanban(qr: KanbanQrData) {
        viewModelScope.launch {
            _state.value = _state.value.copy(submitting = true, message = "正在入库 ${qr.partName} ...", error = false)
            try {
                val res = RetrofitClient.api.scanKanban(
                    KanbanScanRequest(
                        kanbanNo = qr.kanbanNo,
                        partCode = qr.partCode,
                        partName = qr.partName,
                        supplierName = qr.supplierName,
                        quantity = qr.quantity,
                        warehouseArea = qr.warehouseArea,
                        inboundOrderNo = qr.inboundOrderNo,
                        boxSeq = qr.boxSeq,
                        operatorId = 1 // TODO: 使用当前登录用户ID
                    )
                )
                if (res.code == 200 && res.data != null) {
                    val r = res.data
                    _state.value = _state.value.copy(
                        submitting = false,
                        needConfirm = true,
                        lastScannedKanban = qr.kanbanNo,
                        message = "✅ ${r.partName} C-${r.boxSeq}箱 已入库 (${r.quantity}${r.unit})",
                        progress = ScanProgress(
                            partCode = r.partCode,
                            partName = r.partName,
                            boxScanned = r.boxScanned,
                            boxTotal = r.boxTotal,
                            quantity = r.quantity,
                            plannedQty = r.plannedQty,
                            actualQty = r.actualQty,
                            unit = r.unit ?: "",
                            inboundOrderNo = r.inboundOrderNo ?: "",
                            supplierName = r.supplierName ?: "",
                            warehouseArea = r.warehouseArea ?: "",
                            boxSeq = r.boxSeq,
                            orderStatus = r.orderStatus,
                            orderStatusText = r.orderStatusText ?: ""
                        ),
                        error = false
                    )
                } else {
                    // 失败时也展示QR中的基本信息
                    _state.value = _state.value.copy(
                        submitting = false, error = true,
                        needConfirm = true,
                        lastScannedKanban = qr.kanbanNo,
                        message = "⚠️ ${res.message}",
                        progress = ScanProgress(
                            partCode = qr.partCode,
                            partName = qr.partName,
                            boxScanned = 0,
                            boxTotal = 0,
                            quantity = qr.quantity,
                            unit = "",
                            boxSeq = qr.boxSeq,
                            inboundOrderNo = qr.inboundOrderNo,
                            supplierName = qr.supplierName,
                            warehouseArea = qr.warehouseArea
                        )
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    submitting = false, error = true,
                    message = "网络错误: ${e.localizedMessage}"
                )
            }
        }
    }

    // ==================== 以下为旧版兼容逻辑 ====================

    fun loadOrder() {
        val no = _state.value.orderNo.trim()
        if (no.isEmpty()) return
        loadOrder(no)
    }

    private fun loadOrder(orderNo: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = "", error = false)
            try {
                val res = RetrofitClient.api.getInboundOrderDetailByNo(
                    mapOf("orderNo" to orderNo)
                )
                if (res.code != 200 || res.data == null) {
                    _state.value = _state.value.copy(
                        loading = false, error = true,
                        message = "未找到入库单: $orderNo"
                    )
                    return@launch
                }
                val order = res.data
                if (order.status == 2 || order.status == 3) {
                    val msg = if (order.status == 2) "该入库单已完成入库" else "该入库单已作废"
                    _state.value = _state.value.copy(loading = false, error = true,
                        message = msg, orderInfo = "供应商: ${order.supplierName} | 状态: ${order.statusText}")
                    return@launch
                }
                _state.value = _state.value.copy(
                    loading = false,
                    orderInfo = "供应商: ${order.supplierName} | 状态: ${order.statusText} | 单号: $orderNo",
                    message = "✅ 已加载入库单（共 ${order.details?.size ?: 0} 个零件），请扫描看板标签"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = true,
                    message = "网络错误: ${e.localizedMessage}")
            }
        }
    }
}
