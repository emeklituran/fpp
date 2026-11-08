package com.galaxyoffline.voicerecorder

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Gerçek mikrofonla (emülatörde sanal mikrofon) kısa bir kayıt yapar, listede görünmesini bekler. */
@RunWith(AndroidJUnit4::class)
class RecordFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun recordStopAndList() {
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("rec_start").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("rec_start").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("rec_stop").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("00:02").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("rec_stop").performClick()
        rule.waitUntil(15_000) { rule.onAllNodesWithTag("play_1").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("rec_start").fetchSemanticsNodes().isNotEmpty() }
    }
}
