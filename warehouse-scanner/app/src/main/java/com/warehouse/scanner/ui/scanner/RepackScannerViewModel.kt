package com.warehouse.scanner.ui.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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
    val targetBoxCapacity: Int = 0,  // BREAKDOWN 目标箱容量

    val loading: Boolean = false,
    val message: String = "",
    val error: Boolean = false,
    val showCamera: Boolean = false,
    val needConfirm: Boolean = false,

    // 模式: init → scanned → added → generated → done
    val mode: String = "init"
)

class RepackScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(RepackState())
    val state: StateFlow<RepackState> = _state
    private val gson = Gson()

    fun setRepackType(type: String) { _state.value = _state.value.copy(repackType = type) }

    fun showCamera() { _state.value = _state.value.copy(showCamera = true) }
    fun hideCamera() { _state.value = _state.value.copy(showCamera = false) }

    fun setScanQty(qty: Int) { _state.value = _state.value.copy(scanQty = qty) }
    fun setTargetBoxCapacity(cap: Int) { _state.value = _state.value.copy(targetBoxCapacity = cap) }

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

    /** 扫码 → 预览看板（自动识别看板JSON或纯文本看板号） */
    fun onBarcodeScanned(rawText: String) {
        val trimmed = rawText.trim()
        Log.d("RepackScanner", "扫码原始内容: $trimmed")

        // 尝试解析为看板JSON，提取kanbanNo
        val kanbanNo = try {
            val qr = gson.fromJson(trimmed, KanbanQrData::class.java)
            (qr.kanbanNo ?: "").ifBlank { null }
        } catch (_: Exception) { null }

        if (kanbanNo != null) {
            Log.d("RepackScanner", "从JSON提取看板号: $kanbanNo")
            previewKanban(kanbanNo)
        } else {
            Log.d("RepackScanner", "纯文本看板号: $trimmed")
            previewKanban(trimmed)
        }
    }

    /** 查询看板预览（首次扫码自动创建转包单） */
    private fun previewKanban(kanbanNo: String) {
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

                    // 首次扫码：自动创建转包单（使用看板的零件和库区信息）
                    if (_state.value.orderId == null) {
                        val createBody = mapOf<String, Any>(
                            "repackType" to _state.value.repackType,
                            "partId" to p.partId,
                            "warehouseAreaId" to (p.warehouseAreaId ?: 0L),
                            "remark" to ""
                        )
                        val createRes = RetrofitClient.api.repackCreate(createBody)
                        if (createRes.code != 200 || createRes.data == null) {
                            _state.value = _state.value.copy(loading = false, error = true, message = "创建转包单失败: ${createRes.message}")
                            return@launch
                        }
                        val o = createRes.data
                        _state.value = _state.value.copy(
                            orderId = o.id, orderNo = o.orderNo,
                            partId = p.partId, partCode = p.partCode, partName = p.partName ?: "",
                            warehouseAreaId = p.warehouseAreaId ?: 0L, warehouseAreaName = p.warehouseAreaName ?: "",
                            orderStatus = o.status, orderStatusText = o.statusText
                        )
                    }

                    // 向下转包默认取全部剩余量；带余量转包默认取一半
                    val defaultQty = if (_state.value.repackType == "BREAKDOWN") {
                        p.transferableQty  // 拆包需要全拆
                    } else if (_state.value.repackType == "REMAINDER") {
                        (p.transferableQty / 2).coerceAtLeast(1)
                    } else {
                        p.transferableQty // CONSOLIDATE 自动取全部
                    }
                    _state.value = _state.value.copy(
                        loading = false,
                        preview = p,
                        scanQty = defaultQty,
                        // 向下转包预填目标箱容量 = 源数量/2（至少1）
                        targetBoxCapacity = if (_state.value.repackType == "BREAKDOWN") (p.transferableQty / 2).coerceAtLeast(1) else 0,
                        mode = "scanned", message = ""
                    )
                } else {
                    _state.value = _state.value.copy(loading = false, error = true, message = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = true, message = "查询失败: ${e.localizedMessage}")
            }
        }
    }

    /** 添加明细行（REMAINDER/CONSOLIDATE模式）或添加源箱（BREAKDOWN模式第一步） */
    fun addDetail() {
        val s = _state.value
        val p = s.preview ?: return
        if (s.orderId == null) return

        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = "添加中...", error = false)
            try {
                // 对于 BREAKDOWN，先用全部源数量添加一行（后续由 breakdownGenerate 替换为拆分行）
                val qty = if (s.repackType == "BREAKDOWN") p.transferableQty
                    else s.scanQty.coerceAtMost(p.transferableQty)

                val body = mapOf<String, Any>(
                    "orderId" to s.orderId,
                    "sourceKanbanNo" to p.kanbanNo,
                    "transferQty" to qty
                )
                val res = RetrofitClient.api.repackAddDetail(body)
                if (res.code == 200 && res.data != null) {
                    val o = res.data
                    if (s.repackType == "BREAKDOWN") {
                        // 向下转包：添加源箱后进入容量设置阶段
                        _state.value = _state.value.copy(
                            loading = false,
                            detailCount = o.detailCount,
                            totalQty = o.totalTransferQty,
                            mode = "added",
                            message = "已选择源箱 ${p.partName}(${p.partCode})，共${p.transferableQty}件\n请设置目标箱容量后自动拆包"
                        )
                    } else {
                        _state.value = _state.value.copy(
                            loading = false, needConfirm = true,
                            detailCount = o.detailCount,
                            totalQty = o.totalTransferQty,
                            mode = "added",
                            message = "已添加 ${p.partName} ×$qty 件，共${o.detailCount}行 ${o.totalTransferQty}件"
                        )
                    }
                } else {
                    _state.value = _state.value.copy(loading = false, error = true, message = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = true, message = "添加失败: ${e.localizedMessage}")
            }
        }
    }

    /** 向下转包：按目标箱容量自动生成拆分方案 */
    fun breakdownGenerate() {
        val s = _state.value
        val orderId = s.orderId ?: return
        val capacity = s.targetBoxCapacity
        if (capacity <= 0) {
            _state.value = _state.value.copy(error = true, message = "请输入目标箱容量（>0）")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = "自动拆包中...", error = false)
            try {
                val body = mapOf<String, Any>(
                    "orderId" to orderId,
                    "targetBoxCapacity" to capacity
                )
                val res = RetrofitClient.api.repackBreakdownGenerate(body)
                if (res.code == 200 && res.data != null) {
                    val o = res.data
                    _state.value = _state.value.copy(
                        loading = false, needConfirm = true,
                        detailCount = o.detailCount,
                        totalQty = o.totalTransferQty,
                        mode = "generated",
                        message = "拆包方案已生成：${o.detailCount} 箱，合计 ${o.totalTransferQty} 件（箱容量 $capacity）"
                    )
                } else {
                    _state.value = _state.value.copy(loading = false, error = true, message = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = true, message = "拆包失败: ${e.localizedMessage}")
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
    fun confirmContinue() { _state.value = _state.value.copy(needConfirm = false, message = "", preview = null) }
}
