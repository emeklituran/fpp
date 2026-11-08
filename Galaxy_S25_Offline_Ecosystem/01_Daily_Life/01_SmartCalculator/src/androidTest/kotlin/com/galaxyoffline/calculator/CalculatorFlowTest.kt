package com.galaxyoffline.calculator

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalculatorFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun press(vararg labels: String) = labels.forEach { rule.onNodeWithTag("key_$it").performClick() }

    @Test fun smartPercentAndHistory() {
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("key_AC").fetchSemanticsNodes().isNotEmpty() }
        press("AC", "2", "0", "0", "+", "1", "0", "%")
        rule.onNodeWithTag("display_preview").assertTextEquals("= 220")
        press("=")
        rule.onNodeWithTag("display_expression").assertTextEquals("220")
        // Geçmişte kayıt görünür ve uygulama yeniden oluşturulduğunda korunur.
        rule.activityRule.scenario.recreate()
        rule.onNodeWithContentDescription("Geçmiş").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("= 220").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("= 220").assertExists()
    }

    @Test fun divisionByZeroShowsError() {
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("key_AC").fetchSemanticsNodes().isNotEmpty() }
        press("AC", "5", "÷", "0", "=")
        rule.onNodeWithTag("display_preview").assertTextEquals("Sıfıra bölünemez")
    }
}
