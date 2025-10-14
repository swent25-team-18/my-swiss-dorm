package com.android.mySwissDorm.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import java.util.concurrent.atomic.AtomicBoolean
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
    composeTestRule.onNodeWithTag("ProfileButton", useUnmergedTree = true).assertExists()
  }

  @Test
  fun backButton_triggersOnGoBackCallback() {
    val onGoBackCalled = AtomicBoolean(false)
    composeTestRule.setContent {
      MySwissDormAppTheme { SettingsScreen(onGoBack = { onGoBackCalled.set(true) }) }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("BackButton", useUnmergedTree = true).performClick()
    assert(onGoBackCalled.get()) { "onGoBack callback was not called." }
  }

  @Test
  fun deleteAccountButton_triggersOnItemClickCallback() {
    val clickedItem = AtomicReference<String?>(null)
    composeTestRule.setContent {
      MySwissDormAppTheme { SettingsScreen(onItemClick = { item -> clickedItem.set(item) }) }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("DeleteAccountButton", useUnmergedTree = true).performClick()
    assert(clickedItem.get() == "Delete my account") {
      "onItemClick was not called with 'Delete my account'."
    }
  }

  @Test
  fun notificationSwitches_toggleStateCorrectly() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()

    val messagesTag = "SettingSwitch_Show notifications for messages"
    val listingsTag = "SettingSwitch_Show notifications for new listings"

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

    val scrollTag = "SettingsScroll"
    val listTag = "BlockedContactsList"
    val toggleTag = "BlockedContactsToggle"

    // Starts collapsed
    composeTestRule.onNodeWithTag(listTag, useUnmergedTree = true).assertDoesNotExist()

    // Make sure the toggle is actually on screen (not just present in semantics)
    composeTestRule.scrollUntilDisplayed(scrollTag, toggleTag)
    composeTestRule.onNodeWithTag(toggleTag, useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Wait for the list to exist, then ensure it's displayed (it may render below the fold)
    composeTestRule.waitUntilTagExists(listTag, timeoutMs = 30_000)
    // If it's not displayed yet, scroll more until it is.
    composeTestRule.scrollUntilDisplayed(scrollTag, listTag)
    composeTestRule.onNodeWithTag(listTag, useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Clarisse K.").assertExists()

    // Collapse again — the toggle may have moved, so re-scroll to it
    composeTestRule.scrollUntilDisplayed(scrollTag, toggleTag)
    composeTestRule.onNodeWithTag(toggleTag, useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Optionally ensure it disappears from the tree (not just off-screen)
    composeTestRule.waitUntilTagGone(listTag, timeoutMs = 30_000)
    composeTestRule.onNodeWithTag(listTag, useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun emailField_updatesValueOnInput() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()

    val emailFieldTag = "EmailField"
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
}
