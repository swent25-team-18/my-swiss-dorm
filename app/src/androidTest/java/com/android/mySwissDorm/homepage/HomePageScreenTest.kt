package com.android.mySwissDorm.homepage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.ui.homepage.HomePageScreen
import com.android.mySwissDorm.ui.homepage.HomePageViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomePageScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: HomePageViewModel

  val repository = CitiesRepositoryProvider.repository

  @Before
  fun setup() {
    viewModel = HomePageViewModel(repository)
    composeTestRule.setContent { HomePageScreen() }
  }

  @Test
  fun testBasicElementsAreDisplayed() {
    composeTestRule.onNodeWithText("Browse").isDisplayed()
    composeTestRule.onNodeWithText("Contact Support").isDisplayed()
  }

  @Test
  fun testCityCards() {
    // Check that the "Lausanne" card is displayed
    composeTestRule.onNodeWithText("Lausanne").performScrollTo().assertIsDisplayed()
    composeTestRule
        .onNodeWithText(
            "Lausanne is a city located on Lake Geneva, known for its universities and the Olympic Museum.")
        .performScrollTo()
        .assertIsDisplayed()

    // Check that the "Geneva" card is displayed
    composeTestRule.onNodeWithText("Geneva").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Geneva is a global city, hosting numerous international organizations.")
        .performScrollTo()
        .assertIsDisplayed()

    // Check that the "Zürich" card is displayed
    composeTestRule.onNodeWithText("Zürich").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Zurich is the largest city in Switzerland and a major financial hub.")
        .performScrollTo()
        .assertIsDisplayed()

    // Check that the "Fribourg" card is displayed
    composeTestRule.onNodeWithText("Fribourg").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Fribourg is a bilingual city famous for its medieval architecture.")
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun testSearchTextField() {
    // Check that the search bar field is displayed
    composeTestRule.onNodeWithText("Browse").assertIsDisplayed()

    // Type text into the search bar
    composeTestRule.onNodeWithText("Browse").performTextInput("Example")

    // Verify that the value is updated
    composeTestRule.onNodeWithText("Example").assertExists()
  }

  @Test
  fun testSearchTextFieldWorks() {
    // Check that the search bar field is displayed
    composeTestRule.onNodeWithText("Browse").assertIsDisplayed()

    // Check that the "Lausanne" card is displayed
    composeTestRule.onNodeWithText("Lausanne").performScrollTo().assertIsDisplayed()

    // Check that the "Geneva" card is displayed
    composeTestRule.onNodeWithText("Geneva").performScrollTo().assertIsDisplayed()

    // Check that the "Zürich" card is displayed
    composeTestRule.onNodeWithText("Zürich").performScrollTo().assertIsDisplayed()

    // Check that the "Fribourg" card is displayed
    composeTestRule.onNodeWithText("Fribourg").performScrollTo().assertIsDisplayed()

    // Type text into the search bar
    composeTestRule.onNodeWithText("Browse").performTextInput("Laus")

    // Check that the "Lausanne" card is still displayed
    composeTestRule.onNodeWithText("Lausanne").performScrollTo().assertIsDisplayed()

    // Check that the "Geneva" card is not displayed anymore
    composeTestRule.onNodeWithText("Geneva").assertIsNotDisplayed()

    // Check that the "Zürich" card is not displayed anymore
    composeTestRule.onNodeWithText("Zürich").assertIsNotDisplayed()

    // Check that the "Fribourg" card is not displayed anymore
    composeTestRule.onNodeWithText("Fribourg").assertIsNotDisplayed()
  }
}
