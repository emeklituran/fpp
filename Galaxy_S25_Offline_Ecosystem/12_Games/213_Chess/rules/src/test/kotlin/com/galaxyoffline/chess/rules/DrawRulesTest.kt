package com.galaxyoffline.chess.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawRulesTest {
    private fun game(f: String) = (Game.fromFen(f) as GameLoad.Ok).game
    private fun pos(f: String) = (Fen.parse(f) as FenResult.Ok).position
    private val knightDance = listOf("g1f3", "g8f6", "f3g1", "f6g8")

    @Test fun threefoldIsClaimableNotAutomatic() {
        val g = Game.standard()
        repeat(2) { knightDance.forEach { assertTrue(g.play(it)) } }
        assertEquals(3, g.repetitionCount())
        val s = g.status()
        assertEquals(GameResult.ONGOING, s.result)
        assertTrue(Termination.THREEFOLD_CLAIMABLE in s.claimable)
    }

    @Test fun fivefoldIsAutomatic() {
        val g = Game.standard()
        repeat(3) { knightDance.forEach { g.play(it) } }
        assertEquals(GameResult.ONGOING, g.status().result) // 4 kez
        knightDance.forEach { g.play(it) }
        assertEquals(5, g.repetitionCount())
        assertEquals(Termination.FIVEFOLD_REPETITION, g.status().termination)
    }

    @Test fun repetitionIgnoresNonCapturableEnPassantSquare() {
        val g = Game.standard()
        g.play("e2e4") // e3 geçerken alma karesi yazılır ama siyah alamaz
        repeat(2) { listOf("g8f6", "g1f3", "f6g8", "f3g1").forEach { g.play(it) } }
        assertEquals(3, g.repetitionCount())
    }

    @Test fun fiftyMoveRuleIsClaimableAt100HalfMoves() {
        val g = game("4k3/8/8/8/8/8/8/R3K3 w - - 99 80")
        assertFalse(Termination.FIFTY_MOVES_CLAIMABLE in g.status().claimable)
        g.play("a1a2")
        val s = g.status()
        assertEquals(GameResult.ONGOING, s.result)
        assertTrue(Termination.FIFTY_MOVES_CLAIMABLE in s.claimable)
    }

    @Test fun seventyFiveMoveRuleIsAutomaticAt150HalfMoves() {
        val g = game("4k3/8/8/8/8/8/8/R3K3 w - - 149 120")
        assertEquals(GameResult.ONGOING, g.status().result)
        g.play("a1a2")
        assertEquals(Termination.SEVENTY_FIVE_MOVES, g.status().termination)
    }

    @Test fun checkmateOnThe150thHalfMoveTakesPrecedence() {
        // Ra1-a8 mat; yarım hamle sayacı 150'ye ulaşır ama mat geçerlidir (FIDE 9.6.2).
        val g = game("6k1/5ppp/8/8/8/8/8/R5K1 w - - 149 120")
        assertTrue(g.play("a1a8"))
        assertEquals(Termination.CHECKMATE, g.status().termination)
        assertEquals(GameResult.WHITE_WINS, g.status().result)
    }

    @Test fun stalemate() = assertEquals(Termination.STALEMATE, game("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1").status().termination)

    @Test fun deadPositionOnlyForCertainCases() {
        mapOf(
            "8/8/4k3/8/8/4K3/8/8 w - - 0 1" to true,                 // Ş – Ş
            "8/8/4k3/8/8/4K3/5B2/8 w - - 0 1" to true,               // Ş+F – Ş
            "8/8/4k3/8/8/4K3/5N2/8 w - - 0 1" to true,               // Ş+A – Ş
            "8/5b2/4k3/8/8/4K3/6B1/8 w - - 0 1" to true,             // Ş+F – Ş+F aynı renk
            "8/8/4k3/8/8/4K3/4B1B1/8 w - - 0 1" to true,             // Ş+F+F aynı renk – Ş
            "8/8/4k3/8/8/4K3/4NN2/8 w - - 0 1" to false,             // Ş+A+A – Ş: mat mümkün
            "8/5n2/4k3/8/8/4K3/5N2/8 w - - 0 1" to false,            // Ş+A – Ş+A: mat mümkün (chesslib yanlış)
            "8/5n2/4k3/8/8/4K3/5B2/8 w - - 0 1" to false,            // Ş+F – Ş+A: mat mümkün (chesslib yanlış)
            "8/6b1/4k3/8/8/4K3/6B1/8 w - - 0 1" to false,            // zıt renk filler: mat mümkün
            "8/8/4k3/8/8/4K3/4P3/8 w - - 0 1" to false,              // piyon var
            "8/8/4k3/8/8/4K3/4R3/8 w - - 0 1" to false,
        ).forEach { (f, dead) -> assertEquals(f, dead, DeadPosition.isDead(pos(f))) }
        assertEquals(Termination.DEAD_POSITION, game("8/8/4k3/8/8/4K3/5N2/8 w - - 0 1").status().termination)
        assertEquals(GameResult.ONGOING, game("8/5n2/4k3/8/8/4K3/5N2/8 w - - 0 1").status().result)
    }

    @Test fun cannotCheckmateIsSeparateFromDeadPosition() {
        // Ş+A – Ş+V: ölü değil (siyah mat edebilir) ama beyaz da yardımlı matla mat edebilir → false
        val p = pos("8/5q2/4k3/8/8/4K3/5N2/8 w - - 0 1")
        assertFalse(DeadPosition.isDead(p))
        assertFalse(DeadPosition.cannotCheckmate(p, Color.WHITE))
        assertFalse(DeadPosition.cannotCheckmate(p, Color.BLACK))
        // Yalnız şah: mat edemez; rakip vezirle mat edebilir
        val q = pos("8/5q2/4k3/8/8/4K3/8/8 w - - 0 1")
        assertTrue(DeadPosition.cannotCheckmate(q, Color.WHITE))
        assertFalse(DeadPosition.cannotCheckmate(q, Color.BLACK))
        // Ş+A – Ş: at tarafı mat edemez
        assertTrue(DeadPosition.cannotCheckmate(pos("8/8/4k3/8/8/4K3/5N2/8 w - - 0 1"), Color.WHITE))
    }
}
