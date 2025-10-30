package com.android.mySwissDorm.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Rule
import org.junit.Test

/** UI tests for the Settings screen (screen owns its ViewModel). */
class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  /** Wait for any node with [tag] to exist in the semantics tree. */
  private fun ComposeTestRule.waitUntilTagExists(tag: String, timeoutMs: Long = 30_000) {
    waitUntil(timeoutMs) {
      onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
  }

  /** Wait for any node with [tag] to be gone from the semantics tree. */
  private fun ComposeTestRule.waitUntilTagGone(tag: String, timeoutMs: Long = 30_000) {
    waitUntil(timeoutMs) {
      onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
    }
  }

  /**
   * Scrolls the scrollable with [scrollTag] until a node with [targetTag] is actually *displayed*
   * (visible on screen), or we hit [maxSwipes].
   */
  private fun ComposeTestRule.scrollUntilDisplayed(
      scrollTag: String,
      targetTag: String,
      maxSwipes: Int = 18
  ) {
    val scrollNode = onNodeWithTag(scrollTag, useUnmergedTree = true)
    var swipes = 0
    while (swipes < maxSwipes) {
      try {
        onNodeWithTag(targetTag, useUnmergedTree = true).assertIsDisplayed()
        return
      } catch (_: AssertionError) {
        scrollNode.performTouchInput { swipeUp() }
        waitForIdle()
        swipes++
      }
    }
    // Final assert to throw a clear error if not visible after scrolling.
    onNodeWithTag(targetTag, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun settingsScreen_showsProfileRowAndButton() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Sophie").assertExists()
    composeTestRule.onNodeWithText("View profile").assertExists()
    composeTestRule
        .onNodeWithTag(SettingsTestTags.ProfileButton, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun deleteAccountButton_triggersOnItemClickCallback() {
    val clickedItem = AtomicReference<String?>(null)
    composeTestRule.setContent {
      MySwissDormAppTheme { SettingsScreen(onItemClick = { item -> clickedItem.set(item) }) }
    }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(SettingsTestTags.DeleteAccountButton, useUnmergedTree = true)
        .performClick()
    assert(clickedItem.get() == "Delete my account") {
      "onItemClick was not called with 'Delete my account'."
    }
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

    // Starts collapsed
    composeTestRule.onNodeWithTag(listTag, useUnmergedTree = true).assertDoesNotExist()

    // Make sure the toggle is actually on screen (not just present in semantics)
    composeTestRule.scrollUntilDisplayed(scrollTag, toggleTag)
    composeTestRule.onNodeWithTag(toggleTag, useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Wait for the list to exist, then ensure it's displayed
    composeTestRule.waitUntilTagExists(listTag, timeoutMs = 30_000)
    composeTestRule.scrollUntilDisplayed(scrollTag, listTag)
    composeTestRule.onNodeWithTag(listTag, useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Clarisse K.").assertExists()

    // Collapse again
    composeTestRule.scrollUntilDisplayed(scrollTag, toggleTag)
    composeTestRule.onNodeWithTag(toggleTag, useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilTagGone(listTag, timeoutMs = 30_000)
    composeTestRule.onNodeWithTag(listTag, useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun emailField_updatesValueOnInput() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()

    val emailFieldTag = SettingsTestTags.EmailField
    composeTestRule
        .onNodeWithTag(emailFieldTag, useUnmergedTree = true)
        .assert(hasText("john.doe@email.com"))
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(emailFieldTag, useUnmergedTree = true)
        .performTextInput("new.email@example.com")
    composeTestRule
        .onNodeWithTag(emailFieldTag, useUnmergedTree = true)
        .assert(hasText("new.email@example.com"))
  }

  @Test
  fun emailField_clearsFocusOnDone() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()

    val emailFieldTag = SettingsTestTags.EmailField
    val emailNode = composeTestRule.onNodeWithTag(emailFieldTag, useUnmergedTree = true)

    // Focus then send IME action (Done) and verify focus is cleared.
    emailNode.performClick()
    emailNode.assertIsFocused()
    emailNode.performImeAction()
    emailNode.assertIsNotFocused()
  }
}
