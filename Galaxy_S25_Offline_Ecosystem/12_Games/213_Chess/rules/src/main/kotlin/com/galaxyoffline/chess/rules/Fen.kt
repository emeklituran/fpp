package com.galaxyoffline.chess.rules

/** FEN alanları; hata mesajında hangi alanın hatalı olduğunu göstermek için. */
enum class FenField(val label: String) {
    STRUCTURE("Genel yapı"), PLACEMENT("Taş yerleşimi"), SIDE("Hamle sırası"), CASTLING("Rok hakları"),
    EN_PASSANT("Geçerken alma karesi"), HALFMOVE("Yarım hamle sayacı"), FULLMOVE("Hamle numarası"),
}

data class FenError(val field: FenField, val message: String) {
    override fun toString() = "${field.label}: $message"
}

sealed interface FenResult {
    data class Ok(val position: FenPosition) : FenResult
    data class Error(val error: FenError) : FenResult
}

/**
 * Katı FEN çözümleyici. Yalnızca standart 6 alanlı FEN kabul edilir; her hata alan bilgisiyle döner.
 * Bu katman yalnızca **yazım** kurallarını denetler; konumun satranç açısından geçerliliği [PositionValidator] işidir.
 */
object Fen {
    const val START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun parse(text: String): FenResult {
        val parts = text.trim().split(Regex("\\s+"))
        if (parts.size != 6) return err(FenField.STRUCTURE, "6 alan olmalı (taşlar, sıra, rok, geçerken alma, yarım hamle, hamle no); ${parts.size} alan var")
        val (placement, side, castling, ep, half, full) = parts

        val ranks = placement.split('/')
        if (ranks.size != 8) return err(FenField.PLACEMENT, "8 sıra olmalı; ${ranks.size} sıra var")
        val board = arrayOfNulls<ChessPiece>(64)
        for ((i, r) in ranks.withIndex()) {
            val rank = 7 - i
            var file = 0
            var prevDigit = false
            for (c in r) {
                when {
                    c in '1'..'8' -> {
                        if (prevDigit) return err(FenField.PLACEMENT, "${rank + 1}. sırada ardışık rakam var (\"$r\")")
                        file += c - '0'; prevDigit = true
                    }
                    else -> {
                        val p = ChessPiece.fromFen(c) ?: return err(FenField.PLACEMENT, "geçersiz karakter '$c' (${rank + 1}. sıra)")
                        if (file > 7) return err(FenField.PLACEMENT, "${rank + 1}. sırada 8'den fazla kare var")
                        board[rank * 8 + file] = p; file++; prevDigit = false
                    }
                }
                if (file > 8) return err(FenField.PLACEMENT, "${rank + 1}. sırada 8'den fazla kare var (\"$r\")")
            }
            if (file != 8) return err(FenField.PLACEMENT, "${rank + 1}. sırada $file kare var, 8 olmalı (\"$r\")")
        }

        val stm = when (side) { "w" -> Color.WHITE; "b" -> Color.BLACK; else -> return err(FenField.SIDE, "\"w\" veya \"b\" olmalı; \"$side\" yazılmış") }

        val rights = if (castling == "-") CastlingRights.NONE else {
            if (!castling.matches(Regex("K?Q?k?q?")) || castling.isEmpty())
                return err(FenField.CASTLING, "\"-\" ya da KQkq sırasıyla harfler olmalı; \"$castling\" yazılmış")
            CastlingRights('K' in castling, 'Q' in castling, 'k' in castling, 'q' in castling)
        }

        val epSq = if (ep == "-") null else {
            val s = Sq.parse(ep) ?: return err(FenField.EN_PASSANT, "\"-\" ya da bir kare (örn. e3) olmalı; \"$ep\" yazılmış")
            val expectedRank = if (stm == Color.WHITE) 5 else 2
            if (s.rank != expectedRank) return err(FenField.EN_PASSANT, "sıra ${if (stm == Color.WHITE) "beyazda" else "siyahta"} iken ${expectedRank + 1}. sırada olmalı; $ep yazılmış")
            s
        }

        val hm = half.toIntOrNull()?.takeIf { half.all(Char::isDigit) } ?: return err(FenField.HALFMOVE, "negatif olmayan bir tam sayı olmalı; \"$half\" yazılmış")
        val fm = full.toIntOrNull()?.takeIf { full.all(Char::isDigit) && it >= 1 } ?: return err(FenField.FULLMOVE, "1 veya daha büyük bir tam sayı olmalı; \"$full\" yazılmış")

        return FenResult.Ok(FenPosition(board.toList(), stm, rights, epSq, hm, fm))
    }

    /**
     * Kanonik FEN yazımı (metinsel eşitlik için): rok KQkq sırasında, sayaçlar dahil.
     * [epMode]: geçerken alma alanı her zaman mı yazılsın, yoksa yalnızca yasal alma mümkünse mi.
     */
    fun write(p: FenPosition, legalEnPassant: Sq? = p.enPassant): String =
        "${p.placementFen()} ${if (p.sideToMove == Color.WHITE) "w" else "b"} ${p.castling.toFen()} ${legalEnPassant?.name ?: "-"} ${p.halfmoveClock} ${p.fullmoveNumber}"

    private fun err(f: FenField, m: String) = FenResult.Error(FenError(f, m))
    private operator fun <T> List<T>.component6() = this[5]
}
