package com.android.mySwissDorm.ui.settings

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Rule
import org.junit.Test

/** UI tests for the Settings screen (screen owns its ViewModel). */
class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  /** Robust existence wait that works on slow emulators. */
  private fun waitUntilTagExists(tag: String, timeoutMs: Long = 15_000) {
    composeTestRule.waitUntil(timeoutMs) {
      composeTestRule
          .onAllNodesWithTag(tag, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  /** Wait until **no** nodes with tag exist (for collapse). */
  private fun waitUntilTagGone(tag: String, timeoutMs: Long = 15_000) {
    composeTestRule.waitUntil(timeoutMs) {
      composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
    }
  }

  @Test
  fun settingsScreen_showsProfileRowAndButton() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }
    composeTestRule.waitForIdle()

    // Default VM username is "Sophie"
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

    // Messages: starts On -> click -> Off
    composeTestRule
        .onNodeWithTag(messagesTag, useUnmergedTree = true)
        .assert(hasStateDescription("On"))
        .performClick()
    composeTestRule
        .onNodeWithTag(messagesTag, useUnmergedTree = true)
        .assert(hasStateDescription("Off"))

    // Listings: starts Off -> click -> On
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

    val listTag = "BlockedContactsList"
    val toggleTag = "BlockedContactsToggle"

    // Starts collapsed
    composeTestRule.onNodeWithTag(listTag, useUnmergedTree = true).assertDoesNotExist()

    // Expand
    composeTestRule.onNodeWithTag(toggleTag, useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    waitUntilTagExists(listTag)
    composeTestRule.onNodeWithTag(listTag, useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("Clarisse K.").assertExists()

    // Collapse
    composeTestRule.onNodeWithTag(toggleTag, useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    waitUntilTagGone(listTag)
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
