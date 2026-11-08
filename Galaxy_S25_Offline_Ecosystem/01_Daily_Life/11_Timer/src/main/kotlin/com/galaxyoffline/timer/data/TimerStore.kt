package com.galaxyoffline.timer.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.galaxyoffline.timer.MainActivity
import com.galaxyoffline.timer.R
import com.galaxyoffline.timer.ring.TimerReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Zamanlayıcı durumunun tek kaynağı. Her değişiklik SharedPreferences'a yazılır ve
 * bitiş anı için tam zamanlı alarm kurulur; uygulama kapalı olsa bile süre dolunca çalar.
 */
object TimerStore {
    private const val CH_RUN = "zamanlayici_suruyor"
    private const val RUN_NOTIF = 77
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()
    private val _config = MutableStateFlow(PomodoroConfig())
    val config: StateFlow<PomodoroConfig> = _config.asStateFlow()
    private val _today = MutableStateFlow(0)
    /** Bugün tamamlanan Pomodoro çalışma oturumu sayısı. */
    val todaySessions: StateFlow<Int> = _today.asStateFlow()
    @Volatile private var loaded = false

    private fun prefs(c: Context) = c.getSharedPreferences("zamanlayici", Context.MODE_PRIVATE)

    fun load(c: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val p = prefs(c)
            _state.value = TimerState(
                mode = runCatching { TimerMode.valueOf(p.getString("mode", null)!!) }.getOrDefault(TimerMode.TIMER),
                phase = runCatching { Phase.valueOf(p.getString("phase", null)!!) }.getOrDefault(Phase.TIMER),
                status = runCatching { RunStatus.valueOf(p.getString("status", null)!!) }.getOrDefault(RunStatus.IDLE),
                totalMs = p.getLong("total", 5 * 60_000L), endAt = p.getLong("end", 0), pausedRemaining = p.getLong("paused", 0),
                label = p.getString("label", "").orEmpty(), cycleWorkDone = p.getInt("cycle", 0),
            )
            _config.value = PomodoroConfig(p.getInt("w", 25), p.getInt("s", 5), p.getInt("l", 15), p.getInt("e", 4))
            _today.value = if (p.getLong("day", 0) == LocalDate.now().toEpochDay()) p.getInt("today", 0) else 0
            loaded = true
        }
    }

    fun update(c: Context, f: (TimerState) -> TimerState) {
        load(c)
        val old = _state.value
        val s = f(old)
        _state.value = s
        prefs(c).edit().putString("mode", s.mode.name).putString("phase", s.phase.name).putString("status", s.status.name)
            .putLong("total", s.totalMs).putLong("end", s.endAt).putLong("paused", s.pausedRemaining).putString("label", s.label)
            .putInt("cycle", s.cycleWorkDone).apply()
        if (s.status == RunStatus.RUNNING) schedule(c, s.endAt) else cancelAlarm(c)
        showRunning(c, s)
    }

    fun setConfig(c: Context, cfg: PomodoroConfig) {
        _config.value = cfg
        prefs(c).edit().putInt("w", cfg.workMin).putInt("s", cfg.shortMin).putInt("l", cfg.longMin).putInt("e", cfg.longEvery).apply()
    }

    fun countSession(c: Context) {
        val day = LocalDate.now().toEpochDay()
        val p = prefs(c)
        val n = (if (p.getLong("day", 0) == day) p.getInt("today", 0) else 0) + 1
        p.edit().putLong("day", day).putInt("today", n).apply()
        _today.value = n
    }

    /** Alarm gecikmiş tetiklenirse veya uygulama açıldığında süre çoktan dolduysa. */
    fun isOverdue(now: Long = System.currentTimeMillis()) = _state.value.status == RunStatus.RUNNING && _state.value.endAt <= now

    private fun pi(c: Context) = PendingIntent.getBroadcast(c, 1, Intent(c, TimerReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun canExact(c: Context) = c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun schedule(c: Context, at: Long) {
        val am = c.getSystemService(AlarmManager::class.java)
        if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(c))
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(c))
    }

    private fun cancelAlarm(c: Context) = c.getSystemService(AlarmManager::class.java).cancel(pi(c))

    /** Sayım sürerken geri sayım kronometreli sessiz bildirim (ön plan hizmeti gerekmez). */
    private fun showRunning(c: Context, s: TimerState) {
        val nm = NotificationManagerCompat.from(c)
        if (s.status != RunStatus.RUNNING && s.status != RunStatus.PAUSED) { nm.cancel(RUN_NOTIF); return }
        if (!nm.areNotificationsEnabled()) return
        val sys = c.getSystemService(NotificationManager::class.java)
        if (sys.getNotificationChannel(CH_RUN) == null) sys.createNotificationChannel(NotificationChannel(CH_RUN, "Süren zamanlayıcı", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val b = NotificationCompat.Builder(c, CH_RUN).setSmallIcon(R.drawable.ic_notification).setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setContentTitle(s.label.ifBlank { s.phase.label }).setContentIntent(open)
        if (s.status == RunStatus.RUNNING) b.setUsesChronometer(true).setChronometerCountDown(true).setWhen(s.endAt).setContentText("Süre işliyor")
        else b.setContentText("Duraklatıldı · ${TimerEngine.format(s.pausedRemaining)} kaldı")
        runCatching { nm.notify(RUN_NOTIF, b.build()) }
    }
}
