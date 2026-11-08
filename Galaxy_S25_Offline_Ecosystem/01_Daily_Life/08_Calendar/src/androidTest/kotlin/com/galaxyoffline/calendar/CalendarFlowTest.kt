package com.galaxyoffline.calendar

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun waitTag(tag: String) = rule.waitUntil(8_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun waitText(t: String) = rule.waitUntil(8_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Test fun addEventShowsInDayAndAgenda() {
        waitTag("fab_new")
        rule.onNodeWithTag("fab_new").performClick()
        rule.onNodeWithTag("ev_title").performTextInput("Diş hekimi")
        rule.onNodeWithTag("ev_save").performClick()
        waitText("Diş hekimi")
        rule.onNodeWithContentDescription("Ajanda görünümü").performClick()
        waitText("Bugün")
        waitText("Diş hekimi")
    }
}
