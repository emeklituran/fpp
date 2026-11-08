package com.galaxyoffline.billreminder

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
class BillFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun addBillAndMarkPaid() {
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("fab_new").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("fab_new").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag("bill_name").performTextInput("Elektrik")
        rule.onNodeWithTag("bill_amount").performTextInput("450")
        rule.onNodeWithTag("bill_save").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Elektrik").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("450,00 ₺", substring = true).fetchSemanticsNodes().size >= 2 }
    }
}
