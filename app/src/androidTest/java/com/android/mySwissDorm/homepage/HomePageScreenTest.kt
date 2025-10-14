package com.android.mySwissDorm.homepage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.city.CitiesRepositoryFirestore
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.model.city.CityName
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.ui.homepage.HomePageScreen
import com.android.mySwissDorm.ui.homepage.HomePageScreenTestTags
import com.android.mySwissDorm.ui.homepage.HomePageViewModel
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

  val allCities = CityName.entries.toTypedArray()

  override fun createRepositories() {
    CitiesRepositoryProvider.repository = CitiesRepositoryFirestore(db = FirebaseEmulator.firestore)

    val repository = CitiesRepositoryProvider.repository

    val cityLausanne =
        City(
            name = CityName.LAUSANNE,
            description =
                "Lausanne is a city located on Lake Geneva, known for its universities and the Olympic Museum.",
            location = Location(name = "Lausanne", latitude = 46.5197, longitude = 6.6323),
            imageId = R.drawable.lausanne)
    val cityGeneva =
        City(
            name = CityName.GENEVA,
            description = "Geneva is a global city, hosting numerous international organizations.",
            location = Location(name = "Geneva", latitude = 46.2044, longitude = 6.1432),
            imageId = R.drawable.geneve)
    val cityZurich =
        City(
            name = CityName.ZURICH,
            description = "Zurich is the largest city in Switzerland and a major financial hub.",
            location = Location(name = "Zürich", latitude = 47.3769, longitude = 8.5417),
            imageId = R.drawable.zurich)
    val cityFribourg =
        City(
            name = CityName.FRIBOURG,
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
