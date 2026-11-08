import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.MoveList
import org.junit.Test

/** chesslib davranış tespiti: sonuçlar yazdırılır, değerlendirme raporda yapılır. */
class BehaviorEval {
    private fun b(fen: String) = Board().apply { loadFromFen(fen) }
    private fun out(k: String, v: Any?) = println("BEHAV $k = $v")

    @Test fun insufficientMaterial() {
        mapOf(
            "KvK" to "8/8/4k3/8/8/4K3/8/8 w - - 0 1",
            "KBvK" to "8/8/4k3/8/8/4K3/5B2/8 w - - 0 1",
            "KNvK" to "8/8/4k3/8/8/4K3/5N2/8 w - - 0 1",
            "KNNvK (mat mümkün)" to "8/8/4k3/8/8/4K3/4NN2/8 w - - 0 1",
            "KNvKN (mat mümkün)" to "8/5n2/4k3/8/8/4K3/5N2/8 w - - 0 1",
            "KBvKN (mat mümkün)" to "8/5n2/4k3/8/8/4K3/5B2/8 w - - 0 1",
            "KBvKB aynı renk" to "8/5b2/4k3/8/8/4K3/6B1/8 w - - 0 1",
            "KBvKB zıt renk (mat mümkün)" to "8/6b1/4k3/8/8/4K3/6B1/8 w - - 0 1",
            "KBBvK aynı renk" to "8/8/4k3/8/8/4K3/4B1B1/8 w - - 0 1",
        ).forEach { (k, f) -> out("insufficient[$k]", b(f).isInsufficientMaterial) }
    }

    @Test fun drawRules() {
        val x = b("8/8/4k3/8/8/4K3/8/R7 w - - 100 80")
        out("isDraw(halfmove=100, KR v K)", x.isDraw)
        out("halfMoveCounter", x.halfMoveCounter)
    }

    @Test fun repetitionWithNonCapturableEp() {
        val bd = Board()
        bd.doMove("e2e4")
        val keyAfterE4 = bd.incrementalHashKey; val zAfterE4 = bd.zobristKey
        out("fen after e4 (default)", bd.fen)
        out("fen after e4 (only capturable ep)", bd.getFen(true, true))
        listOf("g8f6", "g1f3", "f6g8", "f3g1").forEach { bd.doMove(it) }
        out("same position, incKey equal", bd.incrementalHashKey == keyAfterE4)
        out("same position, zobrist equal", bd.zobristKey == zAfterE4)
        out("isRepetition(2) after 1 cycle", bd.isRepetition(2))
        listOf("g8f6", "g1f3", "f6g8", "f3g1").forEach { bd.doMove(it) }
        out("FIDE: 3 kez oluştu; chesslib isRepetition(3)", bd.isRepetition(3))
    }

    @Test fun repetitionPlain() {
        val bd = Board()
        repeat(2) { listOf("g1f3", "g8f6", "f3g1", "f6g8").forEach { bd.doMove(it) } }
        out("başlangıç 3 kez: isRepetition(3)", bd.isRepetition(3))
        repeat(2) { listOf("g1f3", "g8f6", "f3g1", "f6g8").forEach { bd.doMove(it) } }
        out("başlangıç 5 kez: isRepetition(5)", bd.isRepetition(5))
    }

    @Test fun fenStrictness() {
        listOf(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1",
            "8/8/8/8/8/8/8/8 w - - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq",
            "rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "garbage",
        ).forEach { f -> out("loadFromFen[$f]", runCatching { b(f).fen }.fold({ "KABUL -> $it" }, { "HATA ${it::class.simpleName}" })) }
    }

    @Test fun san() {
        val ml = MoveList()
        ml.loadFromSan("e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7")
        out("san roundtrip", ml.toSan())
        val p = MoveList("8/4P1k1/8/8/8/8/8/4K3 w - - 0 1")
        p.addSanMove("e8=Q")
        out("promotion san", p.toSan())
        val d = MoveList("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1")
        runCatching { d.addSanMove("Rd1") }.onFailure { out("Rd1 (ambiguous)", "HATA ${it.message}") }.onSuccess { out("Rd1 (ambiguous) accepted", d.toSan()) }
    }
}
