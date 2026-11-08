package com.galaxyoffline.habittracker

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun addAndCheckToday() {
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("fab_new").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("fab_new").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag("habit_name").performTextInput("Kitap oku")
        rule.onNodeWithTag("habit_save").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Kitap oku").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("check_1").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(10_000) { rule.onAllNodesWithText("🔥 1 gün seri", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }
}
