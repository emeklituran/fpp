package com.galaxyoffline.alarm.ring

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
import com.galaxyoffline.alarm.R
import com.galaxyoffline.alarm.data.Alarm
import com.galaxyoffline.alarm.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RingInfo(val alarmId: Long, val label: String, val hour: Int, val minute: Int, val snoozeMinutes: Int, val preview: Boolean)

/**
 * Alarm sesini ve titreşimini çalan ön plan hizmeti. Tam ekran bildirimle kilit ekranında [RingActivity] açılır.
 * Kullanıcı kapatmazsa [AUTO_STOP_MS] sonra kendiliğinden susar.
 */
class AlarmRingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> {
                val id = intent.getLongExtra(EXTRA_ID, -1)
                val preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
                // Android, startForegroundService sonrası birkaç saniye içinde startForeground ister.
                startInForeground(RingInfo(id, "", 0, 0, 10, preview))
                scope.launch {
                    val a: Alarm? = withContext(Dispatchers.IO) { runCatching { AlarmDatabase.get(this@AlarmRingService).alarms().get(id) }.getOrNull() }
                    val now = java.time.LocalTime.now()
                    val info = RingInfo(id, a?.label.orEmpty(), a?.hour ?: now.hour, a?.minute ?: now.minute, a?.snoozeMinutes ?: 10, preview)
                    startInForeground(info)
                    startRinging(vibrate = a?.vibrate ?: true)
                }
            }
            ACTION_SNOOZE -> {
                val info = _ringing.value
                if (info != null && !info.preview) AlarmScheduler.snooze(this, info.alarmId, info.snoozeMinutes)
                stopAll()
            }
            ACTION_DISMISS -> stopAll()
            else -> stopAll()
        }
        return START_NOT_STICKY
    }

    private fun startInForeground(info: RingInfo) {
        _ringing.value = info
        ensureChannel(this)
        val fullScreen = PendingIntent.getActivity(this, 1, Intent(this, RingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun action(a: String, code: Int) = PendingIntent.getService(this, code, Intent(this, AlarmRingService::class.java).setAction(a),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val title = "%02d:%02d".format(info.hour, info.minute) + if (info.label.isNotBlank()) " · ${info.label}" else ""
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (info.preview) "Alarm önizlemesi" else "Alarm")
            .setContentText(title)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, "Ertele", action(ACTION_SNOOZE, 2))
            .addAction(0, "Kapat", action(ACTION_DISMISS, 3))
            .build()
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } catch (e: Exception) {
            Log.e("Alarm", "Ön plan hizmeti başlatılamadı", e)
        }
    }

    private fun startRinging(vibrate: Boolean) {
        if (player != null) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "galaxyoffline:alarm").apply { acquire(AUTO_STOP_MS + 5_000) }
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(this@AlarmRingService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("Alarm", "Alarm sesi çalınamadı", e); null
        }
        if (vibrate) {
            runCatching {
                getSystemService(VibratorManager::class.java).defaultVibrator
                    .vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
            }
        }
        handler.postDelayed({ stopAll() }, AUTO_STOP_MS)
    }

    private fun stopAll() {
        handler.removeCallbacksAndMessages(null)
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { getSystemService(VibratorManager::class.java).defaultVibrator.cancel() }
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
        _ringing.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (_ringing.value != null) stopAll()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_RING = "com.galaxyoffline.alarm.RING"
        const val ACTION_SNOOZE = "com.galaxyoffline.alarm.SNOOZE"
        const val ACTION_DISMISS = "com.galaxyoffline.alarm.DISMISS"
        const val EXTRA_ID = "id"
        const val EXTRA_PREVIEW = "preview"
        const val CHANNEL = "alarm_calma"
        const val NOTIFICATION_ID = 4242
        const val AUTO_STOP_MS = 10 * 60 * 1000L

        private val _ringing = MutableStateFlow<RingInfo?>(null)
        val ringing: StateFlow<RingInfo?> = _ringing.asStateFlow()

        fun ringIntent(context: Context, id: Long, preview: Boolean) =
            Intent(context, AlarmRingService::class.java).setAction(ACTION_RING).putExtra(EXTRA_ID, id).putExtra(EXTRA_PREVIEW, preview)

        fun command(context: Context, action: String) {
            runCatching { context.startService(Intent(context, AlarmRingService::class.java).setAction(action)) }
        }

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL, "Çalan alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alarm çaldığında gösterilir"
                    setSound(null, null) // ses hizmet tarafından alarm akışında çalınır
                    enableVibration(false)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                })
            }
        }
    }
}
