package com.galaxyoffline.qrcode

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun generateQrFromText() {
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("tab_create").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("tab_create").performClick()
        rule.onNodeWithTag("create_text").performTextInput("Merhaba Galaxy")
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("qr_image").fetchSemanticsNodes().isNotEmpty() }
    }
}
