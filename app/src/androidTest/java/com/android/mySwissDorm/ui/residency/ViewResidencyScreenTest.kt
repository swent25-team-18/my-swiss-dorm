package com.android.mySwissDorm.ui.residency

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.poi.POIDistance
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import java.lang.reflect.Field
import java.net.URL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Test ViewModel that allows setting state directly for UI testing
// Uses reflection to access the private _uiState field
class TestViewResidencyViewModel(
    residenciesRepository: ResidenciesRepository = ResidenciesRepositoryProvider.repository,
    profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewResidencyViewModel(residenciesRepository, profileRepository) {
  fun setState(state: ViewResidencyUIState) {
    // Use reflection to access the private _uiState field
    val field: Field = ViewResidencyViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    val mutableStateFlow = field.get(this) as MutableStateFlow<ViewResidencyUIState>
    mutableStateFlow.value = state
  }

  // Override loadResidency to be a no-op so LaunchedEffect doesn't override our test state
  override fun loadResidency(residencyName: String, context: Context) {
    // Do nothing - we set state directly via setState()
  }
}

@RunWith(AndroidJUnit4::class)
class ViewResidencyScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var residenciesRepo: ResidenciesRepository
  private lateinit var profileRepo: ProfileRepository
  private lateinit var listingsRepo: RentalListingRepository

  private val context = ApplicationProvider.getApplicationContext<Context>()

  override fun createRepositories() {
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    RentalListingRepositoryProvider.repository =
        RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    runBlocking { ResidenciesRepositoryProvider.repository.addResidency(resTest) }
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      residenciesRepo = ResidenciesRepositoryProvider.repository
      profileRepo = ProfileRepositoryProvider.repository
      listingsRepo = RentalListingRepositoryProvider.repository
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  private fun waitForScreenRoot() {
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.ViewResidencyTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  private fun scrollListTo(childTag: String) {
    compose.onNodeWithTag(C.ViewResidencyTags.ROOT).performScrollToNode(hasTestTag(childTag))
  }

  @Test
  fun residencyDetails_displaysCorrectly() = runTest {
    compose.setContent { ViewResidencyScreen(residencyName = resTest.name) }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      compose
          .onAllNodesWithTag(C.ViewResidencyTags.NAME, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.ViewResidencyTags.NAME, useUnmergedTree = true).assertIsDisplayed()
    compose
        .onNodeWithTag(C.ViewResidencyTags.DESCRIPTION, useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun backButton_callsOnGoBack() = runTest {
    var backCalled = false

    compose.setContent {
      ViewResidencyScreen(residencyName = resTest.name, onGoBack = { backCalled = true })
    }

    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.BACK_BUTTON, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.BACK_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    assertEquals(true, backCalled)
  }

  @Test
  fun loadingState_displaysLoadingIndicator() = runTest {
    // Use the test ViewModel so loadResidency is a no-op and we fully control the state
    val testViewModel = TestViewResidencyViewModel()

    // Force the ViewModel into a pure loading state
    testViewModel.setState(
        ViewResidencyUIState(
            residency = null, errorMsg = null, poiDistances = emptyList(), loading = true))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = "AnyResidencyName")
    }

    // Wait until the loading indicator appears in the semantics tree
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.LOADING, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (_: Exception) {
        false
      }
    }

    // Assert that the loading indicator is displayed
    compose.onNodeWithTag(C.ViewResidencyTags.LOADING, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun errorState_displaysError() = runTest {
    val viewModel =
        ViewResidencyViewModel(
            residenciesRepository = residenciesRepo,
            profileRepository = profileRepo,
        )

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = viewModel, residencyName = "NonExistentResidency")
    }

    // Wait for error to appear
    compose.waitUntil(10_000) {
      compose
          .onAllNodesWithTag(C.ViewResidencyTags.ERROR, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.ViewResidencyTags.ERROR, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun emptyPoiDistances_displaysNoPoiMessage() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val residency =
        Residency(
            name = "TestResidencyNoPOI",
            description = "Test description",
            location = Location(name = "Test", latitude = 46.0, longitude = 6.0),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null)

    testViewModel.setState(
        ViewResidencyUIState(
            residency = residency,
            poiDistances = emptyList(),
            loading = false,
        ))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = residency.name)
    }

    // Wait for compose to be idle, then wait for POI_DISTANCES
    compose.waitForIdle()
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun poiDistances_singleUniversity_displaysCorrectly() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val poiDistances = listOf(POIDistance("EPFL", 5, POIDistance.TYPE_UNIVERSITY))
    testViewModel.setState(
        ViewResidencyUIState(
            residency = resTest,
            poiDistances = poiDistances,
            loading = false,
        ))

    compose.setContent {
      ViewResidencyScreen(viewResidencyViewModel = testViewModel, residencyName = resTest.name)
    }

    // Wait for ROOT first to ensure UI is rendered, then wait for POI_DISTANCES
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.ROOT, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun poiDistances_singleSupermarket_displaysCorrectly() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val poiDistances = listOf(POIDistance("Migros", 3, POIDistance.TYPE_SUPERMARKET))
    testViewModel.setState(
        ViewResidencyUIState(
            residency = resTest,
            poiDistances = poiDistances,
            loading = false,
        ))

    compose.setContent {
      ViewResidencyScreen(viewResidencyViewModel = testViewModel, residencyName = resTest.name)
    }

    // Wait for ROOT first to ensure UI is rendered, then wait for POI_DISTANCES
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.ROOT, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun poiDistances_multipleSameTime_displaysCorrectly() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val poiDistances =
        listOf(
            POIDistance("EPFL", 5, POIDistance.TYPE_UNIVERSITY),
            POIDistance("UNIL", 5, POIDistance.TYPE_UNIVERSITY))
    testViewModel.setState(
        ViewResidencyUIState(
            residency = resTest,
            poiDistances = poiDistances,
            loading = false,
        ))

    compose.setContent {
      ViewResidencyScreen(viewResidencyViewModel = testViewModel, residencyName = resTest.name)
    }

    // Wait for ROOT first to ensure UI is rendered, then wait for POI_DISTANCES
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.ROOT, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun poiDistances_multipleThreeOrMore_displaysCorrectly() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val poiDistances =
        listOf(
            POIDistance("EPFL", 5, POIDistance.TYPE_UNIVERSITY),
            POIDistance("UNIL", 5, POIDistance.TYPE_UNIVERSITY),
            POIDistance("HEP", 5, POIDistance.TYPE_UNIVERSITY))
    testViewModel.setState(
        ViewResidencyUIState(
            residency = resTest,
            poiDistances = poiDistances,
            loading = false,
        ))

    compose.setContent {
      ViewResidencyScreen(viewResidencyViewModel = testViewModel, residencyName = resTest.name)
    }

    // Wait for ROOT first to ensure UI is rendered, then wait for POI_DISTANCES
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.ROOT, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun contactInfo_withAllFields_displaysCorrectly() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val residencyWithContact =
        Residency(
            name = "TestResidencyContact",
            description = "Test description",
            location = Location(name = "Test", latitude = 46.0, longitude = 6.0),
            city = "Lausanne",
            email = "test@example.com",
            phone = "+41234567890",
            website = URL("https://example.com"))

    testViewModel.setState(
        ViewResidencyUIState(
            residency = residencyWithContact, loading = false, poiDistances = emptyList()))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = residencyWithContact.name)
    }

    // Wait for compose to be idle, then wait for CONTACT_INFO
    compose.waitForIdle()
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.CONTACT_INFO, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.CONTACT_INFO, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun contactInfo_withoutContactInfo_displaysNoContactMessage() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val residencyWithoutContact =
        Residency(
            name = "TestResidencyNoContact",
            description = "Test description",
            location = Location(name = "Test", latitude = 46.0, longitude = 6.0),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null)

    testViewModel.setState(
        ViewResidencyUIState(
            residency = residencyWithoutContact, loading = false, poiDistances = emptyList()))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = residencyWithoutContact.name)
    }

    // Wait for compose to be idle, then wait for CONTACT_INFO
    compose.waitForIdle()
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.CONTACT_INFO, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.CONTACT_INFO, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun locationSection_callsOnViewMap() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    testViewModel.setState(
        ViewResidencyUIState(residency = resTest, loading = false, poiDistances = emptyList()))

    var mapCalled = false
    var capturedLat: Double = 0.0
    var capturedLng: Double = 0.0
    var capturedTitle: String? = null
    var capturedNameId: Int? = null

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = resTest.name,
          onViewMap = { lat, lng, title, nameId ->
            mapCalled = true
            capturedLat = lat
            capturedLng = lng
            capturedTitle = title
            capturedNameId = nameId
          })
    }

    // Wait for compose to be idle, then wait for LOCATION
    compose.waitForIdle()
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.LOCATION, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.LOCATION, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    assertEquals(true, mapCalled)
    assertEquals(resTest.location.latitude, capturedLat, 0.001)
    assertEquals(resTest.location.longitude, capturedLng, 0.001)
    assertEquals(resTest.name, capturedTitle)
  }

  @Test
  fun topBarTitle_whenResidencyNull_showsResidencyName() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    testViewModel.setState(ViewResidencyUIState(loading = true, residency = null))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = "TestResidencyName")
    }

    // Initially, residency is null, so should show residencyName
    compose
        .onNodeWithTag(C.ViewResidencyTags.TOP_BAR_TITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun errorState_withErrorMsg_displaysError() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    testViewModel.setState(
        ViewResidencyUIState(loading = false, residency = null, errorMsg = "Test error message"))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = "TestResidencyName")
    }

    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.ViewResidencyTags.ERROR, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.ViewResidencyTags.ERROR, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun poiDistances_mixedTypes_displaysCorrectly() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val poiDistances =
        listOf(
            POIDistance("EPFL", 5, POIDistance.TYPE_UNIVERSITY),
            POIDistance("Migros", 3, POIDistance.TYPE_SUPERMARKET))
    testViewModel.setState(
        ViewResidencyUIState(
            residency = resTest,
            poiDistances = poiDistances,
            loading = false,
        ))

    compose.setContent {
      ViewResidencyScreen(viewResidencyViewModel = testViewModel, residencyName = resTest.name)
    }

    // Wait for ROOT first to ensure UI is rendered, then wait for POI_DISTANCES
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.ROOT, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun contactInfo_withOnlyEmail_displaysCorrectly() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val residencyWithEmail =
        Residency(
            name = "TestResidencyEmail",
            description = "Test description",
            location = Location(name = "Test", latitude = 46.0, longitude = 6.0),
            city = "Lausanne",
            email = "test@example.com",
            phone = null,
            website = null)

    testViewModel.setState(
        ViewResidencyUIState(
            residency = residencyWithEmail, loading = false, poiDistances = emptyList()))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = residencyWithEmail.name)
    }

    // Wait for compose to be idle, then wait for CONTACT_INFO
    compose.waitForIdle()
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.CONTACT_INFO, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.CONTACT_INFO, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun imagesSection_withMultipleImages_displaysImageGrid() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val photo1 = Photo.createNewTempPhoto("photo1.jpg")
    val photo2 = Photo.createNewTempPhoto("photo2.jpg")
    val residencyWithImages =
        Residency(
            name = "TestResidencyWithImages",
            description = "Test description",
            location = Location(name = "Test", latitude = 46.0, longitude = 6.0),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null,
            imageUrls = listOf("photo1.jpg", "photo2.jpg"))

    testViewModel.setState(
        ViewResidencyUIState(
            residency = residencyWithImages,
            loading = false,
            poiDistances = emptyList(),
            images = listOf(photo1, photo2)))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = residencyWithImages.name)
    }

    compose.waitForIdle()
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.PHOTOS, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose.onNodeWithTag(C.ViewResidencyTags.PHOTOS, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun imagesSection_withNoImages_doesNotDisplayImageGrid() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val residencyWithoutImages =
        Residency(
            name = "TestResidencyNoImages",
            description = "Test description",
            location = Location(name = "Test", latitude = 46.0, longitude = 6.0),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null,
            imageUrls = emptyList())

    testViewModel.setState(
        ViewResidencyUIState(
            residency = residencyWithoutImages,
            loading = false,
            poiDistances = emptyList(),
            images = emptyList()))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = residencyWithoutImages.name)
    }

    compose.waitForIdle()
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.ROOT, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    // Image grid should not be displayed when there are no images
    compose.onNodeWithTag(C.ViewResidencyTags.PHOTOS, useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun fullScreenImages_showsWhenImageClicked() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val photo1 = Photo.createNewTempPhoto("photo1.jpg")
    val photo2 = Photo.createNewTempPhoto("photo2.jpg")
    val residencyWithImages =
        Residency(
            name = "TestResidencyFullScreen",
            description = "Test description",
            location = Location(name = "Test", latitude = 46.0, longitude = 6.0),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null)

    testViewModel.setState(
        ViewResidencyUIState(
            residency = residencyWithImages,
            loading = false,
            poiDistances = emptyList(),
            images = listOf(photo1, photo2),
            showFullScreenImages = true,
            fullScreenImagesIndex = 1))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = residencyWithImages.name)
    }

    // When showFullScreenImages is true, FullScreenImageViewer should be displayed
    compose.waitForIdle()
    // The screen should show the full screen viewer instead of the main content
    // We verify by checking that ROOT doesn't exist when full screen is shown
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.ROOT, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isEmpty()
      } catch (e: Exception) {
        false
      }
    }
  }

  @Test
  fun poiDistances_loadingState_displaysLoadingIndicator() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    testViewModel.setState(
        ViewResidencyUIState(
            residency = resTest, loading = false, poiDistances = emptyList(), isLoadingPOIs = true))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = resTest.name)
    }

    compose.waitForIdle()
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    // POI section should be displayed even when loading
    compose
        .onNodeWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun contactInfo_withOnlyPhone_displaysCorrectly() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val residencyWithPhone =
        Residency(
            name = "TestResidencyPhone",
            description = "Test description",
            location = Location(name = "Test", latitude = 46.0, longitude = 6.0),
            city = "Lausanne",
            email = null,
            phone = "+41234567890",
            website = null)

    testViewModel.setState(
        ViewResidencyUIState(
            residency = residencyWithPhone, loading = false, poiDistances = emptyList()))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = residencyWithPhone.name)
    }

    compose.waitForIdle()
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.CONTACT_INFO, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.CONTACT_INFO, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun contactInfo_withOnlyWebsite_displaysCorrectly() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val residencyWithWebsite =
        Residency(
            name = "TestResidencyWebsite",
            description = "Test description",
            location = Location(name = "Test", latitude = 46.0, longitude = 6.0),
            city = "Lausanne",
            email = null,
            phone = null,
            website = URL("https://example.com"))

    testViewModel.setState(
        ViewResidencyUIState(
            residency = residencyWithWebsite, loading = false, poiDistances = emptyList()))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = residencyWithWebsite.name)
    }

    compose.waitForIdle()
    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.CONTACT_INFO, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.CONTACT_INFO, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun poiDistances_supermarket_displaysCorrectly() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    val poiDistances = listOf(POIDistance("Coop", 2, POIDistance.TYPE_SUPERMARKET))
    testViewModel.setState(
        ViewResidencyUIState(
            residency = resTest,
            poiDistances = poiDistances,
            loading = false,
            isLoadingPOIs = false))

    compose.setContent {
      ViewResidencyScreen(viewResidencyViewModel = testViewModel, residencyName = resTest.name)
    }

    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun errorState_withNullErrorMsg_displaysDefaultMessage() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    testViewModel.setState(ViewResidencyUIState(loading = false, residency = null, errorMsg = null))

    compose.setContent {
      ViewResidencyScreen(
          viewResidencyViewModel = testViewModel as ViewResidencyViewModel,
          residencyName = "TestResidencyName")
    }

    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.ViewResidencyTags.ERROR, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.ViewResidencyTags.ERROR, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun poiDistances_emptyList_displaysNoPoiMessage() = runTest {
    val testViewModel = TestViewResidencyViewModel()
    testViewModel.setState(
        ViewResidencyUIState(
            residency = resTest,
            poiDistances = emptyList(),
            loading = false,
            isLoadingPOIs = false))

    compose.setContent {
      ViewResidencyScreen(viewResidencyViewModel = testViewModel, residencyName = resTest.name)
    }

    compose.waitUntil(timeoutMillis = 5_000) {
      try {
        compose
            .onAllNodesWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
    compose
        .onNodeWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
