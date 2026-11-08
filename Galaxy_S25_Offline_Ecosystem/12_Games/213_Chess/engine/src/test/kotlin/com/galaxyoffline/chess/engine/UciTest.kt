package com.galaxyoffline.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UciTest {
    @Test fun parsesStockfishInfoLines() {
        val l = Uci.parseInfo("info depth 22 seldepth 31 multipv 2 score cp -35 nodes 4123456 nps 2100000 hashfull 120 tbhits 0 time 1960 pv e7e5 g1f3 b8c6")!!
        assertEquals(2, l.multipv); assertEquals(22, l.depth); assertEquals(31, l.selDepth)
        assertEquals(Score.Cp(-35), l.score); assertEquals(4123456L, l.nodes); assertEquals(1960L, l.timeMs)
        assertEquals(listOf("e7e5", "g1f3", "b8c6"), l.pv)
    }
    @Test fun parsesMateScores() {
        assertEquals(Score.Mate(3), Uci.parseInfo("info depth 9 multipv 1 score mate 3 nodes 10 pv h5f7")!!.score)
        assertEquals(Score.Mate(-2), Uci.parseInfo("info depth 9 multipv 1 score mate -2 nodes 10 pv a1a2")!!.score)
    }
    @Test fun ignoresBoundsStringsAndPvLess() {
        assertNull(Uci.parseInfo("info depth 20 score cp 50 lowerbound nodes 5 pv e2e4"))
        assertNull(Uci.parseInfo("info depth 20 score cp 50 upperbound nodes 5 pv e2e4"))
        assertNull(Uci.parseInfo("info string NNUE evaluation using nn-1a298aa575a0.nnue enabled"))
        assertNull(Uci.parseInfo("info depth 1 currmove e2e4 currmovenumber 1"))
        assertNull(Uci.parseInfo("info depth 5 score cp 10 nodes 100"))
    }
    @Test fun bestMove() {
        assertEquals("e2e4", Uci.parseBestMove("bestmove e2e4 ponder e7e5"))
        assertEquals("e7e8q", Uci.parseBestMove("bestmove e7e8q"))
        assertNull(Uci.parseBestMove("bestmove (none)"))
    }
    @Test fun scorePerspective() {
        assertEquals(Score.Cp(-40), Score.Cp(40).forWhite(whiteToMove = false))
        assertEquals(Score.Mate(-3), Score.Mate(3).forWhite(whiteToMove = false))
        assertEquals(Score.Cp(40), Score.Cp(40).forWhite(whiteToMove = true))
        assert(Score.Mate(5).sortValue() > Score.Cp(5000).sortValue())
        assert(Score.Mate(2).sortValue() > Score.Mate(5).sortValue())
        assert(Score.Mate(-2).sortValue() < Score.Mate(-5).sortValue())
    }
}
