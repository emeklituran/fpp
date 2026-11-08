package com.galaxyoffline.chess.rules

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

/** chesslib ile tek temas noktası: modülün geri kalanı chesslib türlerini görmez. */
internal object Chesslib {
    fun board(p: FenPosition): Board = Board().apply { loadFromFen(Fen.write(p)) }

    fun square(sq: Sq): Square = Square.squareAt(sq.index)
    fun sq(s: Square): Sq = Sq(s.ordinal)
    fun side(c: Color): Side = if (c == Color.WHITE) Side.WHITE else Side.BLACK

    /** [color] tarafının şahına rakip taşlarca saldıran taş sayısı. */
    fun checkers(b: Board, color: Color): Int {
        val k = b.getKingSquare(side(color))
        if (k == Square.NONE) return 0
        return java.lang.Long.bitCount(b.squareAttackedBy(k, side(color.opposite)))
    }

    /** Yasal bir geçerken alma hamlesi var mı (açmaz ve şah durumu dahil, FIDE tanımı). */
    fun legalEnPassant(b: Board, ep: Sq?): Sq? {
        if (ep == null) return null
        val target = square(ep)
        val pawn = if (b.sideToMove == Side.WHITE) Piece.WHITE_PAWN else Piece.BLACK_PAWN
        return if (b.legalMoves().any { it.to == target && b.getPiece(it.from) == pawn && it.from.file != it.to.file }) ep else null
    }
}
