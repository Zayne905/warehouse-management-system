package com.warehouse.scanner.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warehouse.scanner.model.InboundOrderVO
import com.warehouse.scanner.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OrderDetailState(
    val order: InboundOrderVO? = null,
    val loading: Boolean = false
)

class OrderDetailViewModel : ViewModel() {

    private val _state = MutableStateFlow(OrderDetailState())
    val state: StateFlow<OrderDetailState> = _state

    fun loadOrder(orderId: Long) {
        viewModelScope.launch {
            _state.value = OrderDetailState(loading = true)
            try {
                val res = RetrofitClient.api.getInboundOrderDetail(mapOf("id" to orderId))
                if (res.code == 200) {
                    _state.value = OrderDetailState(order = res.data)
                } else {
                    _state.value = OrderDetailState()
                }
            } catch (_: Exception) {
                _state.value = OrderDetailState()
            }
        }
    }
}
