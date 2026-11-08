package com.galaxyoffline.chess.rules

/** Taraf. */
enum class Color { WHITE, BLACK; val opposite get() = if (this == WHITE) BLACK else WHITE }

enum class PieceKind(val fenLower: Char) { PAWN('p'), KNIGHT('n'), BISHOP('b'), ROOK('r'), QUEEN('q'), KING('k') }

data class ChessPiece(val color: Color, val kind: PieceKind) {
    val fen: Char get() = if (color == Color.WHITE) kind.fenLower.uppercaseChar() else kind.fenLower
    companion object {
        fun fromFen(c: Char): ChessPiece? {
            val kind = PieceKind.entries.firstOrNull { it.fenLower == c.lowercaseChar() } ?: return null
            return ChessPiece(if (c.isUpperCase()) Color.WHITE else Color.BLACK, kind)
        }
    }
}

/** Kare: 0 = a1, 7 = h1, 56 = a8, 63 = h8. */
@JvmInline
value class Sq(val index: Int) {
    val file get() = index % 8
    val rank get() = index / 8
    val name get() = "${'a' + file}${rank + 1}"
    /** a1 koyu karedir. */
    val isLight get() = (file + rank) % 2 == 1
    override fun toString() = name
    companion object {
        fun of(file: Int, rank: Int) = Sq(rank * 8 + file)
        fun parse(s: String): Sq? {
            if (s.length != 2) return null
            val f = s[0] - 'a'; val r = s[1] - '1'
            return if (f in 0..7 && r in 0..7) of(f, r) else null
        }
    }
}

/** Rok hakları (standart satranç; Chess960 desteklenmez). */
data class CastlingRights(val whiteKing: Boolean, val whiteQueen: Boolean, val blackKing: Boolean, val blackQueen: Boolean) {
    /** Kanonik FEN yazımı: KQkq sırası, hak yoksa "-". */
    fun toFen(): String = buildString {
        if (whiteKing) append('K'); if (whiteQueen) append('Q'); if (blackKing) append('k'); if (blackQueen) append('q')
    }.ifEmpty { "-" }
    companion object { val NONE = CastlingRights(false, false, false, false) }
}

/**
 * Ayrıştırılmış FEN. [board] 64 elemanlı, a1'den başlar.
 * [enPassant]: FEN'de yazan geçerken alma hedef karesi (alınabilir olup olmadığından bağımsız).
 */
data class FenPosition(
    val board: List<ChessPiece?>,
    val sideToMove: Color,
    val castling: CastlingRights,
    val enPassant: Sq?,
    val halfmoveClock: Int,
    val fullmoveNumber: Int,
) {
    init { require(board.size == 64) }
    fun pieceAt(sq: Sq) = board[sq.index]

    fun placementFen(): String = buildString {
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val p = board[rank * 8 + file]
                if (p == null) empty++ else { if (empty > 0) { append(empty); empty = 0 }; append(p.fen) }
            }
            if (empty > 0) append(empty)
            if (rank > 0) append('/')
        }
    }
}
