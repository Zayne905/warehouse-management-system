package com.warehouse.scanner.network

import android.content.Context
import android.content.SharedPreferences

object TokenProvider {
    private const val PREFS_NAME = "warehouse_auth"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_ROLE = "role"
    private const val KEY_USER_ID = "user_id"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    var token: String?
        get() = prefs?.getString(KEY_TOKEN, null)
        set(value) { prefs?.edit()?.putString(KEY_TOKEN, value)?.apply() }

    var nickname: String?
        get() = prefs?.getString(KEY_NICKNAME, null)
        set(value) { prefs?.edit()?.putString(KEY_NICKNAME, value)?.apply() }

    var role: String?
        get() = prefs?.getString(KEY_ROLE, null)
        set(value) { prefs?.edit()?.putString(KEY_ROLE, value)?.apply() }

    val isLoggedIn: Boolean get() = !token.isNullOrEmpty()

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}
