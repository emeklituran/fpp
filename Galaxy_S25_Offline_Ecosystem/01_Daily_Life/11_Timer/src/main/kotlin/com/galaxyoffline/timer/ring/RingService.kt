package com.galaxyoffline.timer.ring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.galaxyoffline.timer.R
import com.galaxyoffline.timer.data.Phase
import com.galaxyoffline.timer.data.RunStatus
import com.galaxyoffline.timer.data.TimerEngine
import com.galaxyoffline.timer.data.TimerMode
import com.galaxyoffline.timer.data.TimerStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Süre dolunca tetiklenir: durumu "bitti" yapar ve uyarı hizmetini başlatır. */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerStore.load(context)
        if (TimerStore.state.value.status != RunStatus.RUNNING) return
        RingService.fire(context)
    }
}

/** Uyarı sesi + titreşim; tam ekran bildirimle kilit ekranında [RingActivity] açılır. */
class RingService : Service() {
    companion object {
        const val ACTION_RING = "com.galaxyoffline.timer.RING"
        const val ACTION_STOP = "com.galaxyoffline.timer.STOP"
        const val ACTION_PLUS = "com.galaxyoffline.timer.PLUS"
        const val ACTION_NEXT = "com.galaxyoffline.timer.NEXT"
        private const val CHANNEL = "zamanlayici_bitti"
        private const val NOTIF = 78
        private const val AUTO_STOP_MS = 2 * 60_000L
        private val _ringing = MutableStateFlow(false)
        val ringing: StateFlow<Boolean> = _ringing.asStateFlow()

        /** Bitişi kaydeder ve uyarıyı başlatır (alarmdan veya gecikmiş tespit edilince). */
        fun fire(c: Context) {
            TimerStore.update(c) { TimerEngine.finish(it) }
            val s = TimerStore.state.value
            if (s.mode == TimerMode.POMODORO && s.phase == Phase.WORK) TimerStore.countSession(c)
            runCatching { ContextCompat.startForegroundService(c, Intent(c, RingService::class.java).setAction(ACTION_RING)) }
                .onFailure { Log.e("Zamanlayici", "Uyarı hizmeti başlatılamadı", it) }
        }

        fun command(c: Context, action: String) { runCatching { c.startService(Intent(c, RingService::class.java).setAction(action)) } }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> { foreground(); ring() }
            ACTION_PLUS -> { stopRinging(); TimerStore.update(this) { TimerEngine.addMinute(it, System.currentTimeMillis()) }; done() }
            ACTION_NEXT -> {
                stopRinging()
                TimerStore.update(this) { s -> TimerEngine.startPhase(s, TimerEngine.nextPhase(s, TimerStore.config.value), TimerStore.config.value, System.currentTimeMillis()) }
                done()
            }
            else -> { stopRinging(); TimerStore.update(this) { TimerEngine.reset(it) }; done() }
        }
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) nm.createNotificationChannel(NotificationChannel(CHANNEL, "Süre doldu", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(false); lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
        val full = PendingIntent.getActivity(this, 1, Intent(this, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun act(a: String, code: Int) = PendingIntent.getService(this, code, Intent(this, RingService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val s = TimerStore.state.value
        val b = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (s.mode == TimerMode.POMODORO) "${s.phase.label} bitti" else "Süre doldu")
            .setContentText(s.label.ifBlank { TimerEngine.format(s.totalMs) })
            .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX).setOngoing(true)
            .setFullScreenIntent(full, true).setContentIntent(full)
        if (s.mode == TimerMode.POMODORO) b.addAction(0, if (s.phase == Phase.WORK) "Molayı başlat" else "Çalışmaya başla", act(ACTION_NEXT, 4))
        else b.addAction(0, "+1 dk", act(ACTION_PLUS, 2))
        b.addAction(0, "Kapat", act(ACTION_STOP, 3))
        return b.build()
    }

    private fun foreground() {
        try { ServiceCompat.startForeground(this, NOTIF, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) }
        catch (e: Exception) { Log.e("Zamanlayici", "startForeground", e) }
    }

    private fun ring() {
        if (player != null) return
        _ringing.value = true
        wakeLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "galaxyoffline:zamanlayici").apply { acquire(AUTO_STOP_MS + 5_000) }
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(this@RingService, uri); isLooping = true; prepare(); start()
            }
        } catch (e: Exception) { Log.e("Zamanlayici", "Ses çalınamadı", e); null }
        runCatching { getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0)) }
        handler.postDelayed({ stopRinging(); done() }, AUTO_STOP_MS)
    }

    private fun stopRinging() {
        handler.removeCallbacksAndMessages(null)
        runCatching { player?.stop() }; runCatching { player?.release() }; player = null
        runCatching { getSystemService(VibratorManager::class.java).defaultVibrator.cancel() }
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }; wakeLock = null
        _ringing.value = false
    }

    private fun done() { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE); stopSelf() }

    override fun onDestroy() { stopRinging(); super.onDestroy() }
}
