package com.warehouse.scanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.warehouse.scanner.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OutboundScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScannerState(mode = ScanMode.OUTBOUND))
    val state: StateFlow<ScannerState> = _state
    private val gson = Gson()

    fun showCamera() { _state.value = _state.value.copy(showCamera = true) }
    fun hideCamera() { _state.value = _state.value.copy(showCamera = false) }

    fun confirmContinue() {
        _state.value = _state.value.copy(needConfirm = false, message = "", progress = null)
    }

    fun onQrScanned(text: String) {
        val trimmed = text.trim()
        _state.value = _state.value.copy(showCamera = false)
        try {
            val qr = gson.fromJson(trimmed, KanbanQrData::class.java)
            if (qr.kanbanNo.isNotBlank() && qr.partCode.isNotBlank()) {
                scanOutbound(qr)
                return
            }
        } catch (_: Exception) { }
        _state.value = _state.value.copy(error = true, message = "无效的看板二维码")
    }

    private fun scanOutbound(qr: KanbanQrData) {
        viewModelScope.launch {
            _state.value = _state.value.copy(submitting = true, message = "正在出库 ${qr.partName} ...", error = false)
            try {
                val body: Map<String, Any> = mapOf("kanbanNo" to qr.kanbanNo, "operatorId" to 1)
                val res = RetrofitClient.api.scanOutbound(body)
                if (res.code == 200 && res.data != null) {
                    val r = res.data
                    _state.value = _state.value.copy(
                        submitting = false, needConfirm = true,
                        message = "✅ ${r.partName} ×${r.quantity} 已出库",
                        progress = ScanProgress(
                            partCode = r.partCode, partName = r.partName,
                            boxScanned = 0, boxTotal = 0, quantity = r.quantity,
                            plannedQty = r.plannedQty ?: 0.0, actualQty = r.actualQty ?: 0.0,
                            inboundOrderNo = qr.inboundOrderNo, supplierName = qr.supplierName,
                            warehouseArea = qr.warehouseArea, boxSeq = qr.boxSeq
                        )
                    )
                } else {
                    _state.value = _state.value.copy(submitting = false, error = true, message = "⚠️ ${res.message}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(submitting = false, error = true, message = "网络错误: ${e.localizedMessage}")
            }
        }
    }
}
