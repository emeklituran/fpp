package com.galaxyoffline.timer

import android.app.Application
import com.galaxyoffline.core.designsystem.util.CrashLogger
import com.galaxyoffline.timer.data.TimerStore

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        TimerStore.load(this)
    }
}
