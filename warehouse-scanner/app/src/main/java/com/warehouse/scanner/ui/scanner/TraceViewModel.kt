package com.warehouse.scanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warehouse.scanner.model.TraceData
import com.warehouse.scanner.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TraceState(
    val kanbanNo: String = "",
    val result: TraceData? = null,
    val loading: Boolean = false,
    val message: String = "",
    val error: Boolean = false,
    val showCamera: Boolean = false
)

class TraceViewModel : ViewModel() {

    private val _state = MutableStateFlow(TraceState())
    val state: StateFlow<TraceState> = _state

    fun showCamera() { _state.value = _state.value.copy(showCamera = true) }
    fun hideCamera() { _state.value = _state.value.copy(showCamera = false) }

    fun traceByKanban(kanbanNo: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = "追溯中...", showCamera = false, error = false)
            try {
                val res = RetrofitClient.api.repackTrace(mapOf("kanbanNo" to kanbanNo))
                if (res.code == 200 && res.data != null) {
                    val d = res.data
                    if (d.parentChain.isNullOrEmpty() && d.childChain.isNullOrEmpty()) {
                        _state.value = _state.value.copy(
                            loading = false, result = d,
                            message = "该看板暂无转包记录",
                            kanbanNo = kanbanNo
                        )
                    } else {
                        _state.value = _state.value.copy(
                            loading = false, result = d,
                            message = "",
                            kanbanNo = kanbanNo
                        )
                    }
                } else {
                    _state.value = _state.value.copy(loading = false, error = true, message = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = true, message = "追溯失败: ${e.localizedMessage}")
            }
        }
    }
}
