package com.warehouse.scanner.ui.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.warehouse.scanner.network.RetrofitClient
import com.warehouse.scanner.network.TokenProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OutboundScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(OutboundScannerState())
    val state: StateFlow<OutboundScannerState> = _state
    private val gson = Gson()

    fun showCamera() { _state.value = _state.value.copy(showCamera = true) }
    fun hideCamera() { _state.value = _state.value.copy(showCamera = false) }

    fun confirmContinue() {
        _state.value = _state.value.copy(needConfirm = false, message = "", progress = null)
    }

    fun updateOrderNo(orderNo: String) {
        _state.value = _state.value.copy(orderNoInput = orderNo)
    }

    fun queryOrder() {
        val orderNo = _state.value.orderNoInput.trim()
        if (orderNo.isBlank()) {
            _state.value = _state.value.copy(error = true, message = "请输入出库单号")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingOrder = true, message = "查询中...", error = false)
            try {
                val res = RetrofitClient.api.getOutboundOrderByNo(mapOf("orderNo" to orderNo))
                if (res.code == 200 && res.data != null) {
                    val o = res.data
                    val partsInfo = o.parts?.joinToString("\n") {
                        "  ${it.partCode} ${it.partName} (计划 ${it.plannedQty})"
                    } ?: ""
                    _state.value = _state.value.copy(
                        loadingOrder = false,
                        orderId = o.id,
                        selectedOrderNo = o.orderNo,
                        orderInfo = "出库单: ${o.orderNo}\n客户: ${o.customerName ?: "-"}\n状态: ${o.statusText}\n零件(${o.partCount}种):\n$partsInfo",
                        message = "已选择出库单 ${o.orderNo}，请扫描看板",
                        error = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        loadingOrder = false, error = true,
                        message = "查询失败: ${res.message}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loadingOrder = false, error = true,
                    message = "连接失败: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearOrder() {
        _state.value = _state.value.copy(
            orderId = null, selectedOrderNo = null, orderInfo = null, orderNoInput = ""
        )
    }

    fun onQrScanned(text: String) {
        val trimmed = text.trim()
        Log.d("OutboundScanner", "扫码内容: $trimmed")
        _state.value = _state.value.copy(showCamera = false)

        if (trimmed.isEmpty()) {
            _state.value = _state.value.copy(error = true, message = "扫码内容为空，请重新扫描")
            return
        }

        // 尝试解析为看板JSON
        try {
            val qr = gson.fromJson(trimmed, KanbanQrData::class.java)
            val kanbanNo = qr.kanbanNo ?: ""
            val partCode = qr.partCode ?: ""
            if (kanbanNo.isNotBlank() && partCode.isNotBlank()) {
                Log.d("OutboundScanner", "解析看板成功: kanbanNo=$kanbanNo, partCode=$partCode")
                scanOutbound(qr)
                return
            } else {
                Log.d("OutboundScanner", "看板JSON字段不完整: kanbanNo='$kanbanNo', partCode='$partCode'")
                _state.value = _state.value.copy(
                    error = true,
                    message = "看板二维码数据不完整：缺少看板号或零件号\n扫描内容: $trimmed"
                )
                return
            }
        } catch (e: JsonSyntaxException) {
            Log.e("OutboundScanner", "JSON解析失败: ${e.message}")
        } catch (e: Exception) {
            Log.e("OutboundScanner", "扫码处理异常: ${e.message}", e)
        }

        _state.value = _state.value.copy(
            error = true,
            message = "无法识别此二维码格式，请确认扫描的是出库看板标签\n内容预览: ${trimmed.take(50)}..."
        )
    }

    private fun scanOutbound(qr: KanbanQrData, confirmed: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                submitting = true,
                message = "正在出库 ${qr.partName} ...\n看板号: ${qr.kanbanNo}",
                error = false
            )
            try {
                val body = buildMap<String, Any> {
                    put("kanbanNo", qr.kanbanNo ?: "")
                    put("operatorId", TokenProvider.userId ?: 1L)
                    put("confirmNonFifo", confirmed)
                    // 如果已选择出库单，传递 orderId
                    _state.value.orderId?.let { put("orderId", it) }
                }
                Log.d("OutboundScanner", "调用出库API: $body")
                val res = RetrofitClient.api.scanOutbound(body)
                Log.d("OutboundScanner", "API响应: code=${res.code}, message=${res.message}")

                if (res.code == 200 && res.data != null) {
                    val r = res.data
                    // 非FIFO确认提示
                    if (r.needsConfirm == true) {
                        _state.value = _state.value.copy(
                            submitting = false,
                            needsConfirmNonFifo = true,
                            pendingKanbanQr = qr,
                            message = "⚠️ ${r.message ?: "该看板未按FIFO匹配，是否确认出库？"}",
                            error = false
                        )
                        return@launch
                    }
                    var prefix = ""
                    if (r.autoAdded == true) prefix = "[自动匹配] "
                    if (r.crossOrdered == true) prefix = "[跨单转移] "
                    _state.value = _state.value.copy(
                        submitting = false, needConfirm = true,
                        message = "✅ ${prefix}${r.partName} ×${r.quantity} 已出库",
                        progress = ScanProgress(
                            partCode = r.partCode, partName = r.partName,
                            boxScanned = 0, boxTotal = 0, quantity = r.quantity,
                            plannedQty = r.plannedQty ?: 0.0, actualQty = r.actualQty ?: 0.0,
                            inboundOrderNo = qr.inboundOrderNo ?: "",
                            supplierName = qr.supplierName ?: "",
                            warehouseArea = qr.warehouseArea ?: "",
                            boxSeq = qr.boxSeq
                        )
                    )
                } else {
                    Log.e("OutboundScanner", "API返回错误: code=${res.code}, message=${res.message}")
                    _state.value = _state.value.copy(
                        submitting = false, error = true,
                        message = "出库失败: ${res.message}\n看板号: ${qr.kanbanNo}"
                    )
                }
            } catch (e: Exception) {
                Log.e("OutboundScanner", "网络请求异常: ${e.message}", e)
                _state.value = _state.value.copy(
                    submitting = false, error = true,
                    message = "连接服务器失败: ${e.localizedMessage}\n请检查网络或确认后端服务已启动(端口8081)"
                )
            }
        }
    }

    fun confirmNonFifoScan() {
        val pendingQr = _state.value.pendingKanbanQr ?: return
        _state.value = _state.value.copy(needsConfirmNonFifo = false)
        scanOutbound(pendingQr, confirmed = true)
    }

    fun cancelNonFifoScan() {
        _state.value = _state.value.copy(
            needsConfirmNonFifo = false, pendingKanbanQr = null,
            message = "已取消", error = true
        )
    }
}

data class OutboundScannerState(
    val showCamera: Boolean = false,
    val submitting: Boolean = false,
    val needConfirm: Boolean = false,
    val needsConfirmNonFifo: Boolean = false,
    val pendingKanbanQr: KanbanQrData? = null,
    val error: Boolean = false,
    val message: String = "",
    val progress: ScanProgress? = null,
    // 出库单选择
    val orderNoInput: String = "",
    val loadingOrder: Boolean = false,
    val orderId: Long? = null,
    val selectedOrderNo: String? = null,
    val orderInfo: String? = null
)
