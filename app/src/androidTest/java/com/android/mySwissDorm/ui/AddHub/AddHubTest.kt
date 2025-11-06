package com.android.mySwissDorm.ui.AddHub

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.ui.add.AddHubScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddHubTest {

  @get:Rule val compose = createComposeRule()

  private fun setContent(
      showBack: Boolean = true,
      onBack: () -> Unit = {},
      onAddReview: () -> Unit = {},
      onAddListing: () -> Unit = {}
  ) {
    compose.setContent {
      MaterialTheme {
        AddHubScreen(onBack = onBack, onAddReview = onAddReview, onAddListing = onAddListing)
      }
    }
  }

  @Test
  fun renders_title_and_two_buttons() {
    setContent()

    // Title
    compose.onNodeWithText("What would you like to add?").assertIsDisplayed()

    // Buttons
    compose.onNodeWithText("I want to add a review").assertIsDisplayed().assertIsEnabled()

    compose.onNodeWithText("I want to add a listing").assertIsDisplayed().assertIsEnabled()

    // Back icon visible by default
    compose.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun clicking_buttons_invoke_callbacks() {
    var backCalled = false
    var reviewCalled = false
    var listingCalled = false

    setContent(
        onBack = { backCalled = true },
        onAddReview = { reviewCalled = true },
        onAddListing = { listingCalled = true })

    compose.onNodeWithContentDescription("Back").performClick()
    compose.onNodeWithText("I want to add a review").performClick()
    compose.onNodeWithText("I want to add a listing").performClick()

    assertTrue(backCalled)
    assertTrue(reviewCalled)
    assertTrue(listingCalled)
  }
}
