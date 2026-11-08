package com.galaxyoffline.chess.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.galaxyoffline.chess.engine.AnalysisRequest
import com.galaxyoffline.chess.engine.AnalysisUpdate
import com.galaxyoffline.chess.engine.EngineSession
import com.galaxyoffline.chess.engine.ProcessEngineIO
import com.galaxyoffline.chess.engine.SearchLimit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.Executors

/** Motor ayarları (Ayarlar ekranından değiştirilir; S25 ölçümüne göre varsayılanlar güncellenecek). */
data class EngineSettings(val threads: Int, val hashMb: Int, val analysisMs: Long) {
    companion object {
        val CORES: Int = Runtime.getRuntime().availableProcessors()
        /** Varsayılan: çekirdek sayısının bir eksiği (arayüz için bir çekirdek boş kalır), en az 1, en fazla 7. */
        val DEFAULT_THREADS = (CORES - 1).coerceIn(1, 7)
        const val DEFAULT_HASH = 64
        /** 0 = süresiz (kullanıcı durdurana kadar). */
        const val DEFAULT_MS = 3_000L
    }
}

enum class EngineState { NOT_STARTED, READY, MISSING, ERROR }

/**
 * Uygulama genelinde tek motor süreci. Tüm motor çağrıları tek bir arka plan iş parçacığında sıralanır
 * (oturum çağrıları kısa süre bekleyebilir; arayüz iş parçacığı hiçbir zaman beklemez).
 */
class EngineHolder private constructor(private val context: Context) {
    private val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "motor-istemci").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())
    private val prefs = context.getSharedPreferences("motor", Context.MODE_PRIVATE)
    private var session: EngineSession? = null
    private val stopProcess = Runnable { exec.execute { session?.close(); session = null; _state.value = EngineState.NOT_STARTED } }

    private val _state = MutableStateFlow(if (binary().exists()) EngineState.NOT_STARTED else EngineState.MISSING)
    val state: StateFlow<EngineState> = _state.asStateFlow()
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<EngineSettings> = _settings.asStateFlow()
    @Volatile var engineName: String? = null; private set

    /** Android, yerel kütüphane klasöründeki dosyaları çalıştırmaya izin verir (Android 10+ uygulama verisinden çalıştırmayı yasaklar). */
    fun binary() = File(context.applicationInfo.nativeLibraryDir, "libstockfish.so")

    private fun loadSettings() = EngineSettings(
        prefs.getInt("threads", EngineSettings.DEFAULT_THREADS), prefs.getInt("hash", EngineSettings.DEFAULT_HASH),
        prefs.getLong("ms", EngineSettings.DEFAULT_MS))

    fun updateSettings(s: EngineSettings) {
        prefs.edit().putInt("threads", s.threads).putInt("hash", s.hashMb).putLong("ms", s.analysisMs).apply()
        _settings.value = s
        exec.execute { session?.let { it.setOption("Threads", s.threads.toString()); it.setOption("Hash", s.hashMb.toString()) } }
    }

    private fun sessionOrNull(): EngineSession? {
        session?.let { return it }
        if (!binary().exists()) { _state.value = EngineState.MISSING; return null }
        val st = _settings.value
        val s = EngineSession({ ProcessEngineIO(binary(), context.filesDir) })
        s.setOption("Threads", st.threads.toString()); s.setOption("Hash", st.hashMb.toString())
        return try { s.start(); engineName = s.engineName; session = s; _state.value = EngineState.READY; s } catch (e: Exception) {
            s.close(); _state.value = EngineState.ERROR; null
        }
    }

    /**
     * Analiz/arama başlatır. [strengthElo] verilirse motor gücü sınırlanır (bilgisayara karşı oyun);
     * null ise tam güç. Güncellemeler ana iş parçacığında [onUpdate]'e gelir.
     */
    fun analyze(fen: String, limit: SearchLimit, multiPv: Int, strengthElo: Int? = null, onUpdate: (AnalysisUpdate) -> Unit) {
        main.removeCallbacks(stopProcess)
        exec.execute {
            val s = sessionOrNull()
            if (s == null) {
                val msg = if (_state.value == EngineState.MISSING) "Motor bu cihazın işlemci mimarisi için bulunamadı." else "Motor başlatılamadı."
                main.post { onUpdate(AnalysisUpdate(-1, fen, emptyList(), null, true, 0, msg)) }
                return@execute
            }
            s.setOption("UCI_LimitStrength", (strengthElo != null).toString())
            if (strengthElo != null) s.setOption("UCI_Elo", strengthElo.toString())
            s.analyze(AnalysisRequest(fen, limit, multiPv)) { u -> main.post { onUpdate(u) } }
        }
    }

    fun stop() = exec.execute { session?.stop() }
    fun cancel() = exec.execute { session?.cancel() }

    /** Motor testi için: oturumu doğrudan kullanır (ölçüm sırasında başka istek gönderilmemeli). */
    fun <T> withSession(block: (EngineSession?) -> T, onResult: (T) -> Unit) = exec.execute {
        val r = block(sessionOrNull()); main.post { onResult(r) }
    }

    fun onBackground() { cancel(); main.postDelayed(stopProcess, 60_000) }
    fun onForeground() { main.removeCallbacks(stopProcess) }

    /** Testler için: süren aramayı iptal edip sürecin hâlâ çalışıp çalışmadığını bildirir. */
    val restarts: Int get() = session?.restarts ?: 0

    companion object {
        @Volatile private var instance: EngineHolder? = null
        fun get(c: Context): EngineHolder = instance ?: synchronized(this) { instance ?: EngineHolder(c.applicationContext).also { instance = it } }
    }
}
