package com.galaxyoffline.chess.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTest {
    @Test fun sanLineDoesNotChangeTheGame() {
        val g = Game.standard(); g.play("e2e4")
        val before = g.fen()
        assertEquals(listOf("e5", "Nf3", "Nc6", "Bb5"), g.sanLine(listOf("e7e5", "g1f3", "b8c6", "f1b5")))
        assertEquals(before, g.fen())
    }
    @Test fun sanLineStopsAtIllegalMove() {
        assertEquals(listOf("e4"), Game.standard().sanLine(listOf("e2e4", "e2e4", "d7d5")))
    }
    @Test fun replayRestoresFullState() {
        val g = Game.standard()
        listOf("e2e4", "c7c5", "g1f3", "d7d6", "d2d4", "c5d4", "f3d4", "g8f6", "b1c3", "a7a6").forEach { assertTrue(g.play(it)) }
        val r = Game.replay(Fen.START, g.history())!!
        assertTrue(Equality.fullState(g.position(), r.position()))
        assertEquals(g.repetitionCount(), r.repetitionCount())
        assertNull(Game.replay(Fen.START, listOf("e2e5")))
        assertNull(Game.replay("geçersiz", emptyList()))
    }
    @Test fun sanPlayAndSpecialMoves() {
        val g = Game.standard()
        listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "O-O").forEach { assertTrue(it, g.playSan(it)) }
        assertEquals("e1g1", g.history().last())
        val p = (Game.fromFen("8/P6k/8/8/8/8/8/K7 w - - 0 1") as GameLoad.Ok).game
        assertEquals("a8=Q", p.san("a7a8q")); assertEquals("a8=N", p.san("a7a8n"))
        assertTrue(p.playSan("a8=Q+")) // fazladan +/# işareti yok sayılır
        assertTrue(p.fen().startsWith("Q7/7k/"))
        val ep = (Game.fromFen("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3") as GameLoad.Ok).game
        assertEquals("exf6", ep.san("e5f6"))
        assertFalse(ep.play("e5d6")) // d6 geçerken alma hedefi değil
    }
    @Test fun undoRestoresPreviousState() {
        val g = Game.standard(); val s0 = g.fen()
        g.play("e2e4"); g.play("e7e5"); assertTrue(g.undo()); assertTrue(g.undo())
        assertEquals(s0, g.fen()); assertFalse(g.undo())
    }
}
