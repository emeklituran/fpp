package com.galaxyoffline.chess.rules

import com.github.bhlangonijr.chesslib.Board
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Hamle üretimi doğrulaması. Değer kaynakları:
 *  - Chess Programming Wiki "Perft Results" (sığ derinlikler)
 *  - Stockfish sf_19 `tests/perft.sh` (derin derinlikler)
 *  - Zor durumlar seti (Martin Sedlak); değerler CI'da Stockfish `go perft` ile doğrulandı (Chess Engine #1)
 */
class PerftTest {
    private fun perft(b: Board, d: Int): Long {
        val moves = b.legalMoves()
        if (d == 1) return moves.size.toLong()
        var n = 0L
        for (m in moves) { b.doMove(m); n += perft(b, d - 1); b.undoMove() }
        return n
    }
    private fun check(fen: String, d: Int, expected: Long) {
        val b = Board().apply { loadFromFen(fen) }
        assertEquals("$fen d$d", expected, perft(b, d))
    }

    @Test fun startpos() = listOf(20L, 400, 8902, 197281, 4865609).forEachIndexed { i, v -> check(START, i + 1, v) }
    @Test fun kiwipete() = listOf(48L, 2039, 97862, 4085603).forEachIndexed { i, v -> check(KIWI, i + 1, v) }
    @Test fun position3() = listOf(14L, 191, 2812, 43238, 674624, 11030083).forEachIndexed { i, v -> check(P3, i + 1, v) }
    @Test fun position4() = listOf(6L, 264, 9467, 422333).forEachIndexed { i, v -> check(P4, i + 1, v) }
    @Test fun position4Mirrored() = listOf(6L, 264, 9467, 422333).forEachIndexed { i, v -> check(P4M, i + 1, v) }
    @Test fun position5() = listOf(44L, 1486, 62379, 2103487).forEachIndexed { i, v -> check(P5, i + 1, v) }
    @Test fun position6() = listOf(46L, 2079, 89890, 3894594).forEachIndexed { i, v -> check(P6, i + 1, v) }
    @Test fun stockfishPosition7() = check(SF7, 5, 11609488)

    @Test fun trickyPositions() {
        TRICKY.forEach { (d, n, fen) -> check(fen, d, n) }
    }

    /** Oyun katmanının yasal hamle listesi chesslib ile aynı olmalı. */
    @Test fun gameLayerMatchesGenerator() {
        (listOf(START, KIWI, P3, P4, P4M, P5, P6, SF7) + TRICKY.map { it.third }).forEach { fen ->
            val g = (Game.fromFen(fen) as GameLoad.Ok).game
            assertEquals(fen, Board().apply { loadFromFen(fen) }.legalMoves().size, g.legalMoves().size)
        }
    }

    /** Derin set: yalnızca -PdeepPerft=true (ayrı CI işi, toplam ≈4,7 milyar düğüm). */
    @Test fun deepStockfishSet() {
        assumeTrue(System.getProperty("deepPerft") == "true")
        check(P5, 5, 89941194)
        check(P6, 5, 164075551)
        check(P3, 7, 178633661)
        check(KIWI, 5, 193690690)
        check(START, 6, 119060324)
        check(P4, 6, 706045033)
        check(START, 7, 3195901860L)
    }

    companion object {
        const val START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        const val KIWI = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
        const val P3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"
        const val P4 = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"
        const val P4M = "r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1"
        const val P5 = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"
        const val P6 = "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10"
        const val SF7 = "r7/4p3/5p1q/3P4/4pQ2/4pP2/6pp/R3K1kr w Q - 1 3"
        val TRICKY: List<Triple<Int, Long, String>> = PerftTest::class.java.getResource("/perft_tricky.txt")!!.readText()
            .lines().filter { it.isNotBlank() }.map { l -> val (d, n, fen) = l.split(" ", limit = 3); Triple(d.toInt(), n.toLong(), fen) }
    }
}
