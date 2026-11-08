package com.galaxyoffline.chess.engine

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Testler için UCI davranışını taklit eden sahte motor.
 * [mode]: NORMAL (go → zamanla info + bestmove), HANG (stop'a yanıt vermez), CRASH_ON_GO (go gelince süreç kapanır),
 * SILENT (uci'ye yanıt vermez).
 */
class FakeEngine(private val mode: Mode = Mode.NORMAL, private val infoEveryMs: Long = 20) : EngineIO {
    enum class Mode { NORMAL, HANG, CRASH_ON_GO, SILENT }
    private val out = LinkedBlockingQueue<String>()
    @Volatile private var alive = true
    @Volatile private var searching: Thread? = null
    val received = java.util.Collections.synchronizedList(mutableListOf<String>())
    private var multiPv = 1
    private var fen = ""
    private val eof = "\u0000EOF"

    override fun send(line: String) {
        if (!alive) return
        received += line
        when {
            line == "uci" && mode != Mode.SILENT -> { out += "id name FakeFish 1"; out += "uciok" }
            line == "isready" -> out += "readyok"
            line.startsWith("setoption name MultiPV value ") -> multiPv = line.substringAfterLast(' ').toInt()
            line.startsWith("position fen ") -> fen = line.removePrefix("position fen ")
            line.startsWith("go") -> {
                if (mode == Mode.CRASH_ON_GO) { alive = false; out += eof; return }
                val budget = line.split(' ').let { t -> if (t.getOrNull(1) == "movetime") t[2].toLong() else Long.MAX_VALUE }
                val searchFen = fen; val pvCount = multiPv
                searching = Thread {
                    val start = System.currentTimeMillis(); var depth = 1
                    try {
                        while (System.currentTimeMillis() - start < budget) {
                            for (k in 1..pvCount) out += "info depth $depth seldepth ${depth + 2} multipv $k score cp ${100 - k * 10} nodes ${depth * 1000} nps 500000 time ${System.currentTimeMillis() - start} pv m${k}_${searchFen.hashCode()} e7e5"
                            out += "info depth $depth score cp 999 lowerbound nodes 1 pv x"
                            depth++
                            Thread.sleep(infoEveryMs)
                        }
                    } catch (_: InterruptedException) { }
                    if (mode != Mode.HANG) out += "bestmove m1_${searchFen.hashCode()} ponder e7e5"
                }.apply { isDaemon = true; start() }
            }
            line == "stop" -> if (mode != Mode.HANG) searching?.interrupt()
            line == "quit" -> { alive = false; out += eof }
        }
    }

    override fun readLine(): String? {
        while (true) {
            val l = out.poll(100, TimeUnit.MILLISECONDS)
            if (l == eof) return null
            if (l != null) return l
            if (!alive && out.isEmpty()) return null
        }
    }
    override fun isAlive() = alive
    override fun kill() { alive = false; searching?.interrupt(); out += eof }

    companion object { fun pvTag(fen: String) = "m1_${fen.hashCode()}" }
}
