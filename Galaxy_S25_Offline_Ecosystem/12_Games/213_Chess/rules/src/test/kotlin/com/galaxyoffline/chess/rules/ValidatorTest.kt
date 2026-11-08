package com.galaxyoffline.chess.rules

import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorTest {
    private fun problems(f: String) = PositionValidator.validate((Fen.parse(f) as FenResult.Ok).position)

    @Test fun legalPositionsPass() {
        listOf(
            Fen.START, PerftTest.KIWI, PerftTest.P3, PerftTest.P4, PerftTest.P5, PerftTest.P6,
            "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3",   // mat konumu
            "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1",                                    // pat konumu
            "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3",    // yasal geçerken alma
            "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
            "k7/8/8/8/8/8/QQQQQQQ1/7K b - - 0 1",                               // 7 vezir (6 fazla taş ≤ 8 piyon terfisi); siyah iki vezirle şah altında
        ).forEach { assertTrue("$it -> ${problems(it)}", problems(it).isEmpty()) }
    }

    @Test fun illegalPositionsAreRejectedWithReason() {
        mapOf(
            "8/8/8/8/8/8/8/8 w - - 0 1" to "şah",
            "4k3/8/8/8/8/8/8/8 w - - 0 1" to "Beyaz tarafta tam 1 şah",
            "4k3/8/8/8/8/8/8/3KK3 w - - 0 1" to "Beyaz tarafta tam 1 şah",
            "4k3/8/8/8/8/8/8/P3K3 w - - 0 1" to "1. ve 8. sıra",
            "P3k3/8/8/8/8/8/8/4K3 w - - 0 1" to "1. ve 8. sıra",
            "4k3/pppppppp/p7/8/8/8/8/4K3 w - - 0 1" to "en fazla 8 piyon",
            "4k3/8/8/8/8/8/8/4K2R w Q - 0 1" to "uzun rok",
            "4k3/8/8/8/8/8/8/R3K3 w K - 0 1" to "kısa rok",
            "4k3/8/8/8/8/8/8/R4K1R w KQ - 0 1" to "rok",
            "r4k2/8/8/8/8/8/8/4K3 w q - 0 1" to "Siyahın uzun rok",
            "4k3/8/8/8/8/8/8/4K3 w - e6 0 1" to "Geçerken alma",
            "4k3/8/8/4p3/8/8/8/4K3 w - e6 3 1" to "yarım hamle sayacı 0",
            "4k3/8/4p3/4p3/8/8/8/4K3 w - e6 0 1" to "boş olmalı",
            "R3k3/8/8/8/8/8/8/4K3 w - - 0 1" to "tehdit altında olamaz",
            "4k3/8/8/8/8/8/8/4K2r b - - 0 1" to "tehdit altında olamaz",
            "QQQQQQQQ/QQQQQQQQ/8/8/8/8/8/K6k w - - 0 1" to "16 taş",
            "QQQQQQQQ/QQ6/8/8/8/8/PPPP4/K6k w - - 0 1" to "terfiyle açıklanamayacak",
        ).forEach { (f, expect) ->
            val p = problems(f)
            assertTrue("$f -> $p (beklenen: '$expect')", p.any { expect in it })
        }
    }

    @Test fun tripleCheckIsImpossible() {
        // Siyah şah e8'e kale (e1), fil (b5) ve at (d6) aynı anda şah çekiyor.
        val p = problems("4k3/8/3N4/1B6/8/8/8/4RK2 b - - 0 1")
        assertTrue(p.toString(), p.any { "ikiden fazla" in it })
    }
}
