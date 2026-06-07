package com.warehouse.scanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warehouse.scanner.model.*
import com.warehouse.scanner.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PartItem(
    val detailId: Long,
    val partId: Long,
    val partCode: String,
    val partName: String,
    val unit: String,
    val plannedQty: Double,
    val alreadyReceived: Double,
    val receiveQty: String = ""   // 本次要入库的数量
)

data class ScannerState(
    val orderNo: String = "",
    val orderId: Long = 0,
    val orderInfo: String = "",      // "供应商: xxx | 订单号: xxx | 状态: xxx"
    val parts: List<PartItem> = emptyList(),
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val message: String = "",        // 提示信息
    val error: Boolean = false,
    val completed: Boolean = false,  // 全部提交完成
    val showCamera: Boolean = false
)

class ScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state

    fun onOrderNoChange(v: String) {
        _state.value = _state.value.copy(
            orderNo = v, message = "", error = false
        )
    }

    fun showCamera() {
        _state.value = _state.value.copy(showCamera = true)
    }
    fun hideCamera() {
        _state.value = _state.value.copy(showCamera = false)
    }

    /**
     * 扫码回调：QR 看板上是入库单号
     */
    fun onQrScanned(text: String) {
        val trimmed = text.trim()
        _state.value = _state.value.copy(orderNo = trimmed, showCamera = false)
        // 扫码后自动加载
        loadOrder(trimmed)
    }

    /**
     * 手动搜索/加载入库单
     */
    fun loadOrder() {
        val no = _state.value.orderNo.trim()
        if (no.isEmpty()) return
        loadOrder(no)
    }

    private fun loadOrder(orderNo: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = "", error = false)
            try {
                // 用新接口按单号查详情
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
                if (order.status == 2) {
                    _state.value = _state.value.copy(loading = false, error = true,
                        message = "该入库单已完成入库",
                        orderInfo = "供应商: ${order.supplierName} | 状态: ${order.statusText}")
                    return@launch
                }
                if (order.status == 3) {
                    _state.value = _state.value.copy(loading = false, error = true,
                        message = "该入库单已作废",
                        orderInfo = "供应商: ${order.supplierName} | 状态: ${order.statusText}")
                    return@launch
                }

                // 解析零件明细
                val parts = order.details?.map { det ->
                    PartItem(
                        detailId = det.id,
                        partId = det.partId,
                        partCode = det.partCode,
                        partName = det.partName,
                        unit = det.unit ?: "",
                        plannedQty = det.plannedQty,
                        alreadyReceived = det.actualQty,
                        receiveQty = ""
                    )
                } ?: emptyList()

                _state.value = _state.value.copy(
                    loading = false,
                    orderId = order.id,
                    orderInfo = "供应商: ${order.supplierName} | 状态: ${order.statusText} | 单号: $orderNo | 订单号: ${order.orderNumber ?: "-"}",
                    parts = parts,
                    message = "✅ 已加载入库单，共 ${parts.size} 个零件"
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = true,
                    message = "网络错误: ${e.localizedMessage}")
            }
        }
    }

    fun onReceiveQtyChange(partCode: String, qty: String) {
        val parts = _state.value.parts.map {
            if (it.partCode == partCode) it.copy(receiveQty = qty) else it
        }
        _state.value = _state.value.copy(parts = parts)
    }

    /**
     * 提交确认：将所有勾选/输入的零件扫码入库
     */
    fun submitAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(submitting = true, message = "")
            val orderId = _state.value.orderId
            val orderNo = _state.value.orderNo

            var successCount = 0
            var failCount = 0

            for (part in _state.value.parts) {
                val qty = part.receiveQty.toDoubleOrNull() ?: continue
                if (qty <= 0) continue

                try {
                    val res = RetrofitClient.api.submitScan(
                        ScanSubmitRequest(
                            inboundOrderId = orderId,
                            inboundOrderNo = orderNo,
                            partCode = part.partCode,
                            batchNo = "",
                            scanQty = qty,
                            operatorId = 1
                        )
                    )
                    if (res.code == 200) successCount++ else failCount++
                } catch (_: Exception) {
                    failCount++
                }
            }

            val msg = "提交完成: 成功 $successCount 条, 失败 $failCount 条"
            _state.value = _state.value.copy(submitting = false, message = msg,
                completed = successCount > 0 && failCount == 0)
            if (successCount > 0) {
                loadOrder(orderNo) // 重新加载刷新已收数据
            }
        }
    }
}
