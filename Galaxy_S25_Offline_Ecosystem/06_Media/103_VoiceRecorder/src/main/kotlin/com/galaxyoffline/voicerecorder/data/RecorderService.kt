package com.galaxyoffline.voicerecorder.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.galaxyoffline.voicerecorder.MainActivity
import com.galaxyoffline.voicerecorder.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class RecStatus { IDLE, RECORDING, PAUSED }

data class RecState(
    val status: RecStatus = RecStatus.IDLE,
    /** Duraklatmalar hariç biriken süre + şu anki bölümün başlangıcı (elapsedRealtime). */
    val accumulatedMs: Long = 0,
    val segmentStart: Long = 0,
    val amplitude: Float = 0f,
    val error: String? = null,
) {
    fun elapsed(now: Long = SystemClock.elapsedRealtime()) = accumulatedMs + if (status == RecStatus.RECORDING) now - segmentStart else 0
}

/** Ön plan hizmeti: ekran kapalıyken veya uygulama arka plandayken kayıt sürer. */
class RecorderService : Service() {
    companion object {
        private const val CHANNEL = "kayit"
        private const val ACTION_START = "baslat"
        private const val ACTION_PAUSE = "duraklat"
        private const val ACTION_RESUME = "devam"
        private const val ACTION_STOP = "durdur"
        private val _state = MutableStateFlow(RecState())
        val state: StateFlow<RecState> = _state.asStateFlow()

        fun start(c: Context, bitrate: Int) = ContextCompat.startForegroundService(c, Intent(c, RecorderService::class.java).setAction(ACTION_START).putExtra("bitrate", bitrate))
        fun pause(c: Context) = c.startService(Intent(c, RecorderService::class.java).setAction(ACTION_PAUSE))
        fun resume(c: Context) = c.startService(Intent(c, RecorderService::class.java).setAction(ACTION_RESUME))
        fun stop(c: Context) = c.startService(Intent(c, RecorderService::class.java).setAction(ACTION_STOP))
        fun clearError() { _state.value = _state.value.copy(error = null) }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var bitrate = 128_000
    private var meter: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> begin(intent.getIntExtra("bitrate", 128_000))
            ACTION_PAUSE -> pauseRec()
            ACTION_RESUME -> resumeRec()
            ACTION_STOP -> finish()
            else -> if (recorder == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) nm.createNotificationChannel(NotificationChannel(CHANNEL, "Kayıt durumu", NotificationManager.IMPORTANCE_LOW))
        fun act(a: String, code: Int) = PendingIntent.getService(this, code, Intent(this, RecorderService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE)
        val s = _state.value
        val b = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_notification).setOngoing(true).setOnlyAlertOnce(true)
            .setContentTitle(if (s.status == RecStatus.PAUSED) "Kayıt duraklatıldı" else "Ses kaydediliyor")
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
        if (s.status == RecStatus.RECORDING) b.setUsesChronometer(true).setWhen(System.currentTimeMillis() - s.elapsed())
        if (s.status == RecStatus.PAUSED) b.addAction(0, "Devam", act(ACTION_RESUME, 2)) else b.addAction(0, "Duraklat", act(ACTION_PAUSE, 1))
        b.addAction(0, "Durdur ve kaydet", act(ACTION_STOP, 3))
        return b.build()
    }

    private fun begin(rate: Int) {
        if (recorder != null) return
        bitrate = rate
        _state.value = RecState(RecStatus.RECORDING, 0, SystemClock.elapsedRealtime())
        try {
            ServiceCompat.startForeground(this, 1, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } catch (e: Exception) {
            Log.e("SesKayit", "startForeground", e)
            _state.value = RecState(error = "Kayıt başlatılamadı: mikrofon izni gerekli."); stopSelf(); return
        }
        val f = File(RecFiles.dir(this), "kayit_${System.currentTimeMillis()}.m4a")
        try {
            recorder = MediaRecorder(this).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(if (rate >= 128_000) 48_000 else 44_100)
                setAudioChannels(1)
                setAudioEncodingBitRate(rate)
                setOutputFile(f)
                prepare(); start()
            }
            file = f
            _state.value = RecState(RecStatus.RECORDING, 0, SystemClock.elapsedRealtime())
            startMeter()
        } catch (e: Exception) {
            Log.e("SesKayit", "start", e)
            runCatching { recorder?.release() }; recorder = null; f.delete()
            _state.value = RecState(error = "Mikrofon kullanılamıyor (başka bir uygulama kullanıyor olabilir).")
            stopForegroundAndSelf()
        }
    }

    private fun startMeter() {
        meter?.cancel()
        meter = scope.launch {
            while (isActive) {
                val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                if (_state.value.status == RecStatus.RECORDING) _state.value = _state.value.copy(amplitude = (amp / 32767f).coerceIn(0f, 1f))
                delay(80)
            }
        }
    }

    private fun pauseRec() {
        val r = recorder ?: return
        val s = _state.value; if (s.status != RecStatus.RECORDING) return
        runCatching { r.pause() }.onSuccess {
            _state.value = s.copy(status = RecStatus.PAUSED, accumulatedMs = s.elapsed(), amplitude = 0f); update()
        }
    }

    private fun resumeRec() {
        val r = recorder ?: return
        val s = _state.value; if (s.status != RecStatus.PAUSED) return
        runCatching { r.resume() }.onSuccess { _state.value = s.copy(status = RecStatus.RECORDING, segmentStart = SystemClock.elapsedRealtime()); update() }
    }

    private fun update() = runCatching { getSystemService(NotificationManager::class.java).notify(1, notification()) }

    private fun finish() {
        val r = recorder; val f = file
        val duration = _state.value.elapsed()
        meter?.cancel(); recorder = null; file = null
        var ok = false
        if (r != null) { ok = runCatching { r.stop() }.isSuccess; runCatching { r.release() } }
        _state.value = RecState()
        if (f != null) {
            if (ok && f.length() > 0) {
                val title = "Kayıt " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH.mm.ss"))
                val app = applicationContext
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching { RecordingDatabase.get(app).recordings().upsert(Recording(fileName = f.name, title = title, durationMs = duration, sizeBytes = f.length(), bitrate = bitrate, createdAt = System.currentTimeMillis())) }
                }
            } else { f.delete(); _state.value = RecState(error = "Kayıt çok kısa veya kaydedilemedi.") }
        }
        stopForegroundAndSelf()
    }

    private fun stopForegroundAndSelf() { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE); stopSelf() }

    override fun onDestroy() {
        if (recorder != null) finish()
        scope.cancel(); super.onDestroy()
    }
}
