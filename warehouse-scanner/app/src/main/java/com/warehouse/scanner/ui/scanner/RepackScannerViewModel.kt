package com.warehouse.scanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warehouse.scanner.model.RepackOrderData
import com.warehouse.scanner.model.RepackPreviewData
import com.warehouse.scanner.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RepackState(
    val repackType: String = "REMAINDER",  // REMAINDER / CONSOLIDATE / BREAKDOWN
    val orderId: Long? = null,
    val orderNo: String = "",
    val partId: Long? = null,
    val partCode: String = "",
    val partName: String = "",
    val warehouseAreaId: Long? = null,
    val warehouseAreaName: String = "",
    val detailCount: Int = 0,
    val totalQty: Int = 0,
    val orderStatus: Int = 0,
    val orderStatusText: String = "",

    // 扫码预览
    val preview: RepackPreviewData? = null,
    val scanQty: Int = 0,

    val loading: Boolean = false,
    val message: String = "",
    val error: Boolean = false,
    val showCamera: Boolean = false,
    val needConfirm: Boolean = false,

    // 模式
    val mode: String = "init"  // init → preview → added → done
)

class RepackScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(RepackState())
    val state: StateFlow<RepackState> = _state

    fun setRepackType(type: String) { _state.value = _state.value.copy(repackType = type) }

    fun showCamera() { _state.value = _state.value.copy(showCamera = true) }
    fun hideCamera() { _state.value = _state.value.copy(showCamera = false) }

    fun setScanQty(qty: Int) { _state.value = _state.value.copy(scanQty = qty) }

    /** 创建空转包单 */
    fun createOrder(partId: Long, warehouseAreaId: Long, partCode: String, warehouseName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = "正在创建转包单...", error = false)
            try {
                val body = mapOf<String, Any>(
                    "repackType" to _state.value.repackType,
                    "partId" to partId,
                    "warehouseAreaId" to warehouseAreaId,
                    "remark" to ""
                )
                val res = RetrofitClient.api.repackCreate(body)
                if (res.code == 200 && res.data != null) {
                    val o = res.data
                    _state.value = _state.value.copy(
                        loading = false,
                        orderId = o.id, orderNo = o.orderNo,
                        partId = partId, partCode = partCode, partName = o.partName ?: "",
                        warehouseAreaId = warehouseAreaId, warehouseAreaName = warehouseName,
                        orderStatus = o.status, orderStatusText = o.statusText,
                        mode = "ord_created", message = "转包单 ${o.orderNo} 已创建，请扫描源包装"
                    )
                } else {
                    _state.value = _state.value.copy(loading = false, error = true, message = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = true, message = "创建失败: ${e.localizedMessage}")
            }
        }
    }

    /** 扫码 → 预览看板 */
    fun previewKanban(kanbanNo: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = "查询看板...", showCamera = false, error = false)
            try {
                val res = RetrofitClient.api.repackPreview(mapOf("kanbanNo" to kanbanNo))
                if (res.code == 200 && res.data != null) {
                    val p = res.data
                    if (p.status != 1 && p.status != 5) {
                        _state.value = _state.value.copy(loading = false, error = true, message = "看板状态: ${p.statusText}，不可转包")
                        return@launch
                    }
                    _state.value = _state.value.copy(
                        loading = false,
                        preview = p,
                        scanQty = if (_state.value.repackType == "REMAINDER") (p.transferableQty / 2).coerceAtLeast(1) else p.transferableQty,
                        mode = "scanned", message = "" // clear old message on scan page
                    )
                } else {
                    _state.value = _state.value.copy(loading = false, error = true, message = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = true, message = "查询失败: ${e.localizedMessage}")
            }
        }
    }

    /** 确认添加明细 */
    fun addDetail() {
        val s = _state.value
        val p = s.preview ?: return
        if (s.orderId == null) return

        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = "添加中...", error = false)
            try {
                val qty = s.scanQty.coerceAtMost(p.transferableQty)
                val body = mapOf<String, Any>(
                    "orderId" to s.orderId,
                    "sourceKanbanNo" to p.kanbanNo,
                    "transferQty" to qty
                )
                val res = RetrofitClient.api.repackAddDetail(body)
                if (res.code == 200 && res.data != null) {
                    val o = res.data
                    _state.value = _state.value.copy(
                        loading = false, needConfirm = true,
                        detailCount = o.detailCount,
                        totalQty = o.totalTransferQty,
                        mode = "added",
                        message = "已添加 ${p.partName} ×$qty 件，共${o.detailCount}行 ${o.totalTransferQty}件"
                    )
                } else {
                    _state.value = _state.value.copy(loading = false, error = true, message = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = true, message = "添加失败: ${e.localizedMessage}")
            }
        }
    }

    /** 确认执行转包 */
    fun confirmRepack() {
        val id = _state.value.orderId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = "执行转包中...", needConfirm = false, error = false)
            try {
                val res = RetrofitClient.api.repackConfirm(mapOf("id" to id))
                if (res.code == 200 && res.data != null) {
                    val o = res.data
                    _state.value = _state.value.copy(
                        loading = false,
                        orderStatus = o.status,
                        orderStatusText = o.statusText,
                        detailCount = o.detailCount,
                        totalQty = o.totalTransferQty,
                        mode = "done",
                        message = "转包完成！${o.detailCount} 行，合计 ${o.totalTransferQty} 件"
                    )
                } else {
                    _state.value = _state.value.copy(loading = false, error = true, message = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = true, message = "执行失败: ${e.localizedMessage}")
            }
        }
    }

    fun reset() { _state.value = RepackState(repackType = _state.value.repackType) }
    fun confirmContinue() { _state.value = _state.value.copy(needConfirm = false, message = "") }
}
