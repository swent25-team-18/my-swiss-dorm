package com.android.mySwissDorm.ui.overview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowseCityScreenFirestoreTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private val profileRepo = ProfileRepositoryProvider.repository
  private val listingsRepo = RentalListingRepositoryProvider.repository
  private val reviewsRepo = ReviewsRepositoryProvider.repository
  private val residenciesRepo = ResidenciesRepositoryProvider.repository
  private lateinit var vm: BrowseCityViewModel

  private lateinit var ownerUid: String
  private lateinit var otherUid: String

  private lateinit var listingLaus1: RentalListing
  private lateinit var listingLaus2: RentalListing
  private lateinit var listingZurich: RentalListing

  override fun createRepositories() {
    runBlocking {
      residenciesRepo.addResidency(vortex)
      residenciesRepo.addResidency(woko)
      residenciesRepo.addResidency(atrium)
    }
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      createRepositories()
      // two users + profiles
      switchToUser(FakeUser.FakeUser1)
      ownerUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = ownerUid))

      switchToUser(FakeUser.FakeUser2)
      otherUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile2.copy(ownerId = otherUid))

      switchToUser(FakeUser.FakeUser1)
    }

    val resLaus =
        Residency(
            name = "Vortex",
            description = "Vortex",
            location = Location("Vortex", 46.52, 6.57),
            city = "Lausanne",
            email = null,
            phone = null,
            website = null)
    val resZurich =
        resLaus.copy(
            city = "Zurich",
            name = "CUG",
            location = Location("CUG", 47.3769, 8.5417)) // Zurich coordinates

    listingLaus1 =
        RentalListing(
            uid = "laus1",
            ownerId = ownerUid,
            postedAt = Timestamp.now(),
            residencyName = resLaus.name,
            title = "Lausanne Studio 1",
            roomType = RoomType.STUDIO,
            pricePerMonth = 1200.0,
            areaInM2 = 20,
            startDate = Timestamp.now(),
            description = "",
            imageUrls = emptyList(),
            status = RentalStatus.POSTED)
    listingLaus2 =
        listingLaus1.copy(
            uid = "laus2", ownerId = otherUid, title = "Lausanne Studio 2", areaInM2 = 22)
    listingZurich =
        listingLaus1.copy(
            uid = "zurich1",
            ownerId = otherUid,
            title = "Zurich Room",
            residencyName = resZurich.name)

    runTest {
      switchToUser(FakeUser.FakeUser1)
      listingsRepo.addRentalListing(listingLaus1)
      reviewsRepo.addReview(reviewVortex1.copy(ownerId = ownerUid))
      reviewsRepo.addReview(reviewVortex2.copy(ownerId = ownerUid))
      switchToUser(FakeUser.FakeUser2)
      listingsRepo.addRentalListing(listingLaus2)
      listingsRepo.addRentalListing(listingZurich)
      reviewsRepo.addReview(reviewWoko1.copy(ownerId = otherUid))
    }

    vm =
        BrowseCityViewModel(
            listingsRepository = listingsRepo,
            reviewsRepository = reviewsRepo,
            residenciesRepository = residenciesRepo)
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  // ——————————————— Tests ———————————————

  @Test
  fun loadsFromFirestore_filtersByCity_displaysOnlyLausanne() = runTest {
    switchToUser(FakeUser.FakeUser1)

    val lausanneLocation = Location("Lausanne", 46.5197, 6.6323)
    compose.setContent {
      BrowseCityScreen(browseCityViewModel = vm, location = lausanneLocation, onSelectListing = {})
    }
    compose.waitUntil(5_000) { vm.uiState.value.listings.items.isNotEmpty() }
    compose.onNodeWithTag(C.BrowseCityTags.LISTING_LIST).assertIsDisplayed()
    compose
        .onNodeWithTag(C.BrowseCityTags.listingCard(listingLaus1.uid))
        .performScrollTo()
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.BrowseCityTags.listingCard(listingLaus2.uid))
        .performScrollTo()
        .assertIsDisplayed()
    compose.onNodeWithTag(C.BrowseCityTags.listingCard(listingZurich.uid)).assertDoesNotExist()
  }

  @Test
  fun listingFromOwnerWhoBlockedUser_isHidden() = runTest {
    // Owner of listingLaus2 (FakeUser2) blocks current user (FakeUser1)
    switchToUser(FakeUser.FakeUser2)
    profileRepo.addBlockedUser(otherUid, ownerUid)

    // Current user tries to browse listings again
    switchToUser(FakeUser.FakeUser1)

    val lausanneLocation = Location("Lausanne", 46.5197, 6.6323)
    compose.setContent {
      BrowseCityScreen(browseCityViewModel = vm, location = lausanneLocation, onSelectListing = {})
    }

    compose.waitUntil(5_000) { vm.uiState.value.listings.items.isNotEmpty() }

    // User should still see their own listing
    compose
        .onNodeWithTag(C.BrowseCityTags.listingCard(listingLaus1.uid))
        .performScrollTo()
        .assertIsDisplayed()

    // Listing owned by blocker must be hidden
    compose.onNodeWithTag(C.BrowseCityTags.listingCard(listingLaus2.uid)).assertDoesNotExist()
  }

  @Test
  fun onlyLausanneResidenciesAreDisplayed() {
    compose.setContent {
      BrowseCityScreen(browseCityViewModel = vm, location = Location("Lausanne", 46.5197, 6.6323))
    }
    compose.waitForIdle()

    compose.onNodeWithTag(C.BrowseCityTags.TAB_REVIEWS).performClick()

    compose.waitUntil(5_000) { vm.uiState.value.residencies.items.isNotEmpty() }

    compose.onNodeWithTag(C.BrowseCityTags.RESIDENCY_LIST).assertIsDisplayed()
    compose
        .onNodeWithTag(C.BrowseCityTags.residencyCard(vortex.name))
        .performScrollTo()
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.BrowseCityTags.residencyCard(atrium.name))
        .performScrollTo()
        .assertIsDisplayed()
    compose.onNodeWithTag(C.BrowseCityTags.residencyCard(woko.name)).assertIsNotDisplayed()
  }

  @Test
  fun noReviewForResidencyShowsNoReviewsYet() {
    compose.setContent {
      BrowseCityScreen(browseCityViewModel = vm, location = Location("Lausanne", 46.5197, 6.6323))
    }

    compose.waitForIdle()

    compose.onNodeWithTag(C.BrowseCityTags.TAB_REVIEWS).performClick()

    compose.waitUntil(5_000) { vm.uiState.value.residencies.items.isNotEmpty() }

    compose.onNodeWithTag(C.BrowseCityTags.RESIDENCY_LIST).assertIsDisplayed()
    compose
        .onNodeWithTag(C.BrowseCityTags.residencyCard(atrium.name))
        .performScrollTo()
        .assertIsDisplayed()

    compose
        .onNodeWithTag(
            C.BrowseCityTags.residencyCardEmptyReview(atrium.name), useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun showsLatestReviewForResidency() {
    compose.setContent {
      BrowseCityScreen(browseCityViewModel = vm, location = Location("Lausanne", 46.5197, 6.6323))
    }

    compose.waitForIdle()

    compose.onNodeWithTag(C.BrowseCityTags.TAB_REVIEWS).performClick()

    compose.waitUntil(5_000) { vm.uiState.value.residencies.items.isNotEmpty() }

    compose.onNodeWithTag(C.BrowseCityTags.RESIDENCY_LIST).assertIsDisplayed()
    compose
        .onNodeWithTag(C.BrowseCityTags.residencyCard(vortex.name))
        .performScrollTo()
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.BrowseCityTags.reviewText(reviewVortex2.uid), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.BrowseCityTags.reviewText(reviewVortex2.uid), useUnmergedTree = true)
        .assertTextEquals(reviewVortex2.reviewText)
  }

  @Test
  fun clickingCard_callsOnSelectListing_withCorrectUid() = runTest {
    val clicked = mutableStateOf<ListingCardUI?>(null)
    switchToUser(FakeUser.FakeUser1)

    val lausanneLocation = Location("Lausanne", 46.5197, 6.6323)
    compose.setContent {
      BrowseCityScreen(location = lausanneLocation, onSelectListing = { clicked.value = it })
    }
    compose.waitUntil(timeoutMillis = 2000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.LISTING_LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose
        .onNodeWithTag(C.BrowseCityTags.listingCard(listingLaus1.uid))
        .performScrollTo()
        .performClick()
    compose.waitUntil(5_000) { clicked.value?.listingUid == listingLaus1.uid }
  }

  @Test
  fun emptyState_showsNoResidenciesYet() {
    compose.setContent {
      BrowseCityScreen(location = Location("Geneva", 46.2044, 6.1432))
    } // no Geneva data
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.BrowseCityTags.TAB_REVIEWS).performClick()

    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.EMPTY, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.BrowseCityTags.EMPTY, useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("No residencies yet.", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun emptyState_showsNoListingsYet() = runTest {
    switchToUser(FakeUser.FakeUser1)

    val genevaLocation = Location("Geneva", 46.2044, 6.1432) // no Geneva data
    compose.setContent { BrowseCityScreen(location = genevaLocation) }
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.EMPTY, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.BrowseCityTags.EMPTY, useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("No listings yet.", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun topBar_updatesWhenCityChanges() = runTest {
    var location by mutableStateOf(Location("Lausanne", 46.5197, 6.6323))

    compose.setContent { BrowseCityScreen(location = location) }
    compose.waitForIdle()

    // initial title
    compose.onNodeWithText("Lausanne").assertIsDisplayed()

    // change city -> title should update
    compose.runOnIdle { location = Location("Zurich", 47.3769, 8.5417) }
    compose.waitForIdle()

    compose.onNodeWithText("Zurich").assertIsDisplayed()
    compose.onNodeWithText("Lausanne").assertDoesNotExist()
  }

  @Test
  fun locationButton_opensCustomLocationDialog() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val lausanneLocation = Location("Lausanne", 46.5197, 6.6323)

    compose.setContent { BrowseCityScreen(location = lausanneLocation) }
    compose.waitForIdle()

    // Click on location button
    compose.onNodeWithTag(C.BrowseCityTags.LOCATION_BUTTON).performClick()
    compose.waitForIdle()

    // Check that dialog appears
    compose.onNodeWithTag(C.CustomLocationDialogTags.DIALOG_TITLE).assertIsDisplayed()
  }

  @Test
  fun locationButton_displaysLocationName() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val lausanneLocation = Location("Lausanne", 46.5197, 6.6323)

    compose.setContent { BrowseCityScreen(location = lausanneLocation) }
    compose.waitForIdle()

    // Check that location name is displayed
    compose.onNodeWithText("Lausanne").assertIsDisplayed()
  }

  @Test
  fun locationButton_savesLocationToProfile() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid
    val initialLocation = Location("Lausanne", 46.5197, 6.6323)

    // Use profile1 with no location and updated ownerId
    profileRepo.createProfile(
        profile1.copy(userInfo = profile1.userInfo.copy(location = null), ownerId = uid))

    compose.setContent { BrowseCityScreen(location = initialLocation, onLocationChange = {}) }
    compose.waitForIdle()

    // Click location button to open dialog
    compose.onNodeWithTag(C.BrowseCityTags.LOCATION_BUTTON).performClick()
    compose.waitForIdle()

    // Wait for dialog to appear
    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.CustomLocationDialogTags.DIALOG_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify the dialog opens and text field is displayed
    compose.onNodeWithTag(C.CustomLocationDialogTags.LOCATION_TEXT_FIELD).assertIsDisplayed()
  }

  @Test
  fun errorState_displaysError() = runTest {
    class ThrowingRepo : RentalListingRepository {
      override fun getNewUid(): String = "x"

      override suspend fun getAllRentalListings(): List<RentalListing> {
        delay(50)
        error("Boom")
      }

      override suspend fun getAllRentalListingsByUser(userId: String): List<RentalListing> {
        error("unused")
      }

      override suspend fun getAllRentalListingsByLocation(
          location: Location,
          radius: Double
      ): List<RentalListing> {
        error("unused")
      }

      override suspend fun getRentalListing(rentalPostId: String): RentalListing = error("unused")

      override suspend fun addRentalListing(rentalPost: RentalListing) {}

      override suspend fun editRentalListing(rentalPostId: String, newValue: RentalListing) {}

      override suspend fun deleteRentalListing(rentalPostId: String) {}
    }
    // Create mock LocationRepository and ProfileRepository
    val mockLocationRepository =
        object : LocationRepository {
          override suspend fun search(query: String): List<Location> = emptyList()
        }
    val vm =
        BrowseCityViewModel(
            listingsRepository = ThrowingRepo(),
            locationRepository = mockLocationRepository,
            profileRepository = ProfileRepositoryProvider.repository,
            auth = FirebaseEmulator.auth)

    val lausanneLocation = Location("Lausanne", 46.5197, 6.6323)
    compose.setContent { BrowseCityScreen(location = lausanneLocation, browseCityViewModel = vm) }

    // We should eventually see the error label
    compose.waitUntil(timeoutMillis = 4_000) {
      compose.onAllNodesWithTag(C.BrowseCityTags.ERROR).fetchSemanticsNodes().isNotEmpty()
    }
    compose
        .onNodeWithTag(C.BrowseCityTags.ERROR)
        .assertIsDisplayed()
        .assertTextContains("Boom", substring = true)
  }
}
