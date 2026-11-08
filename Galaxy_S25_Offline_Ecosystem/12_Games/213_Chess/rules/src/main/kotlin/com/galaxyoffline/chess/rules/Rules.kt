package com.galaxyoffline.chess.rules

/**
 * Konum kimliği (FIDE 9.2.3): taş yerleşimi, hamle sırası, rok hakları ve **yasal olarak mümkün** geçerken alma.
 * Üç/beş kez tekrar ve açılış tanıma bu kimlikle yapılır.
 */
data class PositionKey(val placement: String, val sideToMove: Color, val castling: String, val legalEnPassant: String)

/** Tam oyun durumu: konum kimliği + sayaçlar. Kayıt/geri yükleme eşitliği bununla denetlenir. */
data class GameStateKey(val position: PositionKey, val halfmoveClock: Int, val fullmoveNumber: Int)

/** Üç eşitlik düzeyi; ayrıntı: docs/F1_RAPORU.md §3 ve plan bölüm 3. */
object Equality {
    /** Metinsel: normalleştirilmiş FEN metinleri aynı mı (yazılan geçerken alma alanı korunur). */
    fun textual(a: FenPosition, b: FenPosition) = Fen.write(a) == Fen.write(b)
    fun position(a: FenPosition, b: FenPosition) = keyOf(a) == keyOf(b)
    fun fullState(a: FenPosition, b: FenPosition) = stateOf(a) == stateOf(b)

    fun keyOf(p: FenPosition): PositionKey = PositionKey(
        p.placementFen(), p.sideToMove, p.castling.toFen(),
        Chesslib.legalEnPassant(Chesslib.board(p), p.enPassant)?.name ?: "-",
    )
    fun stateOf(p: FenPosition) = GameStateKey(keyOf(p), p.halfmoveClock, p.fullmoveNumber)
}

/**
 * Ölü pozisyon (FIDE 5.2.2 / 9.6): hiçbir yasal hamle dizisiyle mat yapılamayan konum → **otomatik** beraberlik.
 * Yanlış beraberlik kararı vermemek için yalnızca kesin bilinen durumlar tanınır:
 *  - Piyon, kale, vezir yok ve tüm filler (her iki tarafta) aynı renk karelerde, at yok (Ş-Ş, Ş+F–Ş, Ş+F–Ş+F aynı renk vb.)
 *  - Piyon, kale, vezir, fil yok ve toplam tek at (Ş+A–Ş)
 * Kilitli piyon yapıları gibi diğer ölü konumlar tanınmaz (beraberlik ilan edilmez).
 */
object DeadPosition {
    fun isDead(p: FenPosition): Boolean {
        val pieces = p.board.filterNotNull().filter { it.kind != PieceKind.KING }
        if (pieces.any { it.kind == PieceKind.PAWN || it.kind == PieceKind.ROOK || it.kind == PieceKind.QUEEN }) return false
        val knights = pieces.count { it.kind == PieceKind.KNIGHT }
        val bishopSquares = p.board.indices.filter { p.board[it]?.kind == PieceKind.BISHOP }.map { Sq(it).isLight }
        if (knights == 0) return bishopSquares.distinct().size <= 1
        return knights == 1 && bishopSquares.isEmpty()
    }

    /**
     * Yetersiz materyal (FIDE 6.9, süre bittiğinde kullanılır): [side] rakibi hiçbir yasal hamle dizisiyle mat edemez mi?
     * Ölü pozisyondan farklıdır: yalnızca bir tarafın imkânını sorar. Kesin olmayan durumda `false` (mat edebilir) döner.
     */
    fun cannotCheckmate(p: FenPosition, side: Color): Boolean {
        if (isDead(p)) return true
        val own = p.board.filterNotNull().filter { it.color == side && it.kind != PieceKind.KING }
        if (own.isEmpty()) return true
        val opponentBare = p.board.filterNotNull().none { it.color == side.opposite && it.kind != PieceKind.KING }
        if (!opponentBare) return false
        if (own.size == 1 && own[0].kind == PieceKind.KNIGHT) return true
        if (own.all { it.kind == PieceKind.BISHOP }) {
            val colors = p.board.indices.filter { p.board[it] == ChessPiece(side, PieceKind.BISHOP) }.map { Sq(it).isLight }.distinct()
            return colors.size == 1
        }
        return false
    }
}

enum class GameResult { ONGOING, WHITE_WINS, BLACK_WINS, DRAW }

/** Oyunun bitme nedeni (otomatik) ya da talep edilebilir beraberlik. */
enum class Termination(val text: String, val automatic: Boolean) {
    CHECKMATE("Mat", true),
    STALEMATE("Pat", true),
    DEAD_POSITION("Ölü pozisyon (mat imkânsız)", true),
    FIVEFOLD_REPETITION("Beş kez tekrar", true),
    SEVENTY_FIVE_MOVES("75 hamle kuralı (150 yarım hamle)", true),
    THREEFOLD_CLAIMABLE("Üç kez tekrar – beraberlik talep edilebilir", false),
    FIFTY_MOVES_CLAIMABLE("50 hamle kuralı – beraberlik talep edilebilir", false),
}

data class GameStatus(val result: GameResult, val termination: Termination?, val claimable: List<Termination>, val inCheck: Boolean)
