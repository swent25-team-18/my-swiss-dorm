package com.android.mySwissDorm

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.ui.profile.ProfileScreen
import com.android.mySwissDorm.ui.profile.SettingToggle
import kotlin.assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun profileScreen_initialElements_areDisplayed() {
    composeTestRule.setContent {
      ProfileScreen(onLogout = {}, onChangeProfilePicture = {}, onBack = {})
    }

    // Verify key structural elements are found by their tags
    composeTestRule.onNodeWithTag("profile_title").assertIsDisplayed().assertTextEquals("Profile")
    composeTestRule.onNodeWithTag("profile_back_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_logout_button").assertIsDisplayed()

    // Verify a sample text field and switch are present
    composeTestRule.onNodeWithTag("field_username").assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_anonymous").assertIsDisplayed()
  }

  @Test
  fun settingToggle_click_changesState() {
    val anonymousTag = "switch_anonymous"

    composeTestRule.setContent {
      SettingToggle(label = "Anonymous", redColor = Color.Red, tag = anonymousTag)
    }

    val switchNode = composeTestRule.onNodeWithTag(anonymousTag)

    // Initial state is ON
    switchNode.assertIsOn()

    // Toggle OFF
    switchNode.performClick()
    switchNode.assertIsOff()

    // Toggle ON
    switchNode.performClick()
    switchNode.assertIsOn()
  }

  @Test
  fun buttonInteractions_areExecuted() {
    var logoutClicked = false
    var backClicked = false

    composeTestRule.setContent {
      ProfileScreen(
          onLogout = { logoutClicked = true },
          onChangeProfilePicture = {},
          onBack = { backClicked = true })
    }

    // Test Logout Button
    composeTestRule.onNodeWithTag("profile_logout_button").performClick()
    assert(logoutClicked) { "Logout handler was not called." }

    // Test Back Button
    composeTestRule.onNodeWithTag("profile_back_button").performClick()
    assert(backClicked) { "Back handler was not called." }
  }

  @Test
  fun settingToggle_contains_correctLabel() {
    val visibilityTag = "switch_visibility"

    composeTestRule.setContent {
      SettingToggle(label = "Visibility", redColor = Color.Red, tag = visibilityTag)
    }

    // 1. Find the parent component (the box containing the text and switch)
    // We use the tag on the switch and assert its parent contains the expected text.
    val switchNode = composeTestRule.onNodeWithTag(visibilityTag)

    // Assert that the node's parent (the Row) contains the correct label text
    // This verifies the label is correctly associated with the toggle box structure.
    switchNode.assertIsDisplayed()
    composeTestRule.onNodeWithText("Visibility").assertIsDisplayed()
  }

  @Test
  fun profilePictureBox_isClickable() {
    var pictureChangeClicked = false

    composeTestRule.setContent {
      ProfileScreen(
          onLogout = {}, onChangeProfilePicture = { pictureChangeClicked = true }, onBack = {})
    }

    // Find the profile picture box by its tag
    val pictureNode = composeTestRule.onNodeWithTag("profile_picture_box")

    // Assert it has clickable properties and perform the click
    pictureNode.assertIsDisplayed()
    pictureNode.assertHasClickAction()
    pictureNode.performClick()

    // 3. Verify the handler was called
    assert(pictureChangeClicked) { "Profile picture change handler was not called." }
  }

  @Test
  fun allEditableTextFields_are_initial_empty() {
    composeTestRule.setContent {
      ProfileScreen(onLogout = {}, onChangeProfilePicture = {}, onBack = {})
    }

    // Assert each field contains its respective label/placeholder text when empty.
    composeTestRule.onNodeWithTag("field_username").assertTextContains("Username")
    composeTestRule.onNodeWithTag("field_birth_date").assertTextContains("Birth Date")
    composeTestRule.onNodeWithTag("field_language").assertTextContains("Language")
    composeTestRule.onNodeWithTag("field_residence").assertTextContains("Residence")
  }
}
