package com.android.mySwissDorm.ui.homepage

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.city.CitiesRepository
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomePageViewModelTest : FirestoreTest() {
  private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

  override fun createRepositories() {
    /* none */
  }

  @Before override fun setUp() = runTest { super.setUp() }

  @Test
  fun loadCities_imageLoadingError_continuesWithCities() = runTest {
    val cities =
        listOf(
            City(
                name = "Lausanne",
                description = "Test city",
                location = Location("Lausanne", 46.5197, 6.6323),
                imageId = "lausanne.png"),
            City(
                name = "Geneva",
                description = "Test city",
                location = Location("Geneva", 46.2044, 6.1432),
                imageId = "geneva.png"))

    val mockCitiesRepo =
        object : CitiesRepository {
          override suspend fun getAllCities(): List<City> = cities

          override suspend fun getCity(name: String): City = cities.first()

          override suspend fun addCity(city: City) {}
        }

    // Photo repository that throws exception for one city
    val mockPhotoRepo =
        object : PhotoRepositoryCloud() {
          override suspend fun retrievePhoto(uid: String): Photo {
            if (uid == "lausanne.png") {
              throw Exception("Image loading error")
            }
            // Return a valid Photo with a proper URI
            return Photo(image = android.net.Uri.parse("file:///test/${uid}"), fileName = uid)
          }
        }

    val mockLocationRepo =
        object : LocationRepository {
          override suspend fun search(query: String): List<Location> = emptyList()

          override suspend fun reverseSearch(latitude: Double, longitude: Double): Location? = null
        }

    val mockProfileRepo =
        object : ProfileRepository {
          override suspend fun createProfile(
              profile: com.android.mySwissDorm.model.profile.Profile
          ) {}

          override suspend fun getProfile(
              ownerId: String
          ): com.android.mySwissDorm.model.profile.Profile {
            throw Exception("Not implemented")
          }

          override suspend fun getAllProfile():
              List<com.android.mySwissDorm.model.profile.Profile> = emptyList()

          override suspend fun editProfile(
              profile: com.android.mySwissDorm.model.profile.Profile
          ) {}

          override suspend fun deleteProfile(ownerId: String) {}

          override suspend fun getBlockedUserIds(ownerId: String): List<String> = emptyList()

          override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> =
              emptyMap()

          override suspend fun addBlockedUser(ownerId: String, targetUid: String) {}

          override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {}

          override suspend fun getBookmarkedListingIds(ownerId: String): List<String> = emptyList()

          override suspend fun addBookmark(ownerId: String, listingId: String) {}

          override suspend fun removeBookmark(ownerId: String, listingId: String) {}
        }

    val viewModel =
        HomePageViewModel(
            citiesRepository = mockCitiesRepo,
            locationRepository = mockLocationRepo,
            profileRepository = mockProfileRepo,
            auth = FirebaseEmulator.auth,
            photoRepositoryCloud = mockPhotoRepo)

    // Wait for cities to load
    var attempts = 0
    while (viewModel.uiState.value.cities.isEmpty() && attempts < 50) {
      kotlinx.coroutines.delay(100)
      attempts++
    }

    // Wait for image loading to complete (images load separately after cities)
    // The image loading happens after cities are set, so we wait for the image map to be populated
    attempts = 0
    while (attempts < 100) {
      kotlinx.coroutines.delay(100)
      // Image loading should complete - we expect 1 image (Geneva) since Lausanne throws exception
      if (viewModel.uiState.value.cityImageMap.size == 1) {
        break
      }
      // If we've waited a long time and still no images, something might be wrong, but continue
      attempts++
    }

    // Cities should be loaded even if image loading fails
    assertEquals("Cities should be loaded", 2, viewModel.uiState.value.cities.size)
    // Image map should only contain the successfully loaded image (Geneva, not Lausanne)
    assertEquals("Image map should contain one image", 1, viewModel.uiState.value.cityImageMap.size)
    assertNotNull("Geneva image should be loaded", viewModel.uiState.value.cityImageMap[cities[1]])
    assertNull(
        "Lausanne image should not be loaded due to error",
        viewModel.uiState.value.cityImageMap[cities[0]])
  }

  @Test
  fun saveLocationToProfile_userNotLoggedIn_doesNotSave() = runTest {
    FirebaseEmulator.auth.signOut()

    val mockCitiesRepo =
        object : CitiesRepository {
          override suspend fun getAllCities(): List<City> = emptyList()

          override suspend fun getCity(name: String): City {
            throw Exception("Not implemented")
          }

          override suspend fun addCity(city: City) {}
        }

    val mockLocationRepo =
        object : LocationRepository {
          override suspend fun search(query: String): List<Location> = emptyList()

          override suspend fun reverseSearch(latitude: Double, longitude: Double): Location? = null
        }

    val mockProfileRepo =
        object : ProfileRepository {
          override suspend fun createProfile(
              profile: com.android.mySwissDorm.model.profile.Profile
          ) {}

          override suspend fun getProfile(
              ownerId: String
          ): com.android.mySwissDorm.model.profile.Profile {
            throw Exception("Not implemented")
          }

          override suspend fun getAllProfile():
              List<com.android.mySwissDorm.model.profile.Profile> = emptyList()

          override suspend fun editProfile(
              profile: com.android.mySwissDorm.model.profile.Profile
          ) {}

          override suspend fun deleteProfile(ownerId: String) {}

          override suspend fun getBlockedUserIds(ownerId: String): List<String> = emptyList()

          override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> =
              emptyMap()

          override suspend fun addBlockedUser(ownerId: String, targetUid: String) {}

          override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {}

          override suspend fun getBookmarkedListingIds(ownerId: String): List<String> = emptyList()

          override suspend fun addBookmark(ownerId: String, listingId: String) {}

          override suspend fun removeBookmark(ownerId: String, listingId: String) {}
        }

    val viewModel =
        HomePageViewModel(
            citiesRepository = mockCitiesRepo,
            locationRepository = mockLocationRepo,
            profileRepository = mockProfileRepo,
            auth = FirebaseEmulator.auth)

    val testLocation = Location("Lausanne", 46.5197, 6.6323)
    viewModel.saveLocationToProfile(testLocation)

    // Wait a bit to ensure coroutine completes
    kotlinx.coroutines.delay(500)

    // Should not crash, but location should not be saved (user not logged in)
    // The function returns early, so no profile update should occur
    assertEquals("Cities should still be empty", 0, viewModel.uiState.value.cities.size)
  }

  @Test
  fun saveLocationToProfile_errorSaving_handlesGracefully() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    val mockCitiesRepo =
        object : CitiesRepository {
          override suspend fun getAllCities(): List<City> = emptyList()

          override suspend fun getCity(name: String): City {
            throw Exception("Not implemented")
          }

          override suspend fun addCity(city: City) {}
        }

    val mockLocationRepo =
        object : LocationRepository {
          override suspend fun search(query: String): List<Location> = emptyList()

          override suspend fun reverseSearch(latitude: Double, longitude: Double): Location? = null
        }

    // Profile repository that throws exception
    val mockProfileRepo =
        object : ProfileRepository {
          override suspend fun createProfile(
              profile: com.android.mySwissDorm.model.profile.Profile
          ) {}

          override suspend fun getProfile(
              ownerId: String
          ): com.android.mySwissDorm.model.profile.Profile {
            throw Exception("Error getting profile")
          }

          override suspend fun getAllProfile():
              List<com.android.mySwissDorm.model.profile.Profile> = emptyList()

          override suspend fun editProfile(
              profile: com.android.mySwissDorm.model.profile.Profile
          ) {}

          override suspend fun deleteProfile(ownerId: String) {}

          override suspend fun getBlockedUserIds(ownerId: String): List<String> = emptyList()

          override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> =
              emptyMap()

          override suspend fun addBlockedUser(ownerId: String, targetUid: String) {}

          override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {}

          override suspend fun getBookmarkedListingIds(ownerId: String): List<String> = emptyList()

          override suspend fun addBookmark(ownerId: String, listingId: String) {}

          override suspend fun removeBookmark(ownerId: String, listingId: String) {}
        }

    val viewModel =
        HomePageViewModel(
            citiesRepository = mockCitiesRepo,
            locationRepository = mockLocationRepo,
            profileRepository = mockProfileRepo,
            auth = FirebaseEmulator.auth)

    val testLocation = Location("Lausanne", 46.5197, 6.6323)
    viewModel.saveLocationToProfile(testLocation)

    // Wait for coroutine to complete
    kotlinx.coroutines.delay(500)

    // Should not crash, error should be handled gracefully
    assertEquals("Cities should still be empty", 0, viewModel.uiState.value.cities.size)
  }
}
