package com.galaxyoffline.reminder

import android.app.Application
import com.galaxyoffline.core.designsystem.util.CrashLogger
import com.galaxyoffline.reminder.alarm.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        ReminderScheduler.ensureChannel(this)
        // Kendi kendini onarma: her açılışta zamanlamalar doğrulanır (hafif bir işlem).
        appScope.launch { runCatching { ReminderScheduler.rescheduleAll(this@App) } }
    }
}
