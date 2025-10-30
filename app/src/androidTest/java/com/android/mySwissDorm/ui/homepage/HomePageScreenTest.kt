package com.android.mySwissDorm.ui.homepage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.city.CitiesRepositoryFirestore
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomePageScreenTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: HomePageViewModel

  val allCities = listOf("Fribourg", "Geneva", "Lausanne", "Zurich")

  override fun createRepositories() {
    CitiesRepositoryProvider.repository = CitiesRepositoryFirestore(db = FirebaseEmulator.firestore)

    val repository = CitiesRepositoryProvider.repository

    val cityLausanne =
        City(
            name = "Lausanne",
            description =
                "Lausanne is a city located on Lake Geneva, known for its universities and the Olympic Museum.",
            location = Location(name = "Lausanne", latitude = 46.5197, longitude = 6.6323),
            imageId = R.drawable.lausanne)
    val cityGeneva =
        City(
            name = "Geneva",
            description = "Geneva is a global city, hosting numerous international organizations.",
            location = Location(name = "Geneva", latitude = 46.2044, longitude = 6.1432),
            imageId = R.drawable.geneve)
    val cityZurich =
        City(
            name = "Zurich",
            description = "Zurich is the largest city in Switzerland and a major financial hub.",
            location = Location(name = "Zürich", latitude = 47.3769, longitude = 8.5417),
            imageId = R.drawable.zurich)
    val cityFribourg =
        City(
            name = "Fribourg",
            description = "Fribourg is a bilingual city famous for its medieval architecture.",
            location = Location(name = "Fribourg", latitude = 46.8065, longitude = 7.16197),
            imageId = R.drawable.fribourg)

    val cities = listOf(cityLausanne, cityGeneva, cityZurich, cityFribourg)
    runTest {
      switchToUser(FakeUser.FakeUser1)
      cities.forEach { repository.addCity(it) }
    }
  }

  @Before
  fun setup() {
    super.setUp()
    viewModel = HomePageViewModel(CitiesRepositoryProvider.repository)
    composeTestRule.setContent { HomePageScreen(viewModel) }
  }

  @Test
  fun testBasicElementsAreDisplayed() {
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR_TEXT_FIELD).assertIsDisplayed()
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
          .onNodeWithTag(HomePageScreenTestTags.CITIES_LIST)
          .performScrollToIndex(allCities.indexOf(it))
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
          .onNodeWithTag(HomePageScreenTestTags.CITIES_LIST)
          .performScrollToIndex(allCities.indexOf(it))
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
          .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
        .performScrollTo()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
        .assertIsDisplayed()

    allCities
        .filter { city -> city != "Lausanne" }
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
          .onNodeWithTag(HomePageScreenTestTags.CITIES_LIST)
          .performScrollToIndex(allCities.indexOf(it))
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
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Zurich"))
        .performScrollTo()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Zurich"))
        .assertIsDisplayed()

    allCities
        .filter { city -> city != "Zurich" }
        .forEach {
          composeTestRule
              .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard(it))
              .assertIsNotDisplayed()
        }
  }
}
