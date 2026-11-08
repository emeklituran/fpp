package com.galaxyoffline.alarm

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun waitText(t: String) = rule.waitUntil(8_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Test fun createAlarmShowsNextAlarm() {
        waitText("Alarm ekle")
        rule.onNodeWithTag("fab_new").performClick()
        rule.onNodeWithTag("alarm_label").performTextInput("İş")
        rule.onNodeWithTag("alarm_save").performClick()
        waitText("Sonraki alarm")
        waitText("İş · Bir kez")
        rule.activityRule.scenario.recreate()
        waitText("İş · Bir kez")
    }
}
