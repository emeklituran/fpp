import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.MoveList
import org.junit.Test

class SanEval {
    private fun t(name: String, fen: String?, san: String) {
        val r = runCatching {
            val ml = if (fen == null) MoveList() else MoveList(fen)
            ml.loadFromSan(san); ml.toSan()
        }
        println("SAN [$name] '$san' -> " + r.fold({ "OK '$it'" }, { "HATA ${it::class.simpleName}: ${it.message?.take(120)}" }))
    }
    @Test fun cases() {
        t("terfi FEN'den", "8/4P1k1/8/8/8/8/8/4K3 w - - 0 1", "e8=Q")
        t("terfi FEN'den eşitsiz", "8/4P1k1/8/8/8/8/8/4K3 w - - 0 1", "e8Q")
        t("terfi başlangıçtan", null, "e4 d5 exd5 c6 dxc6 Nf6 cxb7 Nbd7 bxa8=Q")
        t("alt terfi", null, "e4 d5 exd5 c6 dxc6 Nf6 cxb7 Nbd7 bxa8=N")
        t("belirsiz kale", "4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1", "Rad1")
        t("belirsiz kale ayrımsız", "4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1", "Rd1")
        t("rok", null, "e4 e5 Nf3 Nc6 Bc4 Bc5 O-O")
        t("geçerken alma", null, "e4 a6 e5 d5 exd6")
        t("şah işareti", null, "e4 e5 Qh5 Nc6 Bc4 Nf6 Qxf7#")
        // FEN ile başlatıldığında FEN'in yeniden yüklenip yüklenmediği
        val b = Board(); b.loadFromFen("8/4P1k1/8/8/8/8/8/4K3 w - - 0 1")
        println("SAN [Board.doMove e7e8q] " + runCatching { b.doMove("e7e8q"); b.fen }.fold({ "OK $it" }, { "HATA $it" }))
    }
}
