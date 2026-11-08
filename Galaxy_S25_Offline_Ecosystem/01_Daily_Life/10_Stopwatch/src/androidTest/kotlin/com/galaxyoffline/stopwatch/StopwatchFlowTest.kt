package com.galaxyoffline.stopwatch

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
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
        rule.onNodeWithTag("start").performClick()
        Thread.sleep(1_200)
        rule.onNodeWithTag("lap").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Tur 1").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("start").performClick() // duraklat
        rule.activityRule.scenario.recreate()
        rule.waitUntil(8_000) { rule.onAllNodesWithText("Tur 1").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(rule.onAllNodesWithTag("reset").fetchSemanticsNodes().isNotEmpty())
    }
}
