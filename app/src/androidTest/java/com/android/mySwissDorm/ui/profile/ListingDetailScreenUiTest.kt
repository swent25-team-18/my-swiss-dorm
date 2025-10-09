package com.android.mySwissDorm.ui.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Rule
import org.junit.Test

class ListingDetailScreenUiTest {

  @get:Rule val rule = createComposeRule()

  // Small helper: wait until a node with this testTag exists (up to 5s)
  @OptIn(ExperimentalTestApi::class)
  private fun ComposeContentTestRule.waitForTag(tag: String, timeoutMs: Long = 5_000) {
    waitUntilAtLeastOneExists(hasTestTag(tag), timeoutMs)
  }

  @Test
  fun rendersBlocksAndButtons() {
    rule.setContent { MySwissDormAppTheme { ListingDetailScreen(id = "l1", onBack = {}) } }

    // Wait for first meaningful node
    rule.waitForTag("field_identifiant")

    rule.onNodeWithTag("field_identifiant", useUnmergedTree = true).assertIsDisplayed()
    rule
        .onNodeWithTag("field_identifiant_value", useUnmergedTree = true)
        .assertTextContains("Annonce #l1")
    rule.onNodeWithTag("btn_edit", useUnmergedTree = true).assertIsDisplayed()
    rule.onNodeWithTag("btn_close", useUnmergedTree = true).assertIsDisplayed()
    rule.onNodeWithTag("nav_back", useUnmergedTree = true).assertIsDisplayed()
  }
}
