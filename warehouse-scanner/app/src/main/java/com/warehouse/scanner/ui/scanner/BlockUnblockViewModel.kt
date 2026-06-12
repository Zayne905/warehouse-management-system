package com.warehouse.scanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.warehouse.scanner.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BlockUnblockViewModel : ViewModel() {

    private val _state = MutableStateFlow(BlockUnblockState())
    val state: StateFlow<BlockUnblockState> = _state
    private val gson = Gson()

    fun showCamera() { _state.value = _state.value.copy(showCamera = true) }
    fun hideCamera() { _state.value = _state.value.copy(showCamera = false) }

    fun confirmContinue() {
        _state.value = _state.value.copy(needConfirm = false, message = "", result = null)
    }

    fun onQrScanned(text: String) {
        val trimmed = text.trim()
        _state.value = _state.value.copy(showCamera = false)
        try {
            val qr = gson.fromJson(trimmed, KanbanQrData::class.java)
            if (qr.kanbanNo.isNotBlank() && qr.partCode.isNotBlank()) {
                toggleBlock(qr.kanbanNo, qr.partName)
                return
            }
        } catch (_: Exception) { }
        _state.value = _state.value.copy(error = true, message = "无效的看板二维码")
    }

    private fun toggleBlock(kanbanNo: String, partName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(submitting = true, message = "正在操作 $partName ...", error = false)
            try {
                val body: Map<String, Any> = mapOf("kanbanNo" to kanbanNo)
                val res = RetrofitClient.api.toggleBlock(body)
                if (res.code == 200 && res.data != null) {
                    val r = res.data
                    _state.value = _state.value.copy(
                        submitting = false, needConfirm = true,
                        message = "${r.partName}(${r.partCode}) 已${r.action}",
                        result = BlockUnblockResult(
                            partCode = r.partCode, partName = r.partName,
                            action = r.action, previousStatusText = r.previousStatusText
                        )
                    )
                } else {
                    _state.value = _state.value.copy(submitting = false, error = true, message = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(submitting = false, error = true, message = "网络错误: ${e.localizedMessage}")
            }
        }
    }
}

data class BlockUnblockState(
    val message: String = "",
    val error: Boolean = false,
    val submitting: Boolean = false,
    val showCamera: Boolean = false,
    val needConfirm: Boolean = false,
    val result: BlockUnblockResult? = null
)

data class BlockUnblockResult(
    val partCode: String = "",
    val partName: String = "",
    val action: String = "",
    val previousStatusText: String = ""
)
