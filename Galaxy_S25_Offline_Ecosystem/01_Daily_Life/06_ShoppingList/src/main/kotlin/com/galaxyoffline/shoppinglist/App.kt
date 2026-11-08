package com.galaxyoffline.shoppinglist

import android.app.Application
import com.galaxyoffline.core.designsystem.util.CrashLogger
import com.galaxyoffline.shoppinglist.data.ShoppingDatabase

class App : Application() {
    val database by lazy { ShoppingDatabase.get(this) }
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
