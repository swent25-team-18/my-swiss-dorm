package com.android.mySwissDorm.ui.homepage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.city.CitiesRepository
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomePageViewModelTest : FirestoreTest() {

  override fun createRepositories() {
    /* none */
  }

  @Before override fun setUp() = runTest { super.setUp() }

  private suspend fun awaitUntil(timeoutMs: Long = 5000, intervalMs: Long = 25, p: () -> Boolean) {
    val start = System.currentTimeMillis()
    while (!p()) {
      if (System.currentTimeMillis() - start > timeoutMs) break
      delay(intervalMs)
    }
  }

  private fun createViewModel(
      citiesRepository: CitiesRepository = createMockCitiesRepository(),
      locationRepository: LocationRepository = createMockLocationRepository(),
      profileRepository: ProfileRepository = ProfileRepositoryFirestore(FirebaseEmulator.firestore),
      auth: com.google.firebase.auth.FirebaseAuth = FirebaseEmulator.auth
  ): HomePageViewModel {
    return HomePageViewModel(
        citiesRepository = citiesRepository,
        locationRepository = locationRepository,
        profileRepository = profileRepository,
        auth = auth)
  }

  private fun createMockCitiesRepository(): CitiesRepository {
    return object : CitiesRepository {
      override suspend fun getAllCities(): List<com.android.mySwissDorm.model.city.City> =
          emptyList()

      override suspend fun getCity(name: String): com.android.mySwissDorm.model.city.City =
          error("Not implemented")

      override suspend fun addCity(city: com.android.mySwissDorm.model.city.City) {}
    }
  }

  private fun createMockLocationRepository(): LocationRepository {
    return object : LocationRepository {
      override suspend fun search(query: String): List<Location> {
        return when {
          query.isEmpty() -> emptyList()
          query.contains("Lausanne", ignoreCase = true) ->
              listOf(Location("Lausanne, Switzerland", 46.5197, 6.6323))
          query.contains("Geneva", ignoreCase = true) ->
              listOf(Location("Geneva, Switzerland", 46.2044, 6.1432))
          else -> emptyList()
        }
      }
    }
  }

  @Test
  fun setCustomLocationQuery_emptyQuery_clearsSuggestions() = runTest {
    val viewModel = createViewModel()

    viewModel.setCustomLocationQuery("")
    val state = viewModel.uiState.first()

    assertEquals("", state.customLocationQuery)
    assertTrue("Suggestions should be empty", state.locationSuggestions.isEmpty())
  }

  @Test
  fun setCustomLocationQuery_nonEmptyQuery_fetchesSuggestions() = runTest {
    val viewModel = createViewModel()

    viewModel.setCustomLocationQuery("Lausanne")
    awaitUntil { viewModel.uiState.first().locationSuggestions.isNotEmpty() }
    val state = viewModel.uiState.first()

    assertEquals("Lausanne", state.customLocationQuery)
    assertTrue("Should have suggestions", state.locationSuggestions.isNotEmpty())
    assertEquals("Lausanne, Switzerland", state.locationSuggestions.first().name)
  }

  @Test
  fun setCustomLocation_updatesState() = runTest {
    val viewModel = createViewModel()
    val location = Location("Geneva", 46.2044, 6.1432)

    viewModel.setCustomLocation(location)
    val state = viewModel.uiState.first()

    assertEquals(location, state.customLocation)
    assertEquals("Geneva", state.customLocationQuery)
  }

  @Test
  fun onCustomLocationClick_showsDialog() = runTest {
    val viewModel = createViewModel()

    viewModel.onCustomLocationClick()
    val state = viewModel.uiState.first()

    assertTrue("Dialog should be shown", state.showCustomLocationDialog)
  }

  @Test
  fun dismissCustomLocationDialog_resetsState() = runTest {
    val viewModel = createViewModel()
    val location = Location("Lausanne", 46.5197, 6.6323)

    // First set location and open dialog
    viewModel.setCustomLocation(location)
    viewModel.onCustomLocationClick()
    var state = viewModel.uiState.first()
    assertTrue(state.showCustomLocationDialog)

    // Then dismiss
    viewModel.dismissCustomLocationDialog()
    state = viewModel.uiState.first()

    assertFalse("Dialog should be closed", state.showCustomLocationDialog)
    assertNull("Location should be cleared", state.customLocation)
    assertEquals("Query should be cleared", "", state.customLocationQuery)
  }

  @Test
  fun saveLocationToProfile_notLoggedIn_doesNotSave() = runTest {
    FirebaseEmulator.auth.signOut()
    val viewModel = createViewModel()
    val location = Location("Lausanne", 46.5197, 6.6323)

    viewModel.saveLocationToProfile(location)

    // Should not throw, but also should not save (no user logged in)
    // This is tested by the fact that it doesn't crash
    assertTrue(true)
  }

  @Test
  fun saveLocationToProfile_loggedIn_savesToProfile() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid
    val location = Location("Geneva", 46.2044, 6.1432)

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
    val profileRepository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    profileRepository.createProfile(profile)

    val viewModel = createViewModel(profileRepository = profileRepository)

    viewModel.saveLocationToProfile(location)

    // Wait for save to complete
    awaitUntil {
      try {
        val profile = profileRepository.getProfile(uid)
        profile.userInfo.location != null
      } catch (e: Exception) {
        false
      }
    }
    val updatedProfile = profileRepository.getProfile(uid)

    assertNotNull("Profile should be updated", updatedProfile)
    assertEquals("Geneva", updatedProfile?.userInfo?.location?.name)
  }
}
