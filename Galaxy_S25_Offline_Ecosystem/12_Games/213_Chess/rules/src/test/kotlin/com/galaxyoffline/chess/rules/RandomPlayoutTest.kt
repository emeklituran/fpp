package com.galaxyoffline.chess.rules

import com.github.bhlangonijr.chesslib.Board
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Rastgele oyunlarda tutarlılık: FEN gidiş-dönüş, doğrulayıcı, tekrar sayımı chesslib ile çapraz kontrol. */
class RandomPlayoutTest {
    @Test fun consistencyOverRandomGames() {
        val rnd = Random(20261108)
        var positions = 0
        repeat(300) { gameNo ->
            val g = Game.standard()
            val ref = Board()
            for (ply in 0 until 200) {
                val st = g.status()
                if (st.result != GameResult.ONGOING) break
                val moves = g.legalMoves()
                val m = moves[rnd.nextInt(moves.size)]
                assertTrue(g.play(m))
                ref.doMove(ref.legalMoves().first { Game.uci(it) == m })
                val fen = g.fen()
                // 1) FEN gidiş-dönüş metinsel olarak aynı
                assertEquals(fen, Fen.write((Fen.parse(fen) as FenResult.Ok).position))
                // 2) Oyunda oluşan her konum doğrulayıcıdan geçer
                val problems = PositionValidator.validate((Fen.parse(fen) as FenResult.Ok).position)
                assertTrue("oyun $gameNo hamle $ply $fen -> $problems", problems.isEmpty())
                // 3) Üç tekrar: bizim sayımımız ile chesslib aynı sonucu verir
                assertEquals("oyun $gameNo $fen", ref.isRepetition(3), g.repetitionCount() >= 3)
                positions++
            }
        }
        assertTrue(positions > 30_000)
    }
}
