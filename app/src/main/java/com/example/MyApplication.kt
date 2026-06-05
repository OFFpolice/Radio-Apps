package com.example

import android.app.Application
import android.content.Context
import android.os.Build

class MyApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        val nextBase = if (base != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            base.createAttributionContext("webradio")
        } else {
            base
        }
        super.attachBaseContext(nextBase)
    }
}
