package com.android.mySwissDorm.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    // App bar items are already visible (not scrollable)
    composeTestRule.onNodeWithTag("profile_title").assertIsDisplayed().assertTextEquals("Profile")
    composeTestRule.onNodeWithTag("profile_back_button").assertIsDisplayed()

    // Content lives inside the scrollable Column tagged "profile_list"
    composeTestRule
        .onNodeWithTag("profile_list")
        .performScrollToNode(hasTestTag("profile_logout_button"))
    composeTestRule.onNodeWithTag("profile_logout_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("field_username"))
    composeTestRule.onNodeWithTag("field_username").assertIsDisplayed()

    composeTestRule
        .onNodeWithTag("profile_list")
        .performScrollToNode(hasTestTag("switch_anonymous"))
    composeTestRule.onNodeWithTag("switch_anonymous").assertIsDisplayed()
  }

  @Test
  fun settingToggle_click_changesState() {
    val anonymousTag = "switch_anonymous"

    composeTestRule.setContent {
      // IMPORTANT: remember the state
      var isChecked by remember { mutableStateOf(true) }
      com.android.mySwissDorm.ui.profile.SettingToggle(
          label = "Anonymous",
          redColor = Color.Red,
          checked = isChecked,
          onCheckedChange = { isChecked = it },
          tag = anonymousTag)
    }

    val switchNode = composeTestRule.onNodeWithTag(anonymousTag)
    switchNode.assertIsOn()

    switchNode.performClick()
    composeTestRule.waitForIdle()
    switchNode.assertIsOff()

    switchNode.performClick()
    composeTestRule.waitForIdle()
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

    // Logout: scroll the **Button** into view, ensure it's clickable, then click
    composeTestRule
        .onNode(hasTestTag("profile_logout_button") and hasClickAction(), useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(2_000) { logoutClicked }
    assert(logoutClicked) { "Logout handler was not called." }

    // Back button (likely an IconButton in TopAppBar)
    composeTestRule
        .onNode(hasTestTag("profile_back_button") and hasClickAction(), useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(2_000) { backClicked }
    assert(backClicked) { "Back handler was not called." }
  }

  @Test
  fun settingToggle_contains_correctLabel() {
    val visibilityTag = "switch_visibility"

    composeTestRule.setContent {
      var isChecked by remember { mutableStateOf(true) }
      com.android.mySwissDorm.ui.profile.SettingToggle(
          label = "Visibility",
          redColor = Color.Red,
          checked = isChecked,
          onCheckedChange = { isChecked = it },
          tag = visibilityTag)
    }

    composeTestRule.onNodeWithTag(visibilityTag).assertIsDisplayed()
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
    composeTestRule.onNodeWithTag("field_language").assertTextContains("Language")
    composeTestRule.onNodeWithTag("field_residence").assertTextContains("Residence")
  }
}
