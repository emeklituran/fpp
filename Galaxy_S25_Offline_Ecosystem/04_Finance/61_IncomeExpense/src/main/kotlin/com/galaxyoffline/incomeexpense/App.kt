package com.galaxyoffline.incomeexpense

import android.app.Application
import com.galaxyoffline.core.designsystem.util.CrashLogger

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
