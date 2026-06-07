package com.warehouse.scanner

import android.app.Application
import com.warehouse.scanner.network.TokenProvider

class WarehouseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenProvider.init(this)
    }
}
