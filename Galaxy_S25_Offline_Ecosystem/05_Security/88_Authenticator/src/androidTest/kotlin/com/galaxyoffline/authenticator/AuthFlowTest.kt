package com.galaxyoffline.authenticator

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun manualAccountShowsCodeAndPersists() {
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("fab_new").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("fab_new").performClick()
        rule.onNodeWithTag("m_issuer").performScrollTo().performTextInput("Test Hizmeti")
        rule.onNodeWithTag("m_secret").performScrollTo().performTextInput("JBSWY3DPEHPK3PXP")
        rule.onNodeWithTag("m_add").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Test Hizmeti").fetchSemanticsNodes().isNotEmpty() }
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Test Hizmeti").fetchSemanticsNodes().isNotEmpty() }
    }
}
