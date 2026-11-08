package com.galaxyoffline.billreminder.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.galaxyoffline.billreminder.MainActivity
import com.galaxyoffline.billreminder.R
import com.galaxyoffline.core.common.TrFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Günde bir kez (yaklaşık 09:00) çalışan yerel kontrol. Tam zamanlı alarm izni gerektirmez:
 * Android pil durumuna göre birkaç dakika kaydırabilir, faturalar için bu yeterlidir.
 */
object DailyCheck {
    private const val CHANNEL = "faturalar"

    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        var first = LocalDate.now().atTime(9, 0)
        if (!first.isAfter(LocalDateTime.now())) first = first.plusDays(1)
        val pi = PendingIntent.getBroadcast(context, 1, Intent(context, CheckReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, first.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), AlarmManager.INTERVAL_DAY, pi)
    }

    suspend fun run(context: Context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) nm.createNotificationChannel(NotificationChannel(CHANNEL, "Fatura hatırlatmaları", NotificationManager.IMPORTANCE_DEFAULT))
        val prefs = context.getSharedPreferences("bildirilen", Context.MODE_PRIVATE)
        val today = LocalDate.now()
        BillDatabase.get(context).bills().activeBills().forEach { b ->
            val st = BillLogic.status(b, today)
            if (st != BillStatus.OVERDUE && st != BillStatus.DUE_SOON) return@forEach
            val key = "${b.id}_${b.dueDate}_$today"
            if (prefs.getBoolean(key, false)) return@forEach
            val open = PendingIntent.getActivity(context, b.id.toInt(), Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val n = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("${b.kindEnum.emoji} ${b.name}")
                .setContentText("${TrFormat.money(b.amount)} · ${BillLogic.daysText(b, today)} (${TrFormat.formatDate(b.due)})")
                .setContentIntent(open).setAutoCancel(true).build()
            runCatching { NotificationManagerCompat.from(context).notify(b.id.toInt(), n) }
            prefs.edit().putBoolean(key, true).apply()
        }
    }
}

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class CheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val p = goAsync()
        scope.launch { try { DailyCheck.run(context) } catch (_: Exception) { } finally { p.finish() } }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { DailyCheck.schedule(context) }
}
