package com.android.mySwissDorm.ui.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Rule
import org.junit.Test

class RequestDetailScreenUiTest {

  @get:Rule val rule = createComposeRule()

  @OptIn(ExperimentalTestApi::class)
  private fun ComposeContentTestRule.waitForTag(tag: String, timeoutMs: Long = 5_000) {
    waitUntilAtLeastOneExists(hasTestTag(tag), timeoutMs)
  }

  @Test
  fun showsRequestInfoAndActions() {
    rule.setContent { MySwissDormAppTheme { RequestDetailScreen(id = "r1", onBack = {}) } }

    rule.waitForTag("req_field_identifiant")

    rule.onNodeWithTag("req_field_identifiant", useUnmergedTree = true).assertIsDisplayed()
    rule
        .onNodeWithTag("req_field_identifiant_value", useUnmergedTree = true)
        .assertTextContains("Request #r1") // <-- updated to English
    rule.onNodeWithTag("req_field_requester", useUnmergedTree = true).assertIsDisplayed()
    rule.onNodeWithTag("req_field_message", useUnmergedTree = true).assertIsDisplayed()
    rule.onNodeWithTag("btn_reject", useUnmergedTree = true).assertIsDisplayed()
    rule.onNodeWithTag("btn_accept", useUnmergedTree = true).assertIsDisplayed()
  }
}
