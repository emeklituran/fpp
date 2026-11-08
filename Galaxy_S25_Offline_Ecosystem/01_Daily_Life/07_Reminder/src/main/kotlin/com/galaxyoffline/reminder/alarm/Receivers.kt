package com.galaxyoffline.reminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.galaxyoffline.reminder.data.ReminderDatabase
import com.galaxyoffline.reminder.data.Repeat
import com.galaxyoffline.reminder.data.RepeatLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try { block() } catch (e: Exception) { Log.e("Reminder", "Alıcı hatası", e) } finally { pending.finish() }
    }
}

/** Zamanı gelen hatırlatmayı bildirir ve tekrarlıysa bir sonrakini kurar. Dışa kapalıdır. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1)
        if (id <= 0) return
        runAsync {
            val dao = ReminderDatabase.get(context).reminders()
            val r = dao.get(id) ?: return@runAsync
            when (intent.action) {
                ReminderScheduler.ACTION_FIRE -> {
                    if (!r.enabled) return@runAsync
                    ReminderScheduler.notify(context, r)
                    if (intent.getBooleanExtra(ReminderScheduler.EXTRA_SNOOZED, false)) return@runAsync
                    if (r.repeatRule == Repeat.NONE) {
                        dao.update(r.copy(done = true))
                    } else {
                        val zone = ZoneId.systemDefault()
                        val anchor = Instant.ofEpochMilli(r.anchorAt).atZone(zone).toLocalDateTime()
                        val next = RepeatLogic.nextAfter(anchor, r.repeatRule, LocalDateTime.now().plusSeconds(1))
                        if (next != null) {
                            val updated = r.copy(triggerAt = next.atZone(zone).toInstant().toEpochMilli())
                            dao.update(updated)
                            ReminderScheduler.schedule(context, updated)
                        }
                    }
                }
                ReminderScheduler.ACTION_DONE -> {
                    NotificationManagerCompat.from(context).cancel(id.toInt())
                    if (r.repeatRule == Repeat.NONE) dao.update(r.copy(done = true))
                }
                ReminderScheduler.ACTION_SNOOZE -> {
                    NotificationManagerCompat.from(context).cancel(id.toInt())
                    ReminderScheduler.snooze(context, id)
                }
            }
        }
    }
}

/** Yeniden başlatma, saat/saat dilimi değişimi, güncelleme ve tam alarm izni değişiminde yeniden kurar. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runAsync { ReminderScheduler.rescheduleAll(context) }
    }
}
