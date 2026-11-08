package com.galaxyoffline.chess

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChessFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun has(tag: String) = rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun waitTag(tag: String, ms: Long = 15_000) = rule.waitUntil(ms) { has(tag) }
    private fun text(tag: String): String = rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        .config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
    private fun fen() = text("fen_text")

    private fun newGame(mode: String) {
        waitTag("menu"); rule.onNodeWithTag("menu").performClick()
        rule.onNodeWithTag("m_new").performClick()
        rule.onNodeWithTag("mode_$mode").performClick()
        rule.onNodeWithTag("start_game").performClick()
        rule.waitUntil(5_000) { !has("start_game") }
    }
    private fun move(from: String, to: String) {
        val before = fen()
        rule.onNodeWithTag("sq_$from").performClick(); rule.onNodeWithTag("sq_$to").performClick()
        rule.waitUntil(5_000) { fen() != before }
    }
    private fun loadFen(f: String) {
        rule.onNodeWithTag("menu").performClick(); rule.onNodeWithTag("m_fen").performClick()
        rule.onNodeWithTag("fen_input").performTextClearance(); rule.onNodeWithTag("fen_input").performTextInput(f)
        rule.onNodeWithTag("fen_load").performClick()
    }

    @Test fun foolsMateEndsGame() {
        newGame("TWO_PLAYER")
        move("f2", "f3"); move("e7", "e5"); move("g2", "g4"); move("d8", "h4")
        rule.onNodeWithTag("status").assertTextContains("Mat – Siyah kazandı")
    }

    @Test fun illegalTapDoesNotMove() {
        newGame("TWO_PLAYER")
        val before = fen()
        rule.onNodeWithTag("sq_e2").performClick(); rule.onNodeWithTag("sq_e5").performClick()
        rule.waitForIdle()
        assertEquals(before, fen())
    }

    @Test fun promotionToKnight() {
        newGame("TWO_PLAYER")
        loadFen("8/P6k/8/8/8/8/8/K7 w - - 0 1")
        rule.waitUntil(5_000) { fen().startsWith("8/P6k") }
        rule.onNodeWithTag("sq_a7").performClick(); rule.onNodeWithTag("sq_a8").performClick()
        waitTag("promo_n"); rule.onNodeWithTag("promo_n").performClick()
        rule.waitUntil(5_000) { fen().startsWith("N7/7k") }
    }

    @Test fun invalidFenShowsErrorAndKeepsGame() {
        newGame("TWO_PLAYER")
        val before = fen()
        loadFen("8/8/8/8 w - - 0 1")
        waitTag("fen_error")
        rule.onNodeWithText("Kapat").performClick()
        assertEquals(before, fen())
        loadFen("8/8/8/8/8/8/8/K6k w - - 0 1") // geçerli FEN, şahlar yan yana değil
        rule.waitUntil(5_000) { fen() == "8/8/8/8/8/8/8/K6k w - - 0 1" }
        rule.onNodeWithTag("status").assertTextContains("Berabere") // yetersiz materyal (otomatik)
    }

    @Test fun fenRoundTripAndPersistenceAfterRecreate() {
        newGame("TWO_PLAYER")
        move("e2", "e4"); move("c7", "c5")
        val f = fen()
        // c6 geçerken alma karesi yasal alma olmadığından FEN'e yazılmaz.
        assertEquals("rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2", f)
        rule.activityRule.scenario.recreate()
        waitTag("fen_text")
        assertEquals(f, fen())
    }

    @Test fun liveAnalysisThreeLinesAndUpdatesAfterMove() {
        newGame("ANALYSIS")
        rule.waitUntil(60_000) { has("pv_3") }
        rule.waitUntil(60_000) { has("analysis_meta") }
        move("e2", "e4")
        // Eski konumun satırları yeni konumda gösterilmemeli: önce temizlenir, sonra yeni satırlar gelir.
        rule.waitUntil(60_000) { has("pv_1") && text("analysis_meta").isNotEmpty() }
        val lines = rule.onAllNodesWithTag("pv_1", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue(lines.isNotEmpty())
        assertTrue("Siyah sırada olmalı", fen().contains(" b "))
    }

    @Test fun variationPreviewDoesNotChangeGame() {
        newGame("ANALYSIS")
        rule.waitUntil(60_000) { has("pv_1") }
        val before = fen()
        rule.onNodeWithTag("pv_1").performScrollTo().performClick()
        waitTag("preview_bar")
        rule.onNodeWithTag("preview_next").performClick()
        assertEquals(before, fen())
        rule.onNodeWithTag("preview_close").performClick()
        rule.waitUntil(5_000) { !has("preview_bar") }
        assertEquals(before, fen())
    }

    @Test fun computerReplies() {
        newGame("VS_COMPUTER")
        move("e2", "e4")
        rule.waitUntil(60_000) { fen().contains(" w ") }
        assertTrue(fen().split(" ")[5] == "2")
    }

    @Test fun editorRejectsIllegalAndAcceptsLegal() {
        waitTag("menu"); rule.onNodeWithTag("menu").performClick(); rule.onNodeWithTag("m_editor").performClick()
        waitTag("editor_clear"); rule.onNodeWithTag("editor_clear").performClick()
        rule.onNodeWithTag("editor_analyze").performScrollTo().performClick()
        waitTag("editor_problems") // boş tahta: şah yok → reddedilir, düzenleyicide kalınır
        rule.onNodeWithTag("brush_K").performScrollTo().performClick(); rule.onNodeWithTag("sq_e1").performScrollTo().performClick()
        rule.onNodeWithTag("brush_k").performScrollTo().performClick(); rule.onNodeWithTag("sq_e8").performScrollTo().performClick()
        rule.onNodeWithTag("brush_Q").performScrollTo().performClick(); rule.onNodeWithTag("sq_d1").performScrollTo().performClick()
        rule.waitUntil(5_000) { !has("editor_problems") }
        assertEquals("4k3/8/8/8/8/8/8/3QK3 w - - 0 1", text("editor_fen"))
        rule.onNodeWithTag("editor_analyze").performScrollTo().performClick()
        rule.waitUntil(5_000) { has("fen_text") && fen().startsWith("4k3/8/8/8/8/8/8/3QK3 w") }
    }

    @Test fun licensesScreenShowsGpl() {
        waitTag("menu"); rule.onNodeWithTag("menu").performClick(); rule.onNodeWithText("Ayarlar").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Açık kaynak lisansları").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("licenses").performScrollTo().performClick()
        waitTag("licenses_text")
        assertTrue(text("licenses_text").contains("GNU GENERAL PUBLIC LICENSE"))
        assertTrue(text("licenses_text").contains("Stockfish"))
    }
}
