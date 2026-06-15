package com.warehouse.scanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.warehouse.scanner.model.KanbanLifecycleData
import com.warehouse.scanner.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TraceState(
    val kanbanNo: String = "",
    val result: KanbanLifecycleData? = null,
    val loading: Boolean = false,
    val message: String = "",
    val error: Boolean = false,
    val showCamera: Boolean = false
)

class TraceViewModel : ViewModel() {
    private val _state = MutableStateFlow(TraceState())
    val state: StateFlow<TraceState> = _state
    private val gson = Gson()

    fun setKanbanNo(value: String) { _state.value = _state.value.copy(kanbanNo = value) }
    fun showCamera() { _state.value = _state.value.copy(showCamera = true) }
    fun hideCamera() { _state.value = _state.value.copy(showCamera = false) }

    fun onBarcodeScanned(rawText: String) {
        val text = rawText.trim()
        val no = try {
            gson.fromJson(text, KanbanQrData::class.java).kanbanNo?.ifBlank { null }
        } catch (_: Exception) { null }
        query(no ?: text)
    }

    fun query(value: String = _state.value.kanbanNo) {
        val no = value.trim()
        if (no.isEmpty()) {
            _state.value = _state.value.copy(error = true, message = "请输入看板号")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(kanbanNo = no, loading = true, showCamera = false, error = false, message = "查询中...")
            try {
                val response = RetrofitClient.api.kanbanLifecycle(mapOf("kanbanNo" to no))
                if (response.code == 200 && response.data != null) {
                    _state.value = _state.value.copy(result = response.data, loading = false, message = "")
                } else {
                    _state.value = _state.value.copy(result = null, loading = false, error = true, message = response.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(result = null, loading = false, error = true, message = "查询失败: ${e.localizedMessage}")
            }
        }
    }
}
