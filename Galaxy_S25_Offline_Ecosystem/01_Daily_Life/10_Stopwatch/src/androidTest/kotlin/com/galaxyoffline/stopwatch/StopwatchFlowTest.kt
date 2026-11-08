package com.galaxyoffline.stopwatch

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StopwatchFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun runLapPauseAndSurviveRecreate() {
        rule.waitUntil(8_000) { rule.onAllNodesWithTag("start").fetchSemanticsNodes().isNotEmpty() }
        // Kronometre çalışırken ekran her karede yenilenir; test saati elle ilerletilir.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("start").performClick()
        rule.mainClock.advanceTimeBy(500)
        Thread.sleep(1_200) // gerçek geçen süre (monoton saat)
        rule.onNodeWithTag("lap").performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Tur 1").assertExists()
        rule.onNodeWithTag("start").performClick() // duraklat: kare döngüsü durur
        rule.mainClock.advanceTimeBy(500)
        rule.mainClock.autoAdvance = true
        rule.activityRule.scenario.recreate()
        rule.waitUntil(8_000) { rule.onAllNodesWithText("Tur 1").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(rule.onAllNodesWithTag("reset").fetchSemanticsNodes().isNotEmpty())
    }
}
