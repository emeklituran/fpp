package com.galaxyoffline.chess.engine

/** Motor puanı; **hamle sırası olan tarafın** bakış açısındadır (UCI kuralı). */
sealed interface Score {
    data class Cp(val centipawns: Int) : Score
    /** Pozitif: hamle sırası olan taraf [moves] hamlede mat eder; negatif: mat olur. */
    data class Mate(val moves: Int) : Score

    /** Beyazın bakış açısına çevirir. */
    fun forWhite(whiteToMove: Boolean): Score = if (whiteToMove) this else when (this) {
        is Cp -> Cp(-centipawns)
        is Mate -> Mate(-moves)
    }

    /** Sıralama/karşılaştırma için tek sayı (mat, her puandan büyük). */
    fun sortValue(): Int = when (this) {
        is Cp -> centipawns
        is Mate -> if (moves > 0) 100_000 - moves else -100_000 - moves
    }
}

/** Bir "info" satırı (yalnızca kesin, sınırsız puanlı ve varyantlı satırlar kullanılır). */
data class PvLine(
    val multipv: Int,
    val depth: Int,
    val selDepth: Int?,
    val score: Score,
    val nodes: Long?,
    val nps: Long?,
    val timeMs: Long?,
    val pv: List<String>,
)

object Uci {
    /**
     * "info" satırını çözer. Varyantsız, puansız, alt/üst sınır (lowerbound/upperbound) ve "info string" satırları
     * için null döner: bunlar kesin sonuç değildir ve ekrana yansıtılmaz.
     */
    fun parseInfo(line: String): PvLine? {
        if (!line.startsWith("info ")) return null
        val t = line.split(' ').filter { it.isNotEmpty() }
        if (t.getOrNull(1) == "string") return null
        var depth: Int? = null; var sel: Int? = null; var multipv = 1; var score: Score? = null
        var nodes: Long? = null; var nps: Long? = null; var time: Long? = null; var pv: List<String> = emptyList()
        var bound = false
        var i = 1
        while (i < t.size) {
            when (t[i]) {
                "depth" -> { depth = t.getOrNull(++i)?.toIntOrNull() }
                "seldepth" -> { sel = t.getOrNull(++i)?.toIntOrNull() }
                "multipv" -> { multipv = t.getOrNull(++i)?.toIntOrNull() ?: 1 }
                "nodes" -> { nodes = t.getOrNull(++i)?.toLongOrNull() }
                "nps" -> { nps = t.getOrNull(++i)?.toLongOrNull() }
                "time" -> { time = t.getOrNull(++i)?.toLongOrNull() }
                "score" -> {
                    val kind = t.getOrNull(i + 1); val v = t.getOrNull(i + 2)?.toIntOrNull()
                    score = when (kind) { "cp" -> v?.let { Score.Cp(it) }; "mate" -> v?.let { Score.Mate(it) }; else -> null }
                    i += 2
                    if (t.getOrNull(i + 1) == "lowerbound" || t.getOrNull(i + 1) == "upperbound") { bound = true; i++ }
                }
                "pv" -> { pv = t.subList(i + 1, t.size).toList(); i = t.size }
            }
            i++
        }
        if (bound || depth == null || score == null || pv.isEmpty()) return null
        return PvLine(multipv, depth, sel, score, nodes, nps, time, pv)
    }

    /** "bestmove e2e4 ponder e7e5" → "e2e4"; "bestmove (none)" → null. */
    fun parseBestMove(line: String): String? =
        line.split(' ').getOrNull(1)?.takeIf { line.startsWith("bestmove ") && it != "(none)" }
}
