package com.galaxyoffline.billreminder

import android.app.Application
import com.galaxyoffline.billreminder.data.DailyCheck
import com.galaxyoffline.core.designsystem.util.CrashLogger

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        runCatching { DailyCheck.schedule(this) }
    }
}
