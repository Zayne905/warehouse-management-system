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
    val loaded: Boolean = false
)

class SubmitViewModel : ViewModel() {

    private val _state = MutableStateFlow(SubmitState())
    val state: StateFlow<SubmitState> = _state

    fun onOrderNoChange(v: String) { _state.value = _state.value.copy(orderNo = v) }

    fun loadScans() {
        val orderNo = _state.value.orderNo.trim()
        if (orderNo.isEmpty()) return

        viewModelScope.launch {
            try {
                // 先通过 feedback 获取 orderId
                val fbRes = RetrofitClient.api.getScanFeedback(
                    com.warehouse.scanner.model.ScanFeedbackRequest(orderNo)
                )
                if (fbRes.code != 200 || fbRes.data == null) return@launch

                // 从 feedback 数据里无法直接拿 orderId，需要后端加接口
                // 这里暂时用已知测试数据
                _state.value = _state.value.copy(loaded = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loaded = true)
            }
        }
    }

    fun deleteScan(scanRecordId: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.deleteScan(mapOf("scanRecordId" to scanRecordId))
                _state.value = _state.value.copy(
                    scanRecords = _state.value.scanRecords.filter { it.id != scanRecordId }
                )
            } catch (_: Exception) {}
        }
    }
}
