import com.github.bhlangonijr.chesslib.Board
import org.junit.Assert.assertEquals
import org.junit.Test

/** chesslib değerlendirmesi: perft. Kaynaklar: Stockfish sf_19 tests/perft.sh (derin), Chess Programming Wiki (sığ). */
class PerftEval {
    private fun perft(b: Board, d: Int): Long {
        val moves = b.legalMoves()
        if (d == 1) return moves.size.toLong()
        var n = 0L
        for (m in moves) { b.doMove(m); n += perft(b, d - 1); b.undoMove() }
        return n
    }
    private fun check(fen: String, d: Int, expected: Long, tag: String) {
        val b = Board(); b.loadFromFen(fen)
        val t = System.nanoTime(); val got = perft(b, d); val ms = (System.nanoTime() - t) / 1_000_000
        println("PERFT $tag d$d expected=$expected got=$got ${if (got == expected) "OK" else "MISMATCH"} ${ms}ms")
        assertEquals("$tag d$d", expected, got)
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
    }
    // Hızlı set (Chess Programming Wiki değerleri)
    @Test fun startpos() = listOf(20L, 400, 8902, 197281, 4865609).forEachIndexed { i, v -> check(START, i + 1, v, "start") }
    @Test fun kiwipete() = listOf(48L, 2039, 97862, 4085603).forEachIndexed { i, v -> check(KIWI, i + 1, v, "kiwipete") }
    @Test fun pos3() = listOf(14L, 191, 2812, 43238, 674624, 11030083).forEachIndexed { i, v -> check(P3, i + 1, v, "pos3") }
    @Test fun pos4() = listOf(6L, 264, 9467, 422333).forEachIndexed { i, v -> check(P4, i + 1, v, "pos4") }
    @Test fun pos4mirror() = listOf(6L, 264, 9467, 422333).forEachIndexed { i, v -> check(P4M, i + 1, v, "pos4m") }
    @Test fun pos5() = listOf(44L, 1486, 62379, 2103487).forEachIndexed { i, v -> check(P5, i + 1, v, "pos5") }
    @Test fun pos6() = listOf(46L, 2079, 89890, 3894594).forEachIndexed { i, v -> check(P6, i + 1, v, "pos6") }

    // Derin set (Stockfish sf_19 tests/perft.sh, standart konumlar)
    private val deep get() = System.getProperty("deep") == "true"
    @Test fun deepSet() {
        if (System.getProperty("d7") == "true") { check(START, 7, 3195901860L, "sf-start"); return }
        check(SF7, 5, 11609488, "sf-pos7")
        if (!deep) return
        check(P5, 5, 89941194, "sf-pos5")
        check(P6, 5, 164075551, "sf-pos6")
        check(P3, 7, 178633661, "sf-pos3")
        check(KIWI, 5, 193690690, "sf-kiwipete")
        check(START, 6, 119060324, "start")
        check(P4, 6, 706045033, "sf-pos4")
    }

    // Zor durumlar (Martin Sedlak listesi; değerler bellekten — eşleşme iki bağımsız kaynağın uyumu demektir, CI'da Stockfish ile ayrıca doğrulanacak)
    @Test fun tricky() {
        listOf(
            Triple("3k4/3p4/8/K1P4r/8/8/8/8 b - - 0 1", 6, 1134888L),       // geçerken alma açmazı
            Triple("8/8/4k3/8/2p5/8/B2P2K1/8 w - - 0 1", 6, 1015133L),
            Triple("8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1", 6, 1440467L),     // geçerken alma şah çeker
            Triple("5k2/8/8/8/8/8/8/4K2R w K - 0 1", 6, 661072L),           // kısa rok şah çeker
            Triple("3k4/8/8/8/8/8/8/R3K3 w Q - 0 1", 6, 803711L),           // uzun rok şah çeker
            Triple("r3k2r/1b4bq/8/8/8/8/7B/R3K2R w KQkq - 0 1", 4, 1274206L),
            Triple("r3k2r/8/3Q4/8/8/5q2/8/R3K2R b KQkq - 0 1", 4, 1720476L), // rok engellenmiş
            Triple("2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1", 6, 3821001L),       // şahtan terfi ile çıkış
            Triple("8/8/1P2K3/8/2n5/1q6/8/5k2 b - - 0 1", 5, 1004658L),     // açarak şah
            Triple("4k3/1P6/8/8/8/8/K7/8 w - - 0 1", 6, 217342L),           // terfi ile şah
            Triple("8/P1k5/K7/8/8/8/8/8 w - - 0 1", 6, 92683L),             // alt terfi ile şah
            Triple("K1k5/8/P7/8/8/8/8/8 w - - 0 1", 6, 2217L),              // kendini pat
            Triple("8/k1P5/8/1K6/8/8/8/8 w - - 0 1", 7, 567584L),           // pat ve mat
            Triple("8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1", 4, 23527L),
        ).forEachIndexed { i, (f, d, n) -> check(f, d, n, "tricky${i + 1}") }
    }
}
