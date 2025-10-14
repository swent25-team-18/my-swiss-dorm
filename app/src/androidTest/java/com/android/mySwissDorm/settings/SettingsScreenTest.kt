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
    // Arrange
    val testUiState = SettingsUiState(userName = "Sophie Urrea")
    composeTestRule.setContent { MySwissDormAppTheme { SettingsScreenContent(ui = testUiState) } }

    // Assert
    composeTestRule.onNodeWithText("Sophie Urrea").assertIsDisplayed()
    composeTestRule.onNodeWithText("View profile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ProfileButton").assertIsDisplayed()
  }

  @Test
  fun backButton_triggersOnGoBackCallback() {
    // Arrange
    val onGoBackCalled = AtomicBoolean(false)
    composeTestRule.setContent {
      MySwissDormAppTheme {
        SettingsScreenContent(ui = SettingsUiState(), onGoBack = { onGoBackCalled.set(true) })
      }
    }

    // Act
    composeTestRule.onNodeWithTag("BackButton").performClick()

    // Assert
    assert(onGoBackCalled.get()) { "onGoBack callback was not called." }
  }

  @Test
  fun deleteAccountButton_triggersOnItemClickCallback() {
    // Arrange
    val clickedItem = AtomicReference<String?>(null)
    composeTestRule.setContent {
      MySwissDormAppTheme {
        SettingsScreenContent(
            ui = SettingsUiState(), onItemClick = { item -> clickedItem.set(item) })
      }
    }

    // Act
    composeTestRule.onNodeWithTag("DeleteAccountButton").performClick()

    // Assert
    assert(clickedItem.get() == "Delete my account") {
      "onItemClick was not called with 'Delete my account'."
    }
  }

  @Test
  fun notificationSwitches_toggleStateCorrectly() {
    // Arrange
    composeTestRule.setContent {
      MySwissDormAppTheme { SettingsScreenContent(ui = SettingsUiState()) }
    }

    // Act & Assert for Messages Switch
    val messagesSwitch =
        composeTestRule.onNodeWithTag("SettingSwitch_Show notifications for messages")
    messagesSwitch.assertIsOn() // Initially checked in the composable's state
    messagesSwitch.performClick()
    messagesSwitch.assertIsOff()

    // Act & Assert for Listings Switch
    val listingsSwitch =
        composeTestRule.onNodeWithTag("SettingSwitch_Show notifications for new listings")
    listingsSwitch.assertIsOff() // Initially unchecked in the composable's state
    listingsSwitch.performClick()
    listingsSwitch.assertIsOn()
  }

  @Test
  fun blockedContacts_expandsAndCollapsesOnClick() {
    // Arrange
    composeTestRule.setContent {
      MySwissDormAppTheme { SettingsScreenContent(ui = SettingsUiState()) }
    }

    // Assert initial state (collapsed)
    composeTestRule.onNodeWithTag("BlockedContactsList").assertDoesNotExist()

    // Act: Expand the list
    composeTestRule.onNodeWithTag("BlockedContactsToggle").performClick()

    // Assert expanded state
    composeTestRule.onNodeWithTag("BlockedContactsList").assertIsDisplayed()
    composeTestRule.onNodeWithText("Clarisse K.").assertIsDisplayed()

    // Act: Collapse the list
    composeTestRule.onNodeWithTag("BlockedContactsToggle").performClick()

    // Assert collapsed state
    composeTestRule.onNodeWithTag("BlockedContactsList").assertDoesNotExist()
  }

  @Test
  fun emailField_updatesValueOnInput() {
    // Arrange
    composeTestRule.setContent {
      MySwissDormAppTheme { SettingsScreenContent(ui = SettingsUiState()) }
    }

    val emailField = composeTestRule.onNodeWithTag("EmailField")

    // Assert initial value
    emailField.assert(hasText("john.doe@email.com"))

    // Act: Clear and type new email
    emailField.performTextClearance()
    emailField.performTextInput("new.email@example.com")

    // Assert new value
    emailField.assert(hasText("new.email@example.com"))
  }
}
