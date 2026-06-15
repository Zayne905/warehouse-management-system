package com.warehouse.scanner.ui.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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
        Log.d("OutboundScanner", "扫码内容: $trimmed")
        _state.value = _state.value.copy(showCamera = false)

        // 空内容检查
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

    private fun scanOutbound(qr: KanbanQrData) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                submitting = true,
                message = "正在出库 ${qr.partName} ...\n看板号: ${qr.kanbanNo}",
                error = false
            )
            try {
                val body: Map<String, Any> = mapOf(
                    "kanbanNo" to (qr.kanbanNo ?: ""),
                    "operatorId" to 1
                )
                Log.d("OutboundScanner", "调用出库API: $body")
                val res = RetrofitClient.api.scanOutbound(body)
                Log.d("OutboundScanner", "API响应: code=${res.code}, message=${res.message}")

                if (res.code == 200 && res.data != null) {
                    val r = res.data
                    _state.value = _state.value.copy(
                        submitting = false, needConfirm = true,
                        message = "✅ ${r.partName} ×${r.quantity} 已出库",
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
                        message = "出库失败: ${res.message}\n看板号: ${qr.kanbanNo}\n请确认已创建出库单并匹配了此看板"
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
}
