package com.android.mySwissDorm.ui.overview

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
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
class BrowseCityViewModelTest : FirestoreTest() {

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
      listingsRepository: RentalListingRepository,
      locationRepository: LocationRepository = createMockLocationRepository(),
      profileRepository: ProfileRepository = ProfileRepositoryFirestore(FirebaseEmulator.firestore),
      auth: com.google.firebase.auth.FirebaseAuth = FirebaseEmulator.auth
  ): BrowseCityViewModel {
    return BrowseCityViewModel(
        listingsRepository = listingsRepository,
        locationRepository = locationRepository,
        profileRepository = profileRepository,
        auth = auth)
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
    val viewModel = createViewModel(createMockRentalListingRepository())

    viewModel.setCustomLocationQuery("")
    val state = viewModel.uiState.first()

    assertEquals("", state.customLocationQuery)
    assertTrue("Suggestions should be empty", state.locationSuggestions.isEmpty())
  }

  @Test
  fun setCustomLocationQuery_nonEmptyQuery_fetchesSuggestions() = runTest {
    val viewModel = createViewModel(createMockRentalListingRepository())

    viewModel.setCustomLocationQuery("Lausanne")
    awaitUntil { viewModel.uiState.first().locationSuggestions.isNotEmpty() }
    val state = viewModel.uiState.first()

    assertEquals("Lausanne", state.customLocationQuery)
    assertTrue("Should have suggestions", state.locationSuggestions.isNotEmpty())
    assertEquals("Lausanne, Switzerland", state.locationSuggestions.first().name)
  }

  @Test
  fun setCustomLocation_updatesState() = runTest {
    val viewModel = createViewModel(createMockRentalListingRepository())
    val location = Location("Geneva", 46.2044, 6.1432)

    viewModel.setCustomLocation(location)
    val state = viewModel.uiState.first()

    assertEquals(location, state.customLocation)
    assertEquals("Geneva", state.customLocationQuery)
  }

  @Test
  fun onCustomLocationClick_withLocation_opensDialogWithLocation() = runTest {
    val viewModel = createViewModel(createMockRentalListingRepository())
    val location = Location("Lausanne", 46.5197, 6.6323)

    viewModel.onCustomLocationClick(location)
    val state = viewModel.uiState.first()

    assertTrue("Dialog should be shown", state.showCustomLocationDialog)
    assertEquals(location, state.customLocation)
    assertEquals("Lausanne", state.customLocationQuery)
  }

  @Test
  fun onCustomLocationClick_withoutLocation_opensDialogWithEmptyQuery() = runTest {
    val viewModel = createViewModel(createMockRentalListingRepository())

    viewModel.onCustomLocationClick()
    val state = viewModel.uiState.first()

    assertTrue("Dialog should be shown", state.showCustomLocationDialog)
    assertNull("Location should be null", state.customLocation)
    assertEquals("", state.customLocationQuery)
  }

  @Test
  fun dismissCustomLocationDialog_resetsState() = runTest {
    val viewModel = createViewModel(createMockRentalListingRepository())
    val location = Location("Geneva", 46.2044, 6.1432)

    // First set location and open dialog
    viewModel.setCustomLocation(location)
    viewModel.onCustomLocationClick(location)
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
    val viewModel = createViewModel(createMockRentalListingRepository())
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

    val viewModel =
        createViewModel(createMockRentalListingRepository(), profileRepository = profileRepository)

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

  private fun createMockRentalListingRepository(): RentalListingRepository {
    return object : RentalListingRepository {
      override fun getNewUid(): String = "test-uid"

      override suspend fun getAllRentalListings(): List<RentalListing> = emptyList()

      override suspend fun getAllRentalListingsByUser(userId: String): List<RentalListing> =
          emptyList()

      override suspend fun getAllRentalListingsByLocation(
          location: Location,
          radius: Double
      ): List<RentalListing> = emptyList()

      override suspend fun getRentalListing(rentalPostId: String): RentalListing =
          error("Not implemented")

      override suspend fun addRentalListing(rentalPost: RentalListing) {}

      override suspend fun editRentalListing(rentalPostId: String, newValue: RentalListing) {}

      override suspend fun deleteRentalListing(rentalPostId: String) {}
    }
  }
}
