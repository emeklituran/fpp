package com.galaxyoffline.alarm

import android.app.Application
import com.galaxyoffline.alarm.ring.AlarmRingService
import com.galaxyoffline.alarm.ring.AlarmScheduler
import com.galaxyoffline.core.designsystem.util.CrashLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        AlarmRingService.ensureChannel(this)
        appScope.launch { runCatching { AlarmScheduler.rescheduleAll(this@App) } }
    }
}
