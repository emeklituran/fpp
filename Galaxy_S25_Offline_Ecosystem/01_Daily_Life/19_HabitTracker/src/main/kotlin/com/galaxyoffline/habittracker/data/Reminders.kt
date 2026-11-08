package com.galaxyoffline.habittracker.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.galaxyoffline.habittracker.MainActivity
import com.galaxyoffline.habittracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Alışkanlık başına günlük hatırlatma. Tam zamanlı alarm izni istenmez: setWindow ile
 * 10 dakikalık pencere kullanılır (Android pil tasarrufu için birkaç dakika kaydırabilir).
 */
object Reminders {
    private const val CHANNEL = "hatirlatma"
    private const val ACTION_REMIND = "com.galaxyoffline.habittracker.REMIND"
    private const val ACTION_DONE = "com.galaxyoffline.habittracker.DONE"

    private fun pi(c: Context, id: Long, action: String) = PendingIntent.getBroadcast(c, (id * 2 + if (action == ACTION_DONE) 1 else 0).toInt(),
        Intent(c, ReminderReceiver::class.java).setAction(action).putExtra("id", id), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun schedule(c: Context, h: Habit) {
        val am = c.getSystemService(AlarmManager::class.java)
        val p = pi(c, h.id, ACTION_REMIND)
        val m = h.remindMinute
        if (m == null || h.archived) { am.cancel(p); return }
        val now = LocalDateTime.now()
        var at = LocalDate.now().atTime(m / 60, m % 60)
        if (!at.isAfter(now)) at = at.plusDays(1)
        repeat(7) { if (!h.scheduledOn(at.toLocalDate())) at = at.plusDays(1) }
        am.setWindow(AlarmManager.RTC_WAKEUP, at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 10 * 60_000L, p)
    }

    fun cancel(c: Context, id: Long) {
        c.getSystemService(AlarmManager::class.java).cancel(pi(c, id, ACTION_REMIND))
        NotificationManagerCompat.from(c).cancel(id.toInt())
    }

    suspend fun rescheduleAll(c: Context) = HabitDatabase.get(c).habits().activeHabits().forEach { schedule(c, it) }

    suspend fun handle(c: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1); if (id < 0) return
        val dao = HabitDatabase.get(c).habits()
        val h = dao.habit(id) ?: return
        val today = LocalDate.now().toEpochDay()
        when (intent.action) {
            ACTION_DONE -> { dao.check(Check(id, today)); NotificationManagerCompat.from(c).cancel(id.toInt()) }
            ACTION_REMIND -> {
                schedule(c, h)
                if (dao.isChecked(id, today) > 0 || !NotificationManagerCompat.from(c).areNotificationsEnabled()) return
                val nm = c.getSystemService(NotificationManager::class.java)
                if (nm.getNotificationChannel(CHANNEL) == null) nm.createNotificationChannel(NotificationChannel(CHANNEL, "Alışkanlık hatırlatmaları", NotificationManager.IMPORTANCE_DEFAULT))
                val open = PendingIntent.getActivity(c, id.toInt(), Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                val n = NotificationCompat.Builder(c, CHANNEL).setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("${h.emoji} ${h.name}").setContentText("Bugün için henüz işaretlenmedi.")
                    .setContentIntent(open).setAutoCancel(true)
                    .addAction(0, "Yaptım", pi(c, id, ACTION_DONE)).build()
                runCatching { NotificationManagerCompat.from(c).notify(id.toInt(), n) }
            }
        }
    }
}

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val p = goAsync()
        scope.launch { try { Reminders.handle(context, intent) } catch (_: Exception) { } finally { p.finish() } }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val p = goAsync()
        scope.launch { try { Reminders.rescheduleAll(context) } catch (_: Exception) { } finally { p.finish() } }
    }
}
