package com.galaxyoffline.chess.rules

/**
 * FEN yazım olarak doğru olsa da satranç açısından olanaksız konumları reddeder.
 * Mat ve pat konumları geçerlidir (analiz edilebilir). Mesajlar Türkçedir.
 */
object PositionValidator {
    fun validate(p: FenPosition): List<String> {
        val e = mutableListOf<String>()
        fun count(c: Color, k: PieceKind) = p.board.count { it == ChessPiece(c, k) }
        val tr = mapOf(Color.WHITE to "Beyaz", Color.BLACK to "Siyah")

        for (c in Color.entries) {
            val kings = count(c, PieceKind.KING)
            if (kings != 1) e += "${tr[c]} tarafta tam 1 şah olmalı; $kings şah var."
            val pawns = count(c, PieceKind.PAWN)
            if (pawns > 8) e += "${tr[c]} tarafta en fazla 8 piyon olabilir; $pawns var."
            val total = p.board.count { it?.color == c }
            if (total > 16) e += "${tr[c]} tarafta en fazla 16 taş olabilir; $total var."
            val extra = maxOf(0, count(c, PieceKind.QUEEN) - 1) + maxOf(0, count(c, PieceKind.ROOK) - 2) +
                maxOf(0, count(c, PieceKind.BISHOP) - 2) + maxOf(0, count(c, PieceKind.KNIGHT) - 2)
            if (pawns <= 8 && extra > 8 - pawns) e += "${tr[c]} tarafta terfiyle açıklanamayacak kadar çok taş var ($pawns piyon varken $extra fazla taş)."
        }
        for (sq in 0..63) {
            val piece = p.board[sq]
            if (piece?.kind == PieceKind.PAWN && (sq / 8 == 0 || sq / 8 == 7)) e += "Piyon ${Sq(sq).name} karesinde olamaz (1. ve 8. sıra)."
        }

        // Rok hakları: şah ve ilgili kale başlangıç karesinde olmalı.
        fun at(s: String) = p.pieceAt(Sq.parse(s)!!)
        val wk = ChessPiece(Color.WHITE, PieceKind.KING); val bk = ChessPiece(Color.BLACK, PieceKind.KING)
        val wr = ChessPiece(Color.WHITE, PieceKind.ROOK); val br = ChessPiece(Color.BLACK, PieceKind.ROOK)
        if (p.castling.whiteKing && (at("e1") != wk || at("h1") != wr)) e += "Beyazın kısa rok hakkı için şah e1'de, kale h1'de olmalı."
        if (p.castling.whiteQueen && (at("e1") != wk || at("a1") != wr)) e += "Beyazın uzun rok hakkı için şah e1'de, kale a1'de olmalı."
        if (p.castling.blackKing && (at("e8") != bk || at("h8") != br)) e += "Siyahın kısa rok hakkı için şah e8'de, kale h8'de olmalı."
        if (p.castling.blackQueen && (at("e8") != bk || at("a8") != br)) e += "Siyahın uzun rok hakkı için şah e8'de, kale a8'de olmalı."

        // Geçerken alma karesi: az önce iki kare ilerlemiş bir piyonla tutarlı olmalı.
        p.enPassant?.let { ep ->
            val moverColor = p.sideToMove.opposite
            val dir = if (moverColor == Color.WHITE) 1 else -1
            val pawnSq = Sq.of(ep.file, ep.rank + dir)
            val fromSq = Sq.of(ep.file, ep.rank - dir)
            if (p.pieceAt(pawnSq) != ChessPiece(moverColor, PieceKind.PAWN)) e += "Geçerken alma karesi ${ep.name} için ${pawnSq.name} karesinde ${tr[moverColor]!!.lowercase()} piyon olmalı."
            if (p.pieceAt(ep) != null || p.pieceAt(fromSq) != null) e += "Geçerken alma karesi ${ep.name} ve ${fromSq.name} boş olmalı."
            if (p.halfmoveClock != 0) e += "Geçerken alma karesi varken yarım hamle sayacı 0 olmalı (son hamle piyon hamlesiydi)."
        }

        // Şah durumu kontrolleri yalnızca her iki tarafta da tek şah varken anlamlıdır.
        if (Color.entries.all { count(it, PieceKind.KING) == 1 } && e.isEmpty()) {
            val b = Chesslib.board(p)
            if (Chesslib.checkers(b, p.sideToMove.opposite) > 0)
                e += "Hamle sırası ${tr[p.sideToMove]!!.lowercase()} tarafta iken ${tr[p.sideToMove.opposite]!!.lowercase()} şah tehdit altında olamaz."
            if (Chesslib.checkers(b, p.sideToMove) > 2) e += "Bir şaha aynı anda ikiden fazla taş şah çekemez."
        }
        return e
    }
}
