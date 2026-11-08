package com.galaxyoffline.passwordvault

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
class VaultFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun waitTag(t: String) = rule.waitUntil(15_000) { rule.onAllNodesWithTag(t).fetchSemanticsNodes().isNotEmpty() }
    private fun waitText(t: String) = rule.waitUntil(15_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Test fun setupAddLockUnlock() {
        waitTag("setup_pw")
        rule.onNodeWithTag("setup_pw").performTextInput("TestSifre!2026")
        rule.onNodeWithTag("setup_confirm").performTextInput("TestSifre!2026")
        rule.onNodeWithTag("setup_ok").performClick()
        rule.onNodeWithTag("setup_create").performClick()
        waitTag("fab_new")
        rule.onNodeWithTag("fab_new").performClick()
        rule.onNodeWithTag("item_title").performTextInput("Banka")
        rule.onNodeWithTag("item_secret").performTextInput("abc-XYZ-987")
        rule.onNodeWithTag("item_save").performClick()
        rule.waitUntil(15_000) { rule.onAllNodesWithTag("lock").fetchSemanticsNodes().isNotEmpty() && rule.onAllNodesWithText("Banka").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("lock").performClick()
        waitTag("master_password")
        rule.onNodeWithTag("master_password").performTextInput("TestSifre!2026")
        rule.onNodeWithTag("unlock").performClick()
        waitText("Banka")
    }
}
