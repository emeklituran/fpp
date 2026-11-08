package com.galaxyoffline.alarm.ring

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.galaxyoffline.alarm.MainActivity
import com.galaxyoffline.alarm.data.Alarm
import com.galaxyoffline.alarm.data.AlarmDatabase
import com.galaxyoffline.alarm.data.AlarmLogic
import java.time.LocalDateTime
import java.time.ZoneId

/** Sistem alarm saati API'si (setAlarmClock): Doze modunda bile tam zamanında çalar, durum çubuğunda görünür. */
object AlarmScheduler {
    const val ACTION_FIRE = "com.galaxyoffline.alarm.FIRE"
    const val EXTRA_ID = "id"
    const val EXTRA_SNOOZE = "snooze"

    private fun operation(context: Context, id: Long, snooze: Boolean): PendingIntent =
        PendingIntent.getBroadcast(context, (id * 2 + if (snooze) 1 else 0).toInt(),
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_ID, id).putExtra(EXTRA_SNOOZE, snooze),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun show(context: Context): PendingIntent = PendingIntent.getActivity(context, 0,
        Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun canSchedule(context: Context) = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun setAt(context: Context, at: Long, op: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java)
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show(context)), op)
        } catch (e: SecurityException) {
            Log.w("Alarm", "Tam zamanlı alarm izni yok", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        }
    }

    /** Etkinse bir sonraki çalma anını kurar ve epoch ms olarak döner. */
    fun schedule(context: Context, a: Alarm, now: LocalDateTime = LocalDateTime.now()): Long? {
        cancel(context, a.id, includeSnooze = false)
        if (!a.enabled) return null
        val at = AlarmLogic.nextTrigger(a, now).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setAt(context, at, operation(context, a.id, false))
        return at
    }

    fun snooze(context: Context, id: Long, minutes: Int) {
        setAt(context, System.currentTimeMillis() + minutes * 60_000L, operation(context, id, true))
    }

    fun cancel(context: Context, id: Long, includeSnooze: Boolean = true) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(operation(context, id, false))
        if (includeSnooze) am.cancel(operation(context, id, true))
    }

    suspend fun rescheduleAll(context: Context) {
        AlarmDatabase.get(context).alarms().all().forEach { if (it.enabled) schedule(context, it) else cancel(context, it.id) }
    }
}
