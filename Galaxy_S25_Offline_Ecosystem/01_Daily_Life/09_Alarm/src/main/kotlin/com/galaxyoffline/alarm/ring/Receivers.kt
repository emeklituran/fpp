package com.galaxyoffline.alarm.ring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.galaxyoffline.alarm.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private fun BroadcastReceiver.async(block: suspend () -> Unit) {
    val pending = goAsync()
    scope.launch {
        try { block() } catch (e: Exception) { Log.e("Alarm", "Alıcı hatası", e) } finally { pending.finish() }
    }
}

/** Alarm zamanı geldi: çalma hizmetini başlatır, tekrarlıysa bir sonrakini kurar, tek seferlikse kapatır. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1)
        if (id <= 0) return
        val snoozed = intent.getBooleanExtra(AlarmScheduler.EXTRA_SNOOZE, false)
        // Ön plan hizmeti hemen başlatılmalı (tam zamanlı alarm muafiyeti bu süre içinde geçerlidir).
        ContextCompat.startForegroundService(context, AlarmRingService.ringIntent(context, id, preview = false))
        async {
            val dao = AlarmDatabase.get(context).alarms()
            val a = dao.get(id) ?: return@async
            if (snoozed) return@async
            if (a.days == 0) dao.update(a.copy(enabled = false)) else AlarmScheduler.schedule(context, a)
        }
    }
}

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = async { AlarmScheduler.rescheduleAll(context) }
}
