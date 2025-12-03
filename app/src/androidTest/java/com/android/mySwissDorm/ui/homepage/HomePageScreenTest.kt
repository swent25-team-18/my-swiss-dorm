package com.android.mySwissDorm.ui.homepage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import com.android.mySwissDorm.model.city.CitiesRepositoryFirestore
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class HomePageScreenTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: HomePageViewModel

  val allCities = listOf("Fribourg", "Geneva", "Lausanne", "Zurich")

  override fun createRepositories() {
    CitiesRepositoryProvider.repository = CitiesRepositoryFirestore(db = FirebaseEmulator.firestore)
    ProfileRepositoryProvider.repository =
        ProfileRepositoryFirestore(db = FirebaseEmulator.firestore)

    val repository = CitiesRepositoryProvider.repository

    val cityLausanne =
        City(
            name = "Lausanne",
            description =
                "Lausanne is a city located on Lake Geneva, known for its universities and the Olympic Museum.",
            location = Location(name = "Lausanne", latitude = 46.5197, longitude = 6.6323),
            imageId = "lausanne.png")
    val cityGeneva =
        City(
            name = "Geneva",
            description = "Geneva is a global city, hosting numerous international organizations.",
            location = Location(name = "Geneva", latitude = 46.2044, longitude = 6.1432),
            imageId = "geneva.png")
    val cityZurich =
        City(
            name = "Zurich",
            description = "Zurich is the largest city in Switzerland and a major financial hub.",
            location = Location(name = "Zürich", latitude = 47.3769, longitude = 8.5417),
            imageId = "zurich.png")
    val cityFribourg =
        City(
            name = "Fribourg",
            description = "Fribourg is a bilingual city famous for its medieval architecture.",
            location = Location(name = "Fribourg", latitude = 46.8065, longitude = 7.16197),
            imageId = "fribourg.png")

    val cities = listOf(cityLausanne, cityGeneva, cityZurich, cityFribourg)
    runTest {
      switchToUser(FakeUser.FakeUser1)
      cities.forEach { repository.addCity(it) }
    }
  }

  @Before
  fun setup() {
    super.setUp()
    // Create a mock LocationRepository that returns empty list
    val mockLocationRepository =
        object : LocationRepository {
          override suspend fun search(query: String): List<Location> = emptyList()

          override suspend fun reverseSearch(latitude: Double, longitude: Double): Location? {
            return null
          }
        }
    val photoRepositoryCloud: PhotoRepositoryCloud = mock()
    runBlocking {
      whenever(photoRepositoryCloud.retrievePhoto("fribourg.png")).thenReturn(photo)
      whenever(photoRepositoryCloud.retrievePhoto("lausanne.png")).thenReturn(photo)
      whenever(photoRepositoryCloud.retrievePhoto("geneva.png")).thenReturn(photo)
      whenever(photoRepositoryCloud.retrievePhoto("zurich.png")).thenReturn(photo)
    }
    viewModel =
        HomePageViewModel(
            citiesRepository = CitiesRepositoryProvider.repository,
            locationRepository = mockLocationRepository,
            profileRepository = ProfileRepositoryProvider.repository,
            auth = FirebaseEmulator.auth,
            photoRepositoryCloud = photoRepositoryCloud)
  }

  @After
  override fun tearDown() {
    unmockkAll()
    super.tearDown()
  }

  @Test
  fun testBasicElementsAreDisplayed() {
    composeTestRule.setContent { HomePageScreen(viewModel) }
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.SEARCH_BAR_TEXT_FIELD).assertIsDisplayed()
  }

  @Test
  fun testCityCards() {
    composeTestRule.setContent { HomePageScreen(viewModel) }
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
    composeTestRule.setContent { HomePageScreen(viewModel) }
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
    composeTestRule.setContent { HomePageScreen(viewModel) }
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
    composeTestRule.setContent { HomePageScreen(viewModel) }
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

  @Test
  fun customLocationDialog_savesLocationToProfile() = runTest {
    composeTestRule.setContent { HomePageScreen(viewModel) }
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Use profile1 with no location and updated ownerId
    ProfileRepositoryProvider.repository.createProfile(
        profile1.copy(userInfo = profile1.userInfo.copy(location = null), ownerId = uid))

    // Open custom location dialog
    viewModel.onCustomLocationClick()
    composeTestRule.waitForIdle()

    // Check dialog is displayed
    composeTestRule.onNodeWithTag(C.CustomLocationDialogTags.DIALOG_TITLE).assertIsDisplayed()

    // Set a location
    val testLocation = Location("Lausanne", 46.5197, 6.6323)
    viewModel.setCustomLocation(testLocation)
    composeTestRule.waitForIdle()

    // Save location to profile
    viewModel.saveLocationToProfile(testLocation)

    // Wait for the save to complete by checking if profile was updated
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      runBlocking {
        try {
          val profile = ProfileRepositoryProvider.repository.getProfile(uid)
          profile.userInfo.location?.name == "Lausanne"
        } catch (_: Exception) {
          false
        }
      }
    }

    // Verify location was saved to profile
    val updatedProfile = runBlocking { ProfileRepositoryProvider.repository.getProfile(uid) }
    assertEquals(
        "Location should be saved to profile", "Lausanne", updatedProfile.userInfo.location?.name)
  }

  @Test
  fun customLocationDialog_confirmButtonSavesLocation() = runTest {
    composeTestRule.setContent { HomePageScreen(viewModel) }
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Use profile1 with no location and updated ownerId
    ProfileRepositoryProvider.repository.createProfile(
        profile1.copy(userInfo = profile1.userInfo.copy(location = null), ownerId = uid))

    // Open custom location dialog
    viewModel.onCustomLocationClick()
    composeTestRule.waitForIdle()

    // Wait for dialog to appear
    composeTestRule.waitUntil(timeoutMillis = 3_000) {
      composeTestRule
          .onAllNodesWithTag(C.CustomLocationDialogTags.DIALOG_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Set a location
    val testLocation = Location("Geneva", 46.2044, 6.1432)
    viewModel.setCustomLocation(testLocation)
    composeTestRule.waitForIdle()

    // Click confirm (this should save to profile)
    composeTestRule.onNodeWithTag(C.CustomLocationDialogTags.CONFIRM_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Wait for dialog to disappear (indicating save completed)
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(C.CustomLocationDialogTags.DIALOG_TITLE)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    // Verify location was saved to profile
    val updatedProfile = ProfileRepositoryProvider.repository.getProfile(uid)
    assertEquals(
        "Location should be saved to profile", "Geneva", updatedProfile.userInfo.location?.name)
  }

  @Test
  fun clickingCityCard_savesLocationToProfile() = runTest {
    composeTestRule.setContent { HomePageScreen(viewModel) }
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Use profile1 with no location and updated ownerId
    ProfileRepositoryProvider.repository.createProfile(
        profile1.copy(userInfo = profile1.userInfo.copy(location = null), ownerId = uid))

    // Wait for cities list to be available
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(HomePageScreenTestTags.CITIES_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll the cities list to Lausanne's index (first city, index 2) to bring it into view
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).performScrollToIndex(2)

    // Wait for Lausanne card to be displayed after scrolling
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to Lausanne card and wait
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
        .performScrollTo()
    composeTestRule.waitForIdle()

    // Click on Lausanne city card
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
        .performClick()
    composeTestRule.waitForIdle()

    // Wait for the save to complete by checking if profile was updated
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      runBlocking {
        try {
          val updatedProfile = ProfileRepositoryProvider.repository.getProfile(uid)
          updatedProfile.userInfo.location?.name == "Lausanne"
        } catch (_: Exception) {
          false
        }
      }
    }

    // Verify location was saved to profile
    val updatedProfile = runBlocking { ProfileRepositoryProvider.repository.getProfile(uid) }
    val savedLocation = updatedProfile.userInfo.location
    assertNotNull("Location should not be null", savedLocation)
    val location = savedLocation!! // Safe to use !! after assertNotNull
    assertEquals(
        "Location should be saved to profile after clicking city card", "Lausanne", location.name)
    assertEquals("Latitude should match", 46.5197, location.latitude, 0.0001)
    assertEquals("Longitude should match", 6.6323, location.longitude, 0.0001)
  }

  @Test
  fun clickingCityCard_updatesExistingLocation() = runTest {
    composeTestRule.setContent { HomePageScreen(viewModel) }
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Use profile1 with updated location and ownerId
    val existingLocation = Location("Geneva", 46.2044, 6.1432)
    ProfileRepositoryProvider.repository.createProfile(
        profile1.copy(
            userInfo = profile1.userInfo.copy(location = existingLocation), ownerId = uid))

    // Verify initial location
    val currentProfile = runBlocking { ProfileRepositoryProvider.repository.getProfile(uid) }
    assertEquals(
        "Initial location should be Geneva", "Geneva", currentProfile.userInfo.location?.name)

    // Wait for cities list to be available
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(HomePageScreenTestTags.CITIES_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll the cities list to Zurich's index (third city, index 3) to bring it into view
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).performScrollToIndex(3)

    // Wait for Zurich card to be displayed after scrolling
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard("Zurich"))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to Zurich card and wait
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Zurich"))
        .performScrollTo()
    composeTestRule.waitForIdle()

    // Click on Zurich city card
    composeTestRule
        .onNodeWithTag(HomePageScreenTestTags.getTestTagForCityCard("Zurich"))
        .performClick()
    composeTestRule.waitForIdle()

    // Wait for the save to complete
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      runBlocking {
        try {
          val updatedProfile = ProfileRepositoryProvider.repository.getProfile(uid)
          updatedProfile.userInfo.location?.name == "Zürich"
        } catch (_: Exception) {
          false
        }
      }
    }

    // Verify location was updated
    val updatedProfile = runBlocking { ProfileRepositoryProvider.repository.getProfile(uid) }
    val savedLocation = updatedProfile.userInfo.location
    assertNotNull("Location should not be null", savedLocation)
    val location = savedLocation!! // Safe to use !! after assertNotNull
    assertEquals("Location should be updated to Zurich", "Zürich", location.name)
    assertEquals("Latitude should match Zurich", 47.3769, location.latitude, 0.0001)
    assertEquals("Longitude should match Zurich", 8.5417, location.longitude, 0.0001)
  }
}
