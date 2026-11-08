package com.galaxyoffline.incomeexpense

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoneyFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun addExpenseUpdatesSummary() {
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("fab_new").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("fab_new").performClick()
        rule.onNodeWithTag("tx_amount").performTextInput("250,50")
        rule.onNodeWithTag("tx_save").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("−250,50 ₺", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("−250,50 ₺", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }
}
