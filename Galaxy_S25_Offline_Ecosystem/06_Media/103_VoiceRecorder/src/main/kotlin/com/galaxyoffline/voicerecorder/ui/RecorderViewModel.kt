package com.galaxyoffline.voicerecorder.ui

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.voicerecorder.data.RecFiles
import com.galaxyoffline.voicerecorder.data.Recording
import com.galaxyoffline.voicerecorder.data.RecordingDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlayState(val id: Long = -1, val playing: Boolean = false, val positionMs: Int = 0, val durationMs: Int = 0)

class RecorderViewModel(private val app: Application) : AndroidViewModel(app) {
    private val dao = RecordingDatabase.get(app).recordings()
    private val prefs = app.getSharedPreferences("ses_ayarlar", Context.MODE_PRIVATE)
    val messages = UiMessages()
    val recordings: StateFlow<List<Recording>?> = dao.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _play = MutableStateFlow(PlayState())
    val play: StateFlow<PlayState> = _play.asStateFlow()
    private val _bitrate = MutableStateFlow(prefs.getInt("bitrate", 128_000))
    val bitrate: StateFlow<Int> = _bitrate.asStateFlow()
    private var player: MediaPlayer? = null
    private var ticker: Job? = null

    init {
        // Veritabanında olmayan yetim dosyaları (ör. kayıt sırasında süreç sonlandıysa) temizlemez; yalnızca kaybolan dosyaların kaydını siler.
        launchSafe(messages) { withContext(Dispatchers.IO) { dao.all().filter { !RecFiles.file(app, it).exists() }.forEach { dao.delete(it.id) } } }
    }

    fun setBitrate(b: Int) { _bitrate.value = b; prefs.edit().putInt("bitrate", b).apply() }

    fun toggle(r: Recording) {
        val p = player
        if (p != null && _play.value.id == r.id) {
            if (p.isPlaying) { p.pause(); _play.value = _play.value.copy(playing = false) } else { p.start(); _play.value = _play.value.copy(playing = true); tick() }
            return
        }
        stopPlayer()
        try {
            val mp = MediaPlayer().apply {
                setDataSource(RecFiles.file(app, r).absolutePath)
                prepare()
                setOnCompletionListener { _play.value = _play.value.copy(playing = false, positionMs = 0); runCatching { seekTo(0) } }
                start()
            }
            player = mp
            _play.value = PlayState(r.id, true, 0, mp.duration)
            tick()
        } catch (e: Exception) {
            messages.post("Kayıt oynatılamadı (dosya bozuk olabilir).")
        }
    }

    private fun tick() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val p = player ?: break
                _play.value = _play.value.copy(positionMs = runCatching { p.currentPosition }.getOrDefault(0))
                if (!_play.value.playing) break
                delay(200)
            }
        }
    }

    fun seek(ms: Int) { player?.let { runCatching { it.seekTo(ms) }; _play.value = _play.value.copy(positionMs = ms) } }
    fun skip(deltaMs: Int) { val s = _play.value; seek((s.positionMs + deltaMs).coerceIn(0, s.durationMs)) }

    fun stopPlayer() {
        ticker?.cancel(); runCatching { player?.release() }; player = null; _play.value = PlayState()
    }

    fun rename(r: Recording, title: String) = launchSafe(messages, "Yeniden adlandırılamadı") {
        val t = title.trim().take(80); if (t.isNotEmpty()) dao.upsert(r.copy(title = t))
    }

    fun delete(r: Recording) = launchSafe(messages, "Silinemedi") {
        if (_play.value.id == r.id) stopPlayer()
        withContext(Dispatchers.IO) { RecFiles.file(app, r).delete() }
        dao.delete(r.id)
    }

    fun shareIntent(r: Recording): Intent? {
        val f = RecFiles.file(app, r); if (!f.exists()) return null
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.dosyalar", f)
        return Intent.createChooser(Intent(Intent.ACTION_SEND).setType("audio/mp4").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Kaydı paylaş")
    }

    /** Music/Ses Kayıtları klasörüne kopyalar (Android 10+ MediaStore; ek izin gerektirmez). */
    fun exportToMusic(r: Recording) = launchSafe(messages, "Dışa aktarılamadı") {
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, RecFiles.safeName(r.title) + ".m4a")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Ses Kayıtları")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val cr = app.contentResolver
            val uri = cr.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values) ?: error("insert")
            try {
                cr.openOutputStream(uri)!!.use { out -> RecFiles.file(app, r).inputStream().use { it.copyTo(out) } }
                cr.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) { cr.delete(uri, null, null); throw e }
        }
        messages.post("Müzik › Ses Kayıtları klasörüne kaydedildi")
    }

    override fun onCleared() { stopPlayer(); super.onCleared() }
}
