package com.galaxyoffline.shoppinglist

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun waitTag(tag: String) = rule.waitUntil(8_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun waitText(t: String) = rule.waitUntil(8_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Test fun createListAndAddItem() {
        waitTag("fab_new")
        rule.onNodeWithTag("fab_new").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag("list_name").performTextReplacement("Hafta sonu")
        rule.onNodeWithTag("list_ok").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(8_000) { rule.onAllNodesWithText("Liste boş").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("item_input").performTextInput("Domates")
        rule.onNodeWithTag("item_add").performSemanticsAction(SemanticsActions.OnClick)
        waitText("1 adet")
        rule.activityRule.scenario.recreate()
        waitText("Domates")
    }
}
