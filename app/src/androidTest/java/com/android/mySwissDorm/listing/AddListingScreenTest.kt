package com.android.mySwissDorm.ui.overview

import AddListingScreen
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.ui.listing.AddListingViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddListingScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: AddListingViewModel

  @Before
  fun setup() {
    viewModel = AddListingViewModel()
    composeTestRule.setContent { AddListingScreen(onOpenMap = {}, onConfirm = {}, onBack = {}) }
  }

  @Test
  fun testTitleTextField() {
    // Check that the title input field is displayed
    composeTestRule.onNodeWithText("Listing title").assertIsDisplayed()

    // Type text into the title input
    composeTestRule.onNodeWithText("Listing title").performTextInput("Beautiful Studio")

    // Verify that the value is updated
    composeTestRule.onNodeWithText("Beautiful Studio").assertExists()
  }

  @Test
  fun testResidencyNameTextField() {
    composeTestRule.onNodeWithText("Location / Residency name").assertIsDisplayed()
    composeTestRule.onNodeWithText("Location / Residency name").performTextInput("Green Apartments")
    composeTestRule.onNodeWithText("Green Apartments").assertExists()
  }

  @Test
  fun testHousingTypeDropdown() {
    composeTestRule.onNodeWithText("Housing type").assertIsDisplayed()
    composeTestRule.onNodeWithText("Housing type").performClick()

    // Select a housing type
    composeTestRule.onNodeWithText(RoomType.STUDIO.toString()).performClick()

    // Check that the selected housing type is displayed
    composeTestRule.onNodeWithText(RoomType.STUDIO.toString()).assertExists()
  }

  @Test
  fun testFormSubmitButtonEnabledWhenValid() {
    // Simulate filling out valid data
    composeTestRule.onNodeWithText("Listing title").performTextInput("Beautiful Studio")
    composeTestRule.onNodeWithText("Location / Residency name").performTextInput("Green Apartments")
    composeTestRule.onNodeWithText("Housing type").performClick()
    composeTestRule.onNodeWithText(RoomType.STUDIO.toString()).performClick()
    composeTestRule.onNodeWithText("Room size (m²)").performTextInput("25")

    // Verify that the submit button is enabled
    composeTestRule.onNodeWithText("Confirm listing").assertIsEnabled()
  }

  @Test
  fun testFormSubmitButtonDisabledWhenInvalid() {
    // Simulate invalid data (e.g., empty title)
    composeTestRule.onNodeWithText("Location / Residency name").performTextInput("Green Apartments")
    composeTestRule.onNodeWithText("Housing type").performClick()
    composeTestRule.onNodeWithText(RoomType.STUDIO.toString()).performClick()
    composeTestRule.onNodeWithText("Room size (m²)").performTextInput("25")

    // Verify that the submit button is disabled due to missing title
    composeTestRule.onNodeWithText("Confirm listing").assertIsNotEnabled()
  }
}
