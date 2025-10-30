package com.android.mySwissDorm.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // ---------- helpers ----------

  private fun ComposeTestRule.waitUntilTagExists(tag: String, timeoutMs: Long = 30_000) {
    waitUntil(timeoutMs) {
      onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun ComposeTestRule.waitUntilTagGone(tag: String, timeoutMs: Long = 30_000) {
    waitUntil(timeoutMs) {
      onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
    }
  }

  private fun ComposeTestRule.scrollUntilDisplayed(
      scrollTag: String,
      targetTag: String,
      maxSwipes: Int = 24
  ) {
    val scrollNode = onNodeWithTag(scrollTag, useUnmergedTree = true)
    repeat(maxSwipes) {
      try {
        onNodeWithTag(targetTag, useUnmergedTree = true).assertIsDisplayed()
        return
      } catch (_: AssertionError) {
        scrollNode.performTouchInput { swipeUp() }
        waitForIdle()
      }
    }
    onNodeWithTag(targetTag, useUnmergedTree = true).assertIsDisplayed()
  }

  // ---------- tests ----------

  @Test
  fun settingsScreen_showsProfileRowAndButton() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Settings").assertExists()
    composeTestRule.onNodeWithText("View profile").assertExists()
    composeTestRule
        .onNodeWithTag(SettingsTestTags.ProfileButton, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick() // no-op in tests; just ensures the node is tappable
  }

  @Test
  fun bottomBar_notRenderedInTests_whenNoNavigationActions() {
    // SettingsScreen() is called without navigationActions in tests
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithTag(SettingsTestTags.BottomBar, useUnmergedTree = true)
        .assertCountEquals(0)
  }

  @Test
  fun notificationSwitches_toggleStateCorrectly() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()

    val messagesTag = SettingsTestTags.switch("Show notifications for messages")
    val listingsTag = SettingsTestTags.switch("Show notifications for new listings")

    composeTestRule
        .onNodeWithTag(messagesTag, useUnmergedTree = true)
        .assert(hasStateDescription("On"))
        .performClick()
    composeTestRule
        .onNodeWithTag(messagesTag, useUnmergedTree = true)
        .assert(hasStateDescription("Off"))

    composeTestRule
        .onNodeWithTag(listingsTag, useUnmergedTree = true)
        .assert(hasStateDescription("Off"))
        .performClick()
    composeTestRule
        .onNodeWithTag(listingsTag, useUnmergedTree = true)
        .assert(hasStateDescription("On"))
  }

  @Test
  fun blockedContacts_expandsAndCollapsesOnClick() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()

    val scrollTag = SettingsTestTags.SettingsScroll
    val listTag = SettingsTestTags.BlockedContactsList
    val toggleTag = SettingsTestTags.BlockedContactsToggle

    composeTestRule.onNodeWithTag(listTag, useUnmergedTree = true).assertDoesNotExist()

    composeTestRule.scrollUntilDisplayed(scrollTag, toggleTag)
    composeTestRule.onNodeWithTag(toggleTag, useUnmergedTree = true).performClick()
    composeTestRule.waitUntilTagExists(listTag)
    composeTestRule.scrollUntilDisplayed(scrollTag, listTag)
    composeTestRule.onNodeWithTag(listTag, useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Clarisse K.").assertExists()

    composeTestRule.scrollUntilDisplayed(scrollTag, toggleTag)
    composeTestRule.onNodeWithTag(toggleTag, useUnmergedTree = true).performClick()
    composeTestRule.waitUntilTagGone(listTag)
    composeTestRule.onNodeWithTag(listTag, useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun emailField_isDisabledAndReadOnly() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(SettingsTestTags.EmailField, useUnmergedTree = true)
        .assertIsNotEnabled()
  }

  @Test
  fun deleteAccountButton_opensAndClosesConfirmDialog() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()

    composeTestRule.scrollUntilDisplayed(
        SettingsTestTags.SettingsScroll, SettingsTestTags.DeleteAccountButton)

    composeTestRule
        .onNodeWithTag(SettingsTestTags.DeleteAccountButton, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.onNodeWithText("Delete account?").assertExists()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.onNodeWithText("Delete account?").assertDoesNotExist()
  }

  // ---------- NEW: Accessibility section ----------

  @Test
  fun accessibilitySwitches_toggleStateCorrectly() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()

    val scrollTag = SettingsTestTags.SettingsScroll
    val nightShiftTag = SettingsTestTags.switch("Night Shift")
    val anonymousTag = SettingsTestTags.switch("Anonymous")

    // Night Shift starts On (reuses notificationsMessages = true)
    composeTestRule.scrollUntilDisplayed(scrollTag, nightShiftTag)
    composeTestRule
        .onNodeWithTag(nightShiftTag, useUnmergedTree = true)
        .assert(hasStateDescription("On"))
        .performClick()
    composeTestRule
        .onNodeWithTag(nightShiftTag, useUnmergedTree = true)
        .assert(hasStateDescription("Off"))

    // Anonymous starts Off (reuses notificationsListings = false)
    composeTestRule.scrollUntilDisplayed(scrollTag, anonymousTag)
    composeTestRule
        .onNodeWithTag(anonymousTag, useUnmergedTree = true)
        .assert(hasStateDescription("Off"))
        .performClick()
    composeTestRule
        .onNodeWithTag(anonymousTag, useUnmergedTree = true)
        .assert(hasStateDescription("On"))
  }
}
