package com.galaxyoffline.chess.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FenTest {
    private fun ok(f: String) = (Fen.parse(f) as FenResult.Ok).position
    private fun errField(f: String) = (Fen.parse(f) as FenResult.Error).error.field

    @Test fun roundTripIsTextuallyIdentical() {
        listOf(Fen.START, PerftTest.KIWI, PerftTest.P3, PerftTest.P4, PerftTest.P5, PerftTest.P6,
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
            "8/8/8/8/8/8/8/K6k b - - 99 150").forEach { assertEquals(it, Fen.write(ok(it))) }
    }

    @Test fun extraWhitespaceIsTolerated() = assertEquals(Fen.START, Fen.write(ok("  rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR   w KQkq -  0 1 ")))

    @Test fun errorsReportTheField() {
        mapOf(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq" to FenField.STRUCTURE,
            "garbage" to FenField.STRUCTURE,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1 x" to FenField.STRUCTURE,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1" to FenField.PLACEMENT,
            "rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" to FenField.PLACEMENT,
            "rnbqkbnr/ppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" to FenField.PLACEMENT,
            "rnbqkbnr/pppppppp/44/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" to FenField.PLACEMENT,
            "rnbqkbnr/ppppxppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" to FenField.PLACEMENT,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1" to FenField.SIDE,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR W KQkq - 0 1" to FenField.SIDE,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w kqKQ - 0 1" to FenField.CASTLING,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkqK - 0 1" to FenField.CASTLING,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w AHah - 0 1" to FenField.CASTLING,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e4 0 1" to FenField.EN_PASSANT,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e3 0 1" to FenField.EN_PASSANT,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq z9 0 1" to FenField.EN_PASSANT,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - -1 1" to FenField.HALFMOVE,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - x 1" to FenField.HALFMOVE,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 0" to FenField.FULLMOVE,
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 +1" to FenField.FULLMOVE,
        ).forEach { (f, field) -> assertEquals(f, field, errField(f)) }
    }

    @Test fun errorMessagesAreTurkishAndSpecific() {
        val e = (Fen.parse("rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") as FenResult.Error).error
        assertTrue(e.toString(), e.toString().startsWith("Taş yerleşimi:") && "6. sıra" in e.message)
    }
}
