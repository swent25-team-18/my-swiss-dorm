package com.android.mySwissDorm.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Rule
import org.junit.Test

/** UI tests for the [SettingsScreenContent] composable. */
class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun settingsScreen_displaysCorrectUsernameAndProfileButton() {
    val testUiState = SettingsUiState(userName = "Sophie Urrea")
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreenContent(ui = testUiState) } }

    composeTestRule.onNodeWithText("Sophie Urrea").assertIsDisplayed()
    composeTestRule.onNodeWithText("View profile").assertIsDisplayed()
    // In case semantics are merged, use unmerged tree for direct hit
    composeTestRule.onNodeWithTag("ProfileButton", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun backButton_triggersOnGoBackCallback() {
    val onGoBackCalled = AtomicBoolean(false)
    composeTestRule.setContent {
      MySwissDormAppTheme {
        SettingsScreenContent(ui = SettingsUiState(), onGoBack = { onGoBackCalled.set(true) })
      }
    }

    composeTestRule.onNodeWithTag("BackButton", useUnmergedTree = true).performClick()
    assert(onGoBackCalled.get()) { "onGoBack callback was not called." }
  }

  @Test
  fun deleteAccountButton_triggersOnItemClickCallback() {
    val clickedItem = AtomicReference<String?>(null)
    composeTestRule.setContent {
      MySwissDormAppTheme {
        SettingsScreenContent(
            ui = SettingsUiState(), onItemClick = { item -> clickedItem.set(item) })
      }
    }

    composeTestRule.onNodeWithTag("DeleteAccountButton", useUnmergedTree = true).performClick()
    assert(clickedItem.get() == "Delete my account") {
      "onItemClick was not called with 'Delete my account'."
    }
  }

  @Test
  fun notificationSwitches_toggleStateCorrectly() {
    composeTestRule.setContent {
      MySwissDormAppTheme { SettingsScreenContent(ui = SettingsUiState()) }
    }

    // Messages Switch
    val messagesSwitch =
        composeTestRule.onNodeWithTag(
            "SettingSwitch_Show notifications for messages", useUnmergedTree = true)
    messagesSwitch.assertIsOn()
    messagesSwitch.performClick()
    messagesSwitch.assertIsOff()

    // Listings Switch
    val listingsSwitch =
        composeTestRule.onNodeWithTag(
            "SettingSwitch_Show notifications for new listings", useUnmergedTree = true)
    listingsSwitch.assertIsOff()
    listingsSwitch.performClick()
    listingsSwitch.assertIsOn()
  }

  @Test
  fun blockedContacts_expandsAndCollapsesOnClick() {
    composeTestRule.setContent {
      MySwissDormAppTheme { SettingsScreenContent(ui = SettingsUiState()) }
    }

    // Initially collapsed
    composeTestRule.onNodeWithTag("BlockedContactsList").assertDoesNotExist()

    // Expand
    composeTestRule.onNodeWithTag("BlockedContactsToggle", useUnmergedTree = true).performClick()

    // Now list should exist and be visible (auto-scrolled into view)
    composeTestRule.onNodeWithTag("BlockedContactsList").assertIsDisplayed()
    composeTestRule.onNodeWithText("Clarisse K.").assertIsDisplayed()

    // Collapse
    composeTestRule.onNodeWithTag("BlockedContactsToggle", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithTag("BlockedContactsList").assertDoesNotExist()
  }

  @Test
  fun emailField_updatesValueOnInput() {
    composeTestRule.setContent {
      MySwissDormAppTheme { SettingsScreenContent(ui = SettingsUiState()) }
    }

    val emailField = composeTestRule.onNodeWithTag("EmailField", useUnmergedTree = true)

    emailField.assert(hasText("john.doe@email.com"))
    emailField.performTextClearance()
    emailField.performTextInput("new.email@example.com")
    emailField.assert(hasText("new.email@example.com"))
  }
}
