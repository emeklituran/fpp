package com.galaxyoffline.timer

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
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
        rule.onNodeWithTag("tab_timer").performSemanticsAction(SemanticsActions.OnClick)
        if (rule.onAllNodesWithTag("reset").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithTag("reset").performSemanticsAction(SemanticsActions.OnClick)
        waitTag("quick_3")
        rule.onNodeWithTag("quick_3").performSemanticsAction(SemanticsActions.OnClick)
        waitTag("pause_resume")
        rule.waitUntil(5_000) { rule.onAllNodesWithText("02:5", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("pause_resume").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag("run_status").assertTextEquals("Duraklatıldı")
        rule.onNodeWithTag("plus_minute").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(5_000) { rule.onAllNodesWithText("03:5", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("reset").performSemanticsAction(SemanticsActions.OnClick)
        waitTag("start")
    }

    @Test fun pomodoroStarts() {
        waitTag("tab_pomodoro")
        if (rule.onAllNodesWithTag("reset").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithTag("reset").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag("tab_pomodoro").performSemanticsAction(SemanticsActions.OnClick)
        waitTag("pomo_start")
        rule.onNodeWithTag("pomo_start").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(5_000) { rule.onAllNodesWithText("24:5", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("reset").performSemanticsAction(SemanticsActions.OnClick)
        waitTag("pomo_start")
    }
}
