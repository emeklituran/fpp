package com.galaxyoffline.timer

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun waitTag(t: String) = rule.waitUntil(10_000) { rule.onAllNodesWithTag(t).fetchSemanticsNodes().isNotEmpty() }

    @Test fun quickStartPausePlusReset() {
        waitTag("tab_timer")
        if (rule.onAllNodesWithTag("reset").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithTag("reset").performClick()
        waitTag("quick_3")
        rule.onNodeWithTag("quick_3").performClick()
        waitTag("pause_resume")
        rule.waitUntil(5_000) { rule.onAllNodesWithText("02:5", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("pause_resume").performClick()
        rule.onNodeWithTag("run_status").assertTextEquals("Duraklatıldı")
        rule.onNodeWithTag("plus_minute").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("03:5", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("reset").performClick()
        waitTag("start")
    }

    @Test fun pomodoroStarts() {
        waitTag("tab_pomodoro")
        if (rule.onAllNodesWithTag("reset").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithTag("reset").performClick()
        rule.onNodeWithTag("tab_pomodoro").performClick()
        waitTag("pomo_start")
        rule.onNodeWithTag("pomo_start").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("24:5", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("reset").performClick()
        waitTag("pomo_start")
    }
}
