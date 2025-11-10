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
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.city.CitiesRepositoryFirestore
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
    // Create a mock LocationRepository that returns empty list
    val mockLocationRepository =
        object : LocationRepository {
          override suspend fun search(query: String): List<Location> = emptyList()
        }
    viewModel =
        HomePageViewModel(
            citiesRepository = CitiesRepositoryProvider.repository,
            locationRepository = mockLocationRepository,
            profileRepository = ProfileRepositoryProvider.repository,
            auth = FirebaseEmulator.auth)
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

  @Test
  fun customLocationDialog_savesLocationToProfile() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile without location
    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = FakeUser.FakeUser1.userName,
                    lastName = "Test",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "+41001112233",
                    location = null),
            userSettings = UserSettings(),
            ownerId = uid)
    ProfileRepositoryProvider.repository.createProfile(profile)

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
        } catch (e: Exception) {
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
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile without location
    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = FakeUser.FakeUser1.userName,
                    lastName = "Test",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "+41001112233",
                    location = null),
            userSettings = UserSettings(),
            ownerId = uid)
    ProfileRepositoryProvider.repository.createProfile(profile)

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
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile without location
    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = FakeUser.FakeUser1.userName,
                    lastName = "Test",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "+41001112233",
                    location = null),
            userSettings = UserSettings(),
            ownerId = uid)
    ProfileRepositoryProvider.repository.createProfile(profile)

    // Wait for cities to load
    composeTestRule.waitUntil(timeoutMillis = 3_000) {
      composeTestRule
          .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard("Lausanne"))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to Lausanne card
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).performScrollToIndex(0)
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
        } catch (e: Exception) {
          false
        }
      }
    }

    // Verify location was saved to profile
    val updatedProfile = runBlocking { ProfileRepositoryProvider.repository.getProfile(uid) }
    assertEquals(
        "Location should be saved to profile after clicking city card",
        "Lausanne",
        updatedProfile.userInfo.location?.name)
    assertNotNull("Location should not be null", updatedProfile.userInfo.location)
    assertEquals(
        "Latitude should match", 46.5197, updatedProfile.userInfo.location!!.latitude, 0.0001)
    assertEquals(
        "Longitude should match", 6.6323, updatedProfile.userInfo.location!!.longitude, 0.0001)
  }

  @Test
  fun clickingCityCard_updatesExistingLocation() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Create profile with existing location
    val existingLocation = Location("Geneva", 46.2044, 6.1432)
    val profile =
        Profile(
            userInfo =
                UserInfo(
                    name = FakeUser.FakeUser1.userName,
                    lastName = "Test",
                    email = FakeUser.FakeUser1.email,
                    phoneNumber = "+41001112233",
                    location = existingLocation),
            userSettings = UserSettings(),
            ownerId = uid)
    ProfileRepositoryProvider.repository.createProfile(profile)

    // Verify initial location
    var currentProfile = runBlocking { ProfileRepositoryProvider.repository.getProfile(uid) }
    assertEquals(
        "Initial location should be Geneva", "Geneva", currentProfile.userInfo.location?.name)

    // Wait for cities to load
    composeTestRule.waitUntil(timeoutMillis = 3_000) {
      composeTestRule
          .onAllNodesWithTag(HomePageScreenTestTags.getTestTagForCityCard("Zurich"))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to Zurich card
    composeTestRule.onNodeWithTag(HomePageScreenTestTags.CITIES_LIST).performScrollToIndex(2)
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
        } catch (e: Exception) {
          false
        }
      }
    }

    // Verify location was updated
    val updatedProfile = runBlocking { ProfileRepositoryProvider.repository.getProfile(uid) }
    assertEquals(
        "Location should be updated to Zurich", "Zürich", updatedProfile.userInfo.location?.name)
    assertNotNull("Location should not be null", updatedProfile.userInfo.location)
    assertEquals(
        "Latitude should match Zurich",
        47.3769,
        updatedProfile.userInfo.location!!.latitude,
        0.0001)
    assertEquals(
        "Longitude should match Zurich",
        8.5417,
        updatedProfile.userInfo.location!!.longitude,
        0.0001)
  }
}
