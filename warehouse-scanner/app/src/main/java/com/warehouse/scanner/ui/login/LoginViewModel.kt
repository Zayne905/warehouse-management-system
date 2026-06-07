package com.warehouse.scanner.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warehouse.scanner.model.LoginRequest
import com.warehouse.scanner.network.RetrofitClient
import com.warehouse.scanner.network.TokenProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginState(
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

class LoginViewModel : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    fun onUsernameChange(value: String) {
        _state.value = _state.value.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun login() {
        val s = _state.value
        if (s.username.isBlank() || s.password.isBlank()) return

        viewModelScope.launch {
            _state.value = s.copy(loading = true, error = null)
            try {
                val res = RetrofitClient.api.login(
                    LoginRequest(s.username.trim(), s.password)
                )
                if (res.code == 200 && res.data != null) {
                    TokenProvider.token = res.data.token
                    TokenProvider.nickname = res.data.nickname
                    TokenProvider.role = res.data.role
                    _state.value = s.copy(loading = false, success = true)
                } else {
                    _state.value = s.copy(
                        loading = false,
                        error = res.message.ifEmpty { "用户名或密码错误" }
                    )
                }
            } catch (e: Exception) {
                _state.value = s.copy(
                    loading = false,
                    error = "网络连接失败: ${e.localizedMessage}"
                )
            }
        }
    }
}
