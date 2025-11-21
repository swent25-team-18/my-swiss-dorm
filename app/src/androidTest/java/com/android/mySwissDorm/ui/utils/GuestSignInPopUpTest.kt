package com.android.mySwissDorm.ui.utils

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Rule
import org.junit.Test

class GuestSignInPopUpTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun popUp_displaysCorrectContentAndHandlesClicks() {
    var signInClicked = false
    var backClicked = false

    composeTestRule.setContent {
      MySwissDormAppTheme {
        SignInPopUp(
            onSignInClick = { signInClicked = true },
            onBack = { backClicked = true },
            title = "Profile")
      }
    }
    composeTestRule.onNodeWithText("Sign in to create a profile").assertIsDisplayed()
    composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sign In").performClick()
    assert(signInClicked) { "Sign In click callback was not triggered" }
    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    assert(backClicked) { "Back click callback was not triggered" }
  }
}
