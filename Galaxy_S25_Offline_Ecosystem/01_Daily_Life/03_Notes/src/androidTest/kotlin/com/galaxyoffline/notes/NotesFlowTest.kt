package com.galaxyoffline.notes

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotesFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun waitText(text: String) = rule.waitUntil(8_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test fun createNoteSurvivesRecreate() {
        waitText("Yeni not")
        rule.onNodeWithTag("fab_new").performClick()
        rule.onNodeWithTag("note_title").performTextInput("Market")
        rule.onNodeWithTag("note_content").performTextInput("Süt, ekmek")
        rule.onNodeWithContentDescription("Geri").performClick()
        waitText("Market")
        rule.activityRule.scenario.recreate()
        waitText("Market")
        rule.onNodeWithText("Süt, ekmek").assertExists()
    }
}
