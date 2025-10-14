package com.android.mySwissDorm.ui.settings

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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

  private fun waitUntilDisplayed(tag: String, timeoutMs: Long = 5_000) {
    composeTestRule.waitUntil(timeoutMs) {
      try {
        composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
        true
      } catch (_: AssertionError) {
        false
      }
    }
  }

  @Test
  fun settingsScreen_showsProfileRowAndButton() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }

    // Default VM username is "Sophie"
    composeTestRule.onNodeWithText("Sophie").assertIsDisplayed()
    composeTestRule.onNodeWithText("View profile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ProfileButton", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun backButton_triggersOnGoBackCallback() {
    val onGoBackCalled = AtomicBoolean(false)
    composeTestRule.setContent {
      MySwissDormAppTheme { SettingsScreen(onGoBack = { onGoBackCalled.set(true) }) }
    }
    composeTestRule.onNodeWithTag("BackButton", useUnmergedTree = true).performClick()
    assert(onGoBackCalled.get()) { "onGoBack callback was not called." }
  }

  @Test
  fun deleteAccountButton_triggersOnItemClickCallback() {
    val clickedItem = AtomicReference<String?>(null)
    composeTestRule.setContent {
      MySwissDormAppTheme { SettingsScreen(onItemClick = { item -> clickedItem.set(item) }) }
    }
    composeTestRule.onNodeWithTag("DeleteAccountButton", useUnmergedTree = true).performClick()
    assert(clickedItem.get() == "Delete my account") {
      "onItemClick was not called with 'Delete my account'."
    }
  }

  @Test
  fun notificationSwitches_toggleStateCorrectly() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }

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

    composeTestRule.onNodeWithTag("BlockedContactsList").assertDoesNotExist()

    composeTestRule.onNodeWithTag("BlockedContactsToggle", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    waitUntilDisplayed("BlockedContactsList")
    composeTestRule.onNodeWithTag("BlockedContactsList").assertIsDisplayed()
    composeTestRule.onNodeWithText("Clarisse K.").assertIsDisplayed()

    composeTestRule.onNodeWithTag("BlockedContactsToggle", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithTag("BlockedContactsList").assertDoesNotExist()
  }

  @Test
  fun emailField_updatesValueOnInput() {
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreen() } }

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
