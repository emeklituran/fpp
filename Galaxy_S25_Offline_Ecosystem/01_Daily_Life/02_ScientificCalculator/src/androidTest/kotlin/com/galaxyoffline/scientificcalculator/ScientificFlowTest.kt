package com.galaxyoffline.scientificcalculator

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScientificFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun press(vararg labels: String) = labels.forEach { rule.onNodeWithTag("key_$it").performClick() }
    private fun ready() = rule.waitUntil(5_000) { rule.onAllNodesWithTag("key_AC").fetchSemanticsNodes().isNotEmpty() }

    @Test fun sineInDegrees() {
        ready()
        press("AC", "sin", "3", "0", ")", "=")
        rule.onNodeWithTag("display_expression").assertTextEquals("0,5")
    }

    @Test fun factorialAndPowers() {
        ready()
        press("AC", "5", "x!", "+", "2", "x²", "=")
        rule.onNodeWithTag("display_expression").assertTextEquals("124")
    }
}
