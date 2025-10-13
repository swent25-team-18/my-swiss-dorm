package com.android.mySwissDorm.homepage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.city.CityName
import com.android.mySwissDorm.ui.homepage.HomePageScreen
import com.android.mySwissDorm.ui.homepage.HomePageScreenTestTags
import com.android.mySwissDorm.ui.homepage.HomePageViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomePageScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: HomePageViewModel

  val repository = CitiesRepositoryProvider.repository
  val allCities = CityName.entries.toTypedArray()

  @Before
  fun setup() {
    viewModel = HomePageViewModel(repository)
    composeTestRule.setContent { HomePageScreen(viewModel) }
  }

  @Test
  fun testBasicElementsAreDisplayed() {
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR_TEXT_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.CONTACT_SUPPORT).assertIsDisplayed()
  }

  @Test
  fun testCityCards() {
    allCities.forEach {
      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
            .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard(it))
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      composeTestRule
          .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(it))
          .performScrollTo()
      composeTestRule.waitForIdle()
      composeTestRule
          .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(it))
          .assertIsDisplayed()

      composeTestRule
          .onNodeWithTag(
              HomePageScreenTestTags.getTestTagForCityCardTitle(it), useUnmergedTree = true)
          .assertIsDisplayed()

      composeTestRule
          .onNodeWithTag(
              HomePageScreenTestTags.getTestTagForCityCardDescription(it), useUnmergedTree = true)
          .assertIsDisplayed()
    }
  }

  @Test
  fun testSearchTextField() {
    // Check that the search bar field is displayed
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR_TEXT_FIELD).assertIsDisplayed()

    // Type text into the search bar
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR_TEXT_FIELD)
        .performTextInput("Example")

    // Verify that the value is updated
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR_TEXT_FIELD)
        .assertTextEquals("Example")
  }

  @Test
  fun testSearchTextFieldWorksForCityName() {
    // Check that the search bar field is displayed
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR_TEXT_FIELD).assertIsDisplayed()

    allCities.forEach {
      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
            .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard(it))
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      composeTestRule
          .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(it))
          .performScrollTo()
      composeTestRule.waitForIdle()
      composeTestRule
          .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(it))
          .assertIsDisplayed()
    }

    // Type text into the search bar
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR_TEXT_FIELD)
        .performTextInput("Laus")

    // Check that the "Lausanne" card is still displayed
    composeTestRule.waitUntil(timeoutMillis = 3_000) {
      composeTestRule
          .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard(CityName.LAUSANNE))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(CityName.LAUSANNE))
        .performScrollTo()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(CityName.LAUSANNE))
        .assertIsDisplayed()

    allCities
        .filter { city -> city != CityName.LAUSANNE }
        .forEach {
          composeTestRule
              .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(it))
              .assertIsNotDisplayed()
        }
  }

  @Test
  fun testSearchTextFieldWorksForCityDescription() {
    // Check that the search bar field is displayed
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR_TEXT_FIELD).assertIsDisplayed()

    allCities.forEach {
      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
            .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard(it))
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      composeTestRule
          .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(it))
          .performScrollTo()
      composeTestRule.waitForIdle()
      composeTestRule
          .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(it))
          .assertIsDisplayed()
    }

    // Type text into the search bar
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR_TEXT_FIELD)
        .performTextInput("major financial")

    // Check that the "Zürich" card is still displayed
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(CityName.ZURICH))
        .performScrollTo()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(CityName.ZURICH))
        .assertIsDisplayed()

    allCities
        .filter { city -> city != CityName.ZURICH }
        .forEach {
          composeTestRule
              .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(it))
              .assertIsNotDisplayed()
        }
  }
}
