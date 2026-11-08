package com.galaxyoffline.chess.engine

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

sealed interface SearchLimit {
    data class Time(val millis: Long) : SearchLimit
    data class Nodes(val count: Long) : SearchLimit
    data object Infinite : SearchLimit
}

data class AnalysisRequest(val fen: String, val limit: SearchLimit, val multiPv: Int = 3)

/**
 * Analiz güncellemesi. [lines] hamle sırası olan tarafın bakış açısındadır ve multipv sırasıyla gelir.
 * [final] true ise bu arama bitti (bestmove geldi, ya da hata).
 */
data class AnalysisUpdate(
    val generation: Long,
    val fen: String,
    val lines: List<PvLine>,
    val bestMove: String?,
    val final: Boolean,
    val elapsedMs: Long,
    val error: String? = null,
)

class EngineException(message: String) : Exception(message)

/**
 * Tek bir UCI motor sürecini yönetir:
 *  - Yeni analiz isteğinde süren arama durdurulur, onun sonuçları **asla** dinleyiciye gitmez (sıra numarası ile).
 *  - Süre/düğüm sınırı + pay aşılırsa önce "stop", yanıt gelmezse süreç öldürülüp yeniden başlatılır.
 *  - Süreç beklenmedik kapanırsa dinleyiciye hata bildirilir; sonraki istekte motor yeniden başlatılır.
 * Dinleyici okuyucu iş parçacığında çağrılır; arayüz tarafı kendi iş parçacığına taşımalıdır.
 */
class EngineSession(
    private val factory: () -> EngineIO,
    private val throttleMs: Long = 125,
    private val stopGraceMs: Long = 2_000,
    private val killGraceMs: Long = 1_500,
) {
    private val lock = Object()
    private var io: EngineIO? = null
    private var reader: Thread? = null
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "motor-zamanlayici").apply { isDaemon = true } }

    private var generation = 0L
    private var current: Search? = null
    private var pendingHandshake: CountDownLatch? = null
    private var pendingReady: CountDownLatch? = null
    private val desired = linkedMapOf<String, String>()
    private val applied = mutableMapOf<String, String>()
    @Volatile var engineName: String? = null; private set
    @Volatile var restarts = 0; private set

    private class Search(
        val gen: Long, val request: AnalysisRequest, val listener: (AnalysisUpdate) -> Unit, val startNs: Long,
    ) {
        val lines = sortedMapOf<Int, PvLine>()
        var done = false
        var cancelled = false
        var lastNotifyNs = 0L
        val finished = CountDownLatch(1)
        fun elapsed() = (System.nanoTime() - startNs) / 1_000_000
    }

    /** Motor seçeneği (Threads, Hash, MultiPV, UCI_LimitStrength, UCI_Elo…); bir sonraki aramadan önce uygulanır. */
    fun setOption(name: String, value: String) = synchronized(lock) { desired[name] = value }

    /** Süreci başlatır ve UCI el sıkışmasını yapar. Hata durumunda [EngineException]. */
    fun start() = synchronized(lock) { ensureRunningLocked() }

    /** Yeni analiz başlatır; önceki arama (varsa) iptal edilir ve sonuçları atılır. Dönen değer aramanın sıra numarasıdır. */
    fun analyze(request: AnalysisRequest, listener: (AnalysisUpdate) -> Unit): Long = synchronized(lock) {
        cancelCurrentLocked()
        desired["MultiPV"] = request.multiPv.toString()
        try { ensureRunningLocked(); applyOptionsLocked() } catch (e: EngineException) {
            val gen = ++generation
            listener(AnalysisUpdate(gen, request.fen, emptyList(), null, true, 0, e.message))
            return gen
        }
        val gen = ++generation
        val s = Search(gen, request, listener, System.nanoTime())
        current = s
        val e = io ?: run {
            current = null
            listener(AnalysisUpdate(gen, request.fen, emptyList(), null, true, 0, "Motor çalışmıyor")); return gen
        }
        e.send("position fen ${request.fen}")
        e.send(when (val l = request.limit) {
            is SearchLimit.Time -> "go movetime ${l.millis}"
            is SearchLimit.Nodes -> "go nodes ${l.count}"
            SearchLimit.Infinite -> "go infinite"
        })
        val budget = (request.limit as? SearchLimit.Time)?.millis
        if (budget != null) scheduleWatchdog(gen, budget + stopGraceMs)
        gen
    }

    /** Süren aramayı durdurur; sonuç (bestmove ile birlikte) son güncelleme olarak gelir. */
    fun stop() = synchronized(lock) { if (current?.done == false) io?.send("stop") }

    /** Süren aramayı iptal eder; sonucu dinleyiciye gönderilmez. */
    fun cancel() = synchronized(lock) { cancelCurrentLocked() }

    fun close() {
        synchronized(lock) {
            current?.cancelled = true
            io?.send("quit"); io?.kill(); io = null
        }
        scheduler.shutdownNow()
    }

    // --- iç işleyiş ---

    private fun ensureRunningLocked() {
        if (io?.isAlive() == true) return
        io?.kill()
        if (engineName != null) restarts++
        applied.clear()
        val e = try { factory() } catch (t: Throwable) { throw EngineException("Motor başlatılamadı: ${t.message}") }
        io = e
        val hs = CountDownLatch(1); pendingHandshake = hs
        reader = Thread({ readLoop(e) }, "motor-okuyucu").apply { isDaemon = true; start() }
        e.send("uci")
        if (!awaitUnlocked(hs, 5_000) || io !== e || !e.isAlive()) { e.kill(); io = null; throw EngineException("Motor yanıt vermedi (uciok gelmedi)") }
        applyOptionsLocked()
        if (io !== e) throw EngineException("Motor ayarları uygulanırken kapandı")
    }

    private fun applyOptionsLocked() {
        val e = io ?: return
        val changes = desired.filter { applied[it.key] != it.value }
        if (changes.isEmpty()) return
        changes.forEach { (k, v) -> e.send("setoption name $k value $v"); applied[k] = v }
        val ready = CountDownLatch(1); pendingReady = ready
        e.send("isready")
        if (!awaitUnlocked(ready, 5_000)) { e.kill(); io = null; throw EngineException("Motor ayarları uygulanamadı (readyok gelmedi)") }
    }

    private fun cancelCurrentLocked() {
        val s = current ?: return
        if (!s.done) {
            s.cancelled = true
            io?.send("stop")
            // Eski aramanın bestmove'u gelmeden yeni pozisyon gönderilmez; böylece eski satırlar yeni aramaya karışmaz.
            // Arama `current` olarak kalır ki bestmove geldiğinde latch serbest kalsın (iptal edildiği için dinleyiciye gitmez).
            if (!awaitUnlocked(s.finished, 1_000)) { io?.kill(); io = null; s.done = true }
        }
        if (current === s) current = null
    }

    /** Kilidi bırakarak bekler (okuyucu iş parçacığı kilidi alabilsin). */
    private fun awaitUnlocked(latch: CountDownLatch, ms: Long): Boolean {
        val deadline = System.nanoTime() + ms * 1_000_000
        while (latch.count > 0) {
            val left = (deadline - System.nanoTime()) / 1_000_000
            if (left <= 0) return false
            (lock as Object).wait(minOf(left, 50))
        }
        return true
    }

    private fun scheduleWatchdog(gen: Long, afterMs: Long) {
        scheduler.schedule({
            synchronized(lock) {
                val s = current
                if (s == null || s.gen != gen || s.done) return@schedule
                io?.send("stop")
            }
            scheduler.schedule({
                synchronized(lock) {
                    val s = current
                    if (s == null || s.gen != gen || s.done) return@schedule
                    // Motor durma komutuna yanıt vermedi: süreç öldürülür, sonraki istekte yeniden başlatılır.
                    io?.kill(); io = null
                    finishLocked(s, error = "Motor zaman sınırında yanıt vermedi; yeniden başlatılacak.")
                }
            }, killGraceMs, TimeUnit.MILLISECONDS)
        }, afterMs, TimeUnit.MILLISECONDS)
    }

    private fun finishLocked(s: Search, bestMove: String? = null, error: String? = null) {
        if (s.done) return
        s.done = true
        s.finished.countDown()
        (lock as Object).notifyAll()
        if (!s.cancelled) s.listener(AnalysisUpdate(s.gen, s.request.fen, s.lines.values.toList(), bestMove, true, s.elapsed(), error))
    }

    private fun readLoop(e: EngineIO) {
        while (true) {
            val line = e.readLine()
            synchronized(lock) {
                if (line == null) {
                    if (io === e) io = null
                    pendingHandshake?.countDown(); pendingReady?.countDown()
                    current?.let { if (!it.done) finishLocked(it, error = "Motor beklenmedik şekilde kapandı; yeniden başlatılacak.") }
                    (lock as Object).notifyAll()
                    return
                }
                handleLocked(line)
            }
        }
    }

    private fun handleLocked(line: String) {
        when {
            line.startsWith("id name ") -> engineName = line.removePrefix("id name ").trim()
            line == "uciok" -> { pendingHandshake?.countDown(); (lock as Object).notifyAll() }
            line == "readyok" -> { pendingReady?.countDown(); (lock as Object).notifyAll() }
            line.startsWith("info ") -> {
                val s = current ?: return
                if (s.done || s.cancelled) return
                val pv = Uci.parseInfo(line) ?: return
                if (pv.multipv > s.request.multiPv) return
                s.lines[pv.multipv] = pv
                val now = System.nanoTime()
                if ((now - s.lastNotifyNs) / 1_000_000 >= throttleMs) {
                    s.lastNotifyNs = now
                    s.listener(AnalysisUpdate(s.gen, s.request.fen, s.lines.values.toList(), null, false, s.elapsed()))
                }
            }
            line.startsWith("bestmove") -> {
                val s = current
                if (s != null && !s.done) finishLocked(s, Uci.parseBestMove(line))
            }
        }
    }
}
