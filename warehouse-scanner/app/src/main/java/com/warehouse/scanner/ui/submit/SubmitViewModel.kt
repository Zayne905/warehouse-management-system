package com.warehouse.scanner.ui.submit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warehouse.scanner.model.ScanRecordVO
import com.warehouse.scanner.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SubmitState(
    val orderNo: String = "",
    val scanRecords: List<ScanRecordVO> = emptyList(),
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val message: String = ""
)

class SubmitViewModel : ViewModel() {

    private val _state = MutableStateFlow(SubmitState())
    val state: StateFlow<SubmitState> = _state

    fun onOrderNoChange(v: String) { _state.value = _state.value.copy(orderNo = v, message = "") }

    fun loadScans() {
        val orderNo = _state.value.orderNo.trim()
        if (orderNo.isEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = "")
            try {
                // Step 1: 通过入库单号查询入库单ID
                val orderRes = RetrofitClient.api.getInboundOrderDetailByNo(
                    mapOf("orderNo" to orderNo)
                )
                if (orderRes.code != 200 || orderRes.data == null) {
                    _state.value = _state.value.copy(
                        loading = false, loaded = true,
                        message = "未找到入库单: $orderNo"
                    )
                    return@launch
                }
                val orderId = orderRes.data.id

                // Step 2: 根据入库单ID查询扫描记录
                val scanRes = RetrofitClient.api.listScans(
                    mapOf("inboundOrderId" to orderId)
                )
                if (scanRes.code == 200 && scanRes.data != null) {
                    _state.value = _state.value.copy(
                        loading = false, loaded = true,
                        scanRecords = scanRes.data,
                        message = if (scanRes.data.isEmpty()) "暂无扫描记录" else ""
                    )
                } else {
                    _state.value = _state.value.copy(
                        loading = false, loaded = true,
                        message = "查询失败: ${scanRes.message}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false, loaded = true,
                    message = "网络错误: ${e.localizedMessage}"
                )
            }
        }
    }

    fun deleteScan(scanRecordId: Long) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.api.deleteScan(mapOf("scanRecordId" to scanRecordId))
                if (res.code == 200) {
                    _state.value = _state.value.copy(
                        scanRecords = _state.value.scanRecords.filter { it.id != scanRecordId }
                    )
                }
            } catch (_: Exception) {}
        }
    }
}
