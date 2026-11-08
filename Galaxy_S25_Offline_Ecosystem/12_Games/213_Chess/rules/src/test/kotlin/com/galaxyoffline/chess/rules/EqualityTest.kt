package com.galaxyoffline.chess.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualityTest {
    private fun p(f: String) = (Fen.parse(f) as FenResult.Ok).position

    @Test fun nonCapturableEnPassantDiffersTextuallyButNotAsPosition() {
        val withEp = p("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
        val noEp = p("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1")
        assertFalse(Equality.textual(withEp, noEp))
        assertTrue(Equality.position(withEp, noEp))
        assertTrue(Equality.fullState(withEp, noEp))
    }

    @Test fun capturableEnPassantMakesPositionsDifferent() {
        val withEp = p("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3")
        val noEp = p("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq - 0 3")
        assertFalse(Equality.position(withEp, noEp))
    }

    @Test fun pinnedEnPassantIsNotAPossibility() {
        // Beyaz e5 piyonu açmazda (siyah kale a5 – beyaz şah h5 aynı sırada): exd6 yasal değil → kimlik "-" ile aynı.
        val withEp = p("4k3/8/8/r2pP2K/8/8/8/8 w - d6 0 2")
        val noEp = p("4k3/8/8/r2pP2K/8/8/8/8 w - - 0 2")
        assertEquals("-", Equality.keyOf(withEp).legalEnPassant)
        assertTrue(Equality.position(withEp, noEp))
    }

    @Test fun countersOnlyMatterForFullState() {
        val a = p("4k3/8/8/8/8/8/8/4K2R w K - 0 1")
        val b = p("4k3/8/8/8/8/8/8/4K2R w K - 37 60")
        assertTrue(Equality.position(a, b))
        assertFalse(Equality.fullState(a, b))
        assertFalse(Equality.textual(a, b))
    }

    @Test fun castlingRightsAndSideToMoveMatter() {
        assertNotEquals(Equality.keyOf(p("4k3/8/8/8/8/8/8/4K2R w K - 0 1")), Equality.keyOf(p("4k3/8/8/8/8/8/8/4K2R w - - 0 1")))
        assertNotEquals(Equality.keyOf(p("4k3/8/8/8/8/8/8/4K2R w - - 0 1")), Equality.keyOf(p("4k3/8/8/8/8/8/8/4K2R b - - 0 1")))
    }
}
