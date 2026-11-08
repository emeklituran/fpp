package com.galaxyoffline.todo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun waitText(t: String) = rule.waitUntil(8_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Test fun addAndCompleteTask() {
        waitText("Görevlerim")
        rule.onNodeWithTag("fab_new").performClick()
        rule.onNodeWithTag("task_title").performTextInput("Faturayı öde")
        rule.onNodeWithTag("task_save").performClick()
        waitText("Faturayı öde")
        rule.activityRule.scenario.recreate()
        waitText("Faturayı öde")
        rule.onNodeWithText("Tamamlanan", substring = true).performClick()
        rule.onNodeWithText("Tümü", substring = true).performClick()
        rule.onNodeWithText("Faturayı öde").assertExists()
    }
}
