package com.galaxyoffline.reminder.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.reminder.MainActivity
import com.galaxyoffline.reminder.R
import com.galaxyoffline.reminder.data.Reminder
import com.galaxyoffline.reminder.data.ReminderDatabase
import com.galaxyoffline.reminder.data.RepeatLogic
import java.time.LocalDateTime
import java.time.ZoneId

/** AlarmManager ile yerel zamanlama. İnternet veya Google Play Hizmetleri kullanılmaz. */
object ReminderScheduler {
    const val CHANNEL = "hatirlatmalar"
    const val ACTION_FIRE = "com.galaxyoffline.reminder.FIRE"
    const val ACTION_DONE = "com.galaxyoffline.reminder.DONE"
    const val ACTION_SNOOZE = "com.galaxyoffline.reminder.SNOOZE"
    const val EXTRA_ID = "id"
    const val EXTRA_SNOOZED = "snoozed"
    const val SNOOZE_MINUTES = 10L

    fun canExact(context: Context): Boolean = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun notificationsAllowed(context: Context) = NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun pending(context: Context, id: Long, snooze: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE)
            .putExtra(EXTRA_ID, id).putExtra(EXTRA_SNOOZED, snooze)
        return PendingIntent.getBroadcast(context, requestCode(id, snooze), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun requestCode(id: Long, snooze: Boolean) = (id * 2 + if (snooze) 1 else 0).toInt()

    private fun set(context: Context, at: Long, pi: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java)
        try {
            if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            Log.w("Reminder", "Tam zamanlı alarm izni yok, yaklaşık zamanlama kullanılıyor", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun schedule(context: Context, r: Reminder) {
        cancel(context, r.id)
        if (r.enabled && !r.done && r.triggerAt > System.currentTimeMillis()) set(context, r.triggerAt, pending(context, r.id, false))
    }

    fun snooze(context: Context, id: Long) {
        set(context, System.currentTimeMillis() + SNOOZE_MINUTES * 60_000, pending(context, id, true))
    }

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(pending(context, id, false))
        am.cancel(pending(context, id, true))
        NotificationManagerCompat.from(context).cancel(id.toInt())
    }

    /**
     * Tüm etkin hatırlatmaları yeniden kurar (açılış, yeniden başlatma, saat değişimi).
     * Kaçırılmış tekrarlı hatırlatmalar bir sonraki tekrara ilerletilir; kaçırılmış tek seferlikler bildirilir.
     */
    suspend fun rescheduleAll(context: Context) {
        val dao = ReminderDatabase.get(context).reminders()
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now()
        dao.all().forEach { r ->
            if (!r.enabled || r.done) { cancel(context, r.id); return@forEach }
            if (r.triggerAt > System.currentTimeMillis()) { schedule(context, r); return@forEach }
            val anchor = java.time.Instant.ofEpochMilli(r.anchorAt).atZone(zone).toLocalDateTime()
            val next = RepeatLogic.nextAfter(anchor, r.repeatRule, now)
            if (next == null) {
                notify(context, r, missed = true)
                dao.update(r.copy(done = true))
            } else {
                val updated = r.copy(triggerAt = next.atZone(zone).toInstant().toEpochMilli())
                dao.update(updated)
                schedule(context, updated)
            }
        }
    }

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Hatırlatmalar", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Zamanı gelen hatırlatmalar"
                enableVibration(true)
            })
        }
    }

    fun notify(context: Context, r: Reminder, missed: Boolean = false) {
        if (!notificationsAllowed(context)) return
        ensureChannel(context)
        val id = r.id.toInt()
        val open = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun action(a: String, code: Int) = PendingIntent.getBroadcast(context, id * 10 + code,
            Intent(context, ReminderReceiver::class.java).setAction(a).putExtra(EXTRA_ID, r.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val time = TrFormat.formatTime(r.triggerDateTime().toLocalTime())
        val text = buildString {
            if (missed) append("Kaçırılan hatırlatma · ")
            append(time)
            if (r.note.isNotBlank()) append(" · ${r.note}")
        }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, "Tamamlandı", action(ACTION_DONE, 1))
            .addAction(0, "$SNOOZE_MINUTES dk ertele", action(ACTION_SNOOZE, 2))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (e: SecurityException) {
            Log.w("Reminder", "Bildirim izni yok", e)
        }
    }

}
