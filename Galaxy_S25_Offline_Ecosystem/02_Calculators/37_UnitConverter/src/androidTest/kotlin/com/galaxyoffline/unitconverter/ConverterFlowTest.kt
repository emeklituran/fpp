package com.galaxyoffline.unitconverter

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConverterFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun celsiusToFahrenheit() {
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("cat_temp").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("cat_temp").performClick()
        rule.onNodeWithTag("value").performTextReplacement("100")
        rule.onNodeWithTag("out_f").assertTextEquals("212 °F")
    }
}
