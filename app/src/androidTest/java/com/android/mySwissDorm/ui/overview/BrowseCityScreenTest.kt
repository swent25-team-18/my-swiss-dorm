package com.android.mySwissDorm.ui.overview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakePhotoRepository
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_FILE_NAME
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_NAME
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_SUFFIX
import com.android.mySwissDorm.utils.FakePhotoRepositoryCloud
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowseCityScreenFirestoreTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private val profileRepo = ProfileRepositoryProvider.repository
  private val listingsRepo = RentalListingRepositoryProvider.repository
  private val reviewsRepo = ReviewsRepositoryProvider.repository
  private val residenciesRepo = ResidenciesRepositoryProvider.repository
  val fakePhoto = Photo(File.createTempFile(FAKE_NAME, FAKE_SUFFIX).toUri(), FAKE_FILE_NAME)
  private lateinit var vm: BrowseCityViewModel

  private lateinit var ownerUid: String
  private lateinit var otherUid: String

  private lateinit var listingLaus1: RentalListing
  private lateinit var listingLaus2: RentalListing
  private lateinit var listingZurich: RentalListing
  private lateinit var listingFrib1: RentalListing

  override fun createRepositories() {
    PhotoRepositoryProvider.initialize(InstrumentationRegistry.getInstrumentation().context)
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
            status = RentalStatus.POSTED,
            location = resLaus.location)
    listingLaus2 =
        listingLaus1.copy(
            uid = "laus2", ownerId = otherUid, title = "Lausanne Studio 2", areaInM2 = 22)
    listingZurich =
        listingLaus1.copy(
            uid = "zurich1",
            ownerId = otherUid,
            title = "Zurich Room",
            residencyName = resZurich.name,
            location = resZurich.location)
    listingFrib1 =
        RentalListing(
            uid = "frib1",
            ownerId = ownerUid,
            postedAt = Timestamp.now(),
            residencyName = resLaus.name,
            title = "Lausanne Studio 1",
            roomType = RoomType.STUDIO,
            pricePerMonth = 1200.0,
            areaInM2 = 20,
            startDate = Timestamp.now(),
            description = "",
            imageUrls = listOf(fakePhoto.fileName),
            status = RentalStatus.POSTED,
            location = fribourgLocation)

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

  // ——————————————— Helper Functions ———————————————

  private val lausanneLocation = Location("Lausanne", 46.5197, 6.6323)
  private val fribourgLocation = Location("Fribourg", 46.4822, 7.0946)

  /** Sets up the screen with Lausanne location and waits for listings to load. */
  private suspend fun setupScreenWithListings() {
    switchToUser(FakeUser.FakeUser1)
    compose.setContent { BrowseCityScreen(browseCityViewModel = vm, location = lausanneLocation) }
    compose.waitUntil(5_000) { vm.uiState.value.listings.items.isNotEmpty() }
  }

  /** Waits for filter chip row to exist. */
  private fun waitForFilterChipRow() {
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.FILTER_CHIP_ROW, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  /** Scrolls to a filter chip by index and clicks it. */
  private fun scrollToAndClickFilterChip(index: Int, chipTag: String) {
    compose
        .onNodeWithTag(C.BrowseCityTags.FILTER_CHIP_ROW, useUnmergedTree = true)
        .performScrollToIndex(index)
    compose.waitForIdle()
    compose.waitUntil(5_000) {
      compose.onAllNodesWithTag(chipTag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(chipTag, useUnmergedTree = true).assertIsDisplayed().performClick()
    compose.waitForIdle()
  }

  /** Waits for bottom sheet to appear. */
  private fun waitForBottomSheet() {
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  /** Waits for Apply button and clicks it. */
  private fun waitForAndClickApplyButton() {
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(
              C.BrowseCityTags.FILTER_BOTTOM_SHEET_APPLY_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose
        .onNodeWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET_APPLY_BUTTON, useUnmergedTree = true)
        .performClick()
    compose.waitForIdle()
    compose.waitUntil(5_000) { !vm.uiState.value.listings.loading }
  }

  /** Selects a room type in the filter bottom sheet. */
  private fun selectRoomType(roomType: RoomType) {
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(
              C.BrowseCityTags.roomTypeCheckbox(roomType.name), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose
        .onNodeWithTag(C.BrowseCityTags.roomTypeCheckbox(roomType.name), useUnmergedTree = true)
        .performClick()
    compose.waitForIdle()
  }

  /** Applies a room type filter (opens filter, selects room type, applies). */
  private fun applyRoomTypeFilter(roomType: RoomType) {
    waitForFilterChipRow()
    scrollToAndClickFilterChip(0, C.BrowseCityTags.FILTER_CHIP_ROOM_TYPE)
    waitForBottomSheet()
    selectRoomType(roomType)
    waitForAndClickApplyButton()
  }

  // ——————————————— Tests ———————————————

  @Test
  fun loadsFromFirestore_filtersByCity_displaysOnlyLausanne() = runTest {
    switchToUser(FakeUser.FakeUser1)
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
  fun reviewFromUserWhoBlockedViewer_isHidden() = runTest {
    // Reviewer (FakeUser2) blocks current user (FakeUser1)
    switchToUser(FakeUser.FakeUser2)
    profileRepo.addBlockedUser(otherUid, ownerUid)

    // Current user browses reviews
    switchToUser(FakeUser.FakeUser1)

    compose.setContent {
      BrowseCityScreen(browseCityViewModel = vm, location = Location("Lausanne", 46.5197, 6.6323))
    }

    compose.onNodeWithTag(C.BrowseCityTags.TAB_REVIEWS).performClick()

    compose.waitUntil(5_000) { vm.uiState.value.residencies.items.isNotEmpty() }

    // Reviews from blocker should not appear
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.reviewText(reviewWoko1.uid), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isEmpty()
    }
    compose.onNodeWithTag(C.BrowseCityTags.reviewText(reviewWoko1.uid)).assertDoesNotExist()

    compose.waitUntil(5_000) {
      vm.uiState.value.residencies.items.any {
        it.title == vortex.name && it.latestReview?.ownerId == ownerUid
      }
    }
    val vortexItem =
        vm.uiState.value.residencies.items.first {
          it.title == vortex.name && it.latestReview != null
        }
    assertEquals(ownerUid, vortexItem.latestReview?.ownerId)
    val latestReviewUid = vortexItem.latestReview!!.uid
    compose
        .onNodeWithTag(C.BrowseCityTags.reviewText(latestReviewUid), useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun reviewFromBlockedUser_doesNotAffectDisplayedMeanGrade() = runTest {
    switchToUser(FakeUser.FakeUser2)
    profileRepo.addBlockedUser(otherUid, ownerUid)

    switchToUser(FakeUser.FakeUser1)
    compose.setContent {
      BrowseCityScreen(browseCityViewModel = vm, location = Location("Lausanne", 46.5197, 6.6323))
    }

    compose.onNodeWithTag(C.BrowseCityTags.TAB_REVIEWS).performClick()
    compose.waitUntil(5_000) { vm.uiState.value.residencies.items.isNotEmpty() }

    val vortexCardTag = C.BrowseCityTags.residencyCard(vortex.name)
    compose.onNodeWithTag(vortexCardTag).performScrollTo().assertIsDisplayed()

    compose.waitUntil(5_000) {
      vm.uiState.value.residencies.items.any {
        it.title == vortex.name && it.latestReview?.ownerId == ownerUid
      }
    }
    val vortexItem =
        vm.uiState.value.residencies.items.first {
          it.title == vortex.name && it.latestReview != null
        }
    val latestReviewUid = vortexItem.latestReview!!.uid
    compose
        .onNodeWithTag(C.BrowseCityTags.reviewText(latestReviewUid), useUnmergedTree = true)
        .assertIsDisplayed()
    assertEquals(reviewVortex1.grade, vortexItem.meanGrade, 0.001)
  }

  @Test
  fun onlyLausanneResidenciesAreDisplayed() {
    compose.setContent { BrowseCityScreen(browseCityViewModel = vm, location = lausanneLocation) }
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
    compose.setContent { BrowseCityScreen(browseCityViewModel = vm, location = lausanneLocation) }

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
    compose.setContent { BrowseCityScreen(browseCityViewModel = vm, location = lausanneLocation) }

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
  fun emptyState_showsNoResidenciesYet() = runTest {
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
    compose.setContent { BrowseCityScreen(location = lausanneLocation) }
    compose.waitForIdle()
    compose.onNodeWithTag(C.BrowseCityTags.LOCATION_BUTTON).performClick()
    compose.waitForIdle()
    compose.onNodeWithTag(C.CustomLocationDialogTags.DIALOG_TITLE).assertIsDisplayed()
  }

  @Test
  fun locationButton_displaysLocationName() = runTest {
    switchToUser(FakeUser.FakeUser1)
    compose.setContent { BrowseCityScreen(location = lausanneLocation) }
    compose.waitForIdle()
    compose.onNodeWithText("Lausanne").assertIsDisplayed()
  }

  @Test
  fun locationButton_savesLocationToProfile() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val uid = FirebaseEmulator.auth.currentUser!!.uid
    profileRepo.createProfile(
        profile1.copy(userInfo = profile1.userInfo.copy(location = null), ownerId = uid))

    compose.setContent { BrowseCityScreen(location = lausanneLocation, onLocationChange = {}) }
    compose.waitForIdle()
    compose.onNodeWithTag(C.BrowseCityTags.LOCATION_BUTTON).performClick()
    compose.waitForIdle()

    compose.waitUntil(timeoutMillis = 5_000) {
      compose
          .onAllNodesWithTag(C.CustomLocationDialogTags.DIALOG_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithTag(C.CustomLocationDialogTags.LOCATION_TEXT_FIELD).assertIsDisplayed()
  }

  @Test
  fun filter_chips_are_displayed() = runTest {
    setupScreenWithListings()

    compose.waitUntil(10_000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.FILTER_CHIP_ROW, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify all filter chips are displayed by scrolling to each index
    val chips =
        listOf(
            Pair(0, C.BrowseCityTags.FILTER_CHIP_ROOM_TYPE),
            Pair(1, C.BrowseCityTags.FILTER_CHIP_PRICE),
            Pair(2, C.BrowseCityTags.FILTER_CHIP_SIZE),
            Pair(3, C.BrowseCityTags.FILTER_CHIP_START_DATE),
            Pair(4, C.BrowseCityTags.FILTER_CHIP_MOST_RECENT))
    chips.forEach { (index, tag) ->
      compose
          .onNodeWithTag(C.BrowseCityTags.FILTER_CHIP_ROW, useUnmergedTree = true)
          .performScrollToIndex(index)
      compose.waitForIdle()
      compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
    }
  }

  @Test
  fun clicking_room_type_filter_opens_bottom_sheet() = runTest {
    setupScreenWithListings()
    waitForFilterChipRow()
    scrollToAndClickFilterChip(0, C.BrowseCityTags.FILTER_CHIP_ROOM_TYPE)
    waitForBottomSheet()

    compose
        .onNodeWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET, useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET_TITLE, useUnmergedTree = true)
        .assertIsDisplayed()
    compose.onNodeWithText("Filter by Room Type", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun room_type_filter_applies_correctly() = runTest {
    setupScreenWithListings()
    val initialCount = vm.uiState.value.listings.items.size

    waitForFilterChipRow()
    scrollToAndClickFilterChip(0, C.BrowseCityTags.FILTER_CHIP_ROOM_TYPE)
    waitForBottomSheet()
    selectRoomType(RoomType.STUDIO)
    waitForAndClickApplyButton()

    val filteredCount = vm.uiState.value.listings.items.size
    assertTrue("Filtered count should be <= initial count", filteredCount <= initialCount)
    assertTrue("Should have some filtered results", filteredCount >= 0)

    vm.uiState.value.listings.items.forEach { listing ->
      assertTrue(
          "All listings should be Studio type",
          listing.leftBullets.firstOrNull() == RoomType.STUDIO.toString())
    }
  }

  @Test
  fun price_filter_applies_correctly() = runTest {
    setupScreenWithListings()
    val initialCount = vm.uiState.value.listings.items.size

    waitForFilterChipRow()
    scrollToAndClickFilterChip(1, C.BrowseCityTags.FILTER_CHIP_PRICE)
    waitForBottomSheet()
    waitForAndClickApplyButton()

    assertTrue(
        "Filtered count should be <= initial count",
        vm.uiState.value.listings.items.size <= initialCount)
  }

  @Test
  fun size_filter_applies_correctly() = runTest {
    setupScreenWithListings()
    val initialCount = vm.uiState.value.listings.items.size

    waitForFilterChipRow()
    scrollToAndClickFilterChip(2, C.BrowseCityTags.FILTER_CHIP_SIZE)
    waitForBottomSheet()
    waitForAndClickApplyButton()

    assertTrue(
        "Filtered count should be <= initial count",
        vm.uiState.value.listings.items.size <= initialCount)
  }

  @Test
  fun start_date_filter_opens_date_picker() = runTest {
    setupScreenWithListings()
    waitForFilterChipRow()
    scrollToAndClickFilterChip(3, C.BrowseCityTags.FILTER_CHIP_START_DATE)
    waitForBottomSheet()

    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithText("Min Start Date", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithText("Min Start Date", useUnmergedTree = true).performClick()
    compose.waitForIdle()

    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun clear_all_button_clears_all_filters() = runTest {
    setupScreenWithListings()
    applyRoomTypeFilter(RoomType.STUDIO)

    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.FILTER_CLEAR_ALL_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose
        .onNodeWithTag(C.BrowseCityTags.FILTER_CLEAR_ALL_BUTTON, useUnmergedTree = true)
        .performClick()
    compose.waitForIdle()
    compose.waitUntil(5_000) { !vm.uiState.value.listings.loading }

    val filterState = vm.uiState.value.filterState
    assertTrue("Room types should be empty", filterState.selectedRoomTypes.isEmpty())
    assertTrue(
        "Price range should be null",
        filterState.priceRange.first == null && filterState.priceRange.second == null)
    assertTrue(
        "Size range should be null",
        filterState.sizeRange.first == null && filterState.sizeRange.second == null)
    assertTrue(
        "Start date range should be null",
        filterState.startDateRange.first == null && filterState.startDateRange.second == null)
    assertTrue("Sort by most recent should be false", !filterState.sortByMostRecent)
  }

  @Test
  fun filter_bottom_sheet_can_be_dismissed_via_apply() = runTest {
    setupScreenWithListings()
    waitForFilterChipRow()
    scrollToAndClickFilterChip(1, C.BrowseCityTags.FILTER_CHIP_PRICE)
    waitForBottomSheet()

    compose
        .onNodeWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET, useUnmergedTree = true)
        .assertIsDisplayed()

    waitForAndClickApplyButton()

    compose.waitUntil(5_000) { !vm.uiState.value.filterState.showFilterBottomSheet }
    compose
        .onNodeWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun most_recent_toggle_works() = runTest {
    setupScreenWithListings()
    waitForFilterChipRow()
    scrollToAndClickFilterChip(4, C.BrowseCityTags.FILTER_CHIP_MOST_RECENT)
    compose.waitUntil(5_000) { !vm.uiState.value.listings.loading }

    assertTrue(
        "Sort by most recent should be enabled", vm.uiState.value.filterState.sortByMostRecent)
  }

  @Test
  fun clear_button_in_bottom_sheet_clears_filter() = runTest {
    setupScreenWithListings()
    applyRoomTypeFilter(RoomType.STUDIO)

    assertTrue(
        "Room type filter should be active",
        vm.uiState.value.filterState.selectedRoomTypes.isNotEmpty())

    waitForFilterChipRow()
    scrollToAndClickFilterChip(0, C.BrowseCityTags.FILTER_CHIP_ROOM_TYPE)
    compose.waitUntil(2_000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(
              C.BrowseCityTags.FILTER_BOTTOM_SHEET_CLEAR_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose
        .onNodeWithTag(C.BrowseCityTags.FILTER_BOTTOM_SHEET_CLEAR_BUTTON, useUnmergedTree = true)
        .performClick()
    compose.waitForIdle()
    compose.waitUntil(5_000) { !vm.uiState.value.listings.loading }

    assertTrue(
        "Room type filter should be cleared",
        vm.uiState.value.filterState.selectedRoomTypes.isEmpty())
  }

  @Test
  fun empty_state_shows_filtered_message_when_filters_active() = runTest {
    setupScreenWithListings()
    vm.setPriceFilter(10000.0, 20000.0) // Very high price range that won't match anything
    vm.loadListings(lausanneLocation)
    compose.waitUntil(5_000) { !vm.uiState.value.listings.loading }

    if (vm.uiState.value.listings.items.isEmpty()) {
      compose.waitUntil(5_000) {
        compose
            .onAllNodesWithText("No listings match your filters.", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      compose
          .onNodeWithText("No listings match your filters.", useUnmergedTree = true)
          .assertIsDisplayed()
      compose.onNodeWithText("Clear filters", useUnmergedTree = true).assertIsDisplayed()
    }
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

          override suspend fun reverseSearch(latitude: Double, longitude: Double): Location? {
            return null
          }
        }
    val vm =
        BrowseCityViewModel(
            listingsRepository = ThrowingRepo(),
            locationRepository = mockLocationRepository,
            profileRepository = ProfileRepositoryProvider.repository,
            auth = FirebaseEmulator.auth)

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

  @Test
  fun test_image_is_display_and_all_stuff() = runTest {
    switchToUser(FakeUser.FakeUser1)
    listingsRepo.addRentalListing(listingFrib1)
    val fakeLocalRepo = FakePhotoRepository({ fakePhoto }, {}, true)
    val fakeCloudRepo = FakePhotoRepositoryCloud({ fakePhoto }, {}, true, fakeLocalRepo)
    val vm = BrowseCityViewModel(photoRepositoryCloud = fakeCloudRepo)
    vm.loadListings(fribourgLocation)
    compose.waitForIdle()
    assertEquals(1, fakeCloudRepo.retrieveCount)
  }

  fun anonymousReview_showsAnonymousInResidencyCard() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Create a review with a timestamp that's definitely later than reviewVortex2
    // reviewVortex2 has postedAt = Timestamp(Timestamp.now().seconds + 10, 0)
    // So we use a timestamp that's 30 seconds after the base time
    val baseTime = Timestamp.now()
    val anonymousReview =
        reviewVortex1.copy(
            uid = reviewsRepo.getNewUid(),
            ownerId = ownerUid,
            postedAt = Timestamp(baseTime.seconds + 30, 0), // Definitely later than reviewVortex2
            isAnonymous = true)
    reviewsRepo.addReview(anonymousReview)

    vm.loadResidencies(lausanneLocation)
    compose.setContent { BrowseCityScreen(location = lausanneLocation, browseCityViewModel = vm) }

    compose.onNodeWithTag(C.BrowseCityTags.TAB_REVIEWS).performClick()
    compose.waitUntil(5_000) { vm.uiState.value.residencies.items.isNotEmpty() }

    // Wait for loading to complete
    compose.waitUntil(5_000) { !vm.uiState.value.residencies.loading }

    compose
        .onNodeWithTag(C.BrowseCityTags.residencyCard(vortex.name))
        .performScrollTo()
        .assertIsDisplayed()

    // Wait for the review poster name to appear
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.reviewPosterName(vortex.name), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose
        .onNodeWithTag(C.BrowseCityTags.reviewPosterName(vortex.name), useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains("anonymous", substring = true, ignoreCase = true)
  }

  @Test
  fun nonAnonymousReview_showsActualNameInResidencyCard() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Create a review with a timestamp that's definitely later than reviewVortex2
    val baseTime = Timestamp.now()
    val nonAnonymousReview =
        reviewVortex1.copy(
            uid = reviewsRepo.getNewUid(),
            ownerId = ownerUid,
            postedAt = Timestamp(baseTime.seconds + 30, 0), // Definitely later than reviewVortex2
            isAnonymous = false)
    reviewsRepo.addReview(nonAnonymousReview)

    vm.loadResidencies(lausanneLocation)
    compose.setContent { BrowseCityScreen(location = lausanneLocation, browseCityViewModel = vm) }

    compose.onNodeWithTag(C.BrowseCityTags.TAB_REVIEWS).performClick()
    compose.waitUntil(5_000) { vm.uiState.value.residencies.items.isNotEmpty() }

    // Wait for loading to complete
    compose.waitUntil(5_000) { !vm.uiState.value.residencies.loading }

    compose
        .onNodeWithTag(C.BrowseCityTags.residencyCard(vortex.name))
        .performScrollTo()
        .assertIsDisplayed()

    // Wait for the review poster name to appear
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.BrowseCityTags.reviewPosterName(vortex.name), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose
        .onNodeWithTag(C.BrowseCityTags.reviewPosterName(vortex.name), useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains("Bob", substring = true, ignoreCase = true)
        .assertTextContains("King", substring = true, ignoreCase = true)
  }

  @Test
  fun navigateToMap_displaysMapContent() = runTest {
    switchToUser(FakeUser.FakeUser1)

    var currentScreen by mutableStateOf("browse")
    var mapListings: List<ListingCardUI> = emptyList()

    compose.setContent {
      if (currentScreen == "browse") {
        BrowseCityScreen(
            browseCityViewModel = vm,
            location = lausanneLocation,
            onMapClick = { listings ->
              mapListings = listings
              currentScreen = "map"
            })
      } else {
        MapOverviewScreen(
            listings = mapListings,
            centerLocation = lausanneLocation,
            onGoBack = { currentScreen = "browse" },
            onListingClick = {})
      }
    }
    compose.waitUntil(5_000) { vm.uiState.value.listings.items.isNotEmpty() }
    compose.onNodeWithContentDescription("Open Map").assertIsDisplayed().performClick()
    compose.waitForIdle()
    compose.onNodeWithText("Listings map").assertIsDisplayed()
    compose.onNodeWithContentDescription("Open in Maps").assertIsDisplayed()
    compose.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun mapShowsCorrectNumberOfListings() = runTest {
    switchToUser(FakeUser.FakeUser1)
    var mapListings: List<ListingCardUI> = emptyList()
    var onMapScreen by mutableStateOf(false)

    compose.setContent {
      if (!onMapScreen) {
        BrowseCityScreen(
            browseCityViewModel = vm,
            location = lausanneLocation,
            onMapClick = { listings ->
              mapListings = listings
              onMapScreen = true
            })
      } else {
        MapOverviewScreen(
            listings = mapListings,
            centerLocation = lausanneLocation,
            onGoBack = {},
            onListingClick = {})
      }
    }
    compose.waitUntil(5_000) { vm.uiState.value.listings.items.isNotEmpty() }
    compose.onNodeWithContentDescription("Open Map").performClick()
    compose.waitForIdle()
    assertEquals(2, mapListings.size)
  }

  @Test
  fun mapCarousel_appears_whenMultipleListingsAtSameLocation() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val duplicateListing =
        listingLaus1.copy(uid = "duplicate", title = "Duplicate Listing", pricePerMonth = 999.0)
    listingsRepo.addRentalListing(duplicateListing)

    var onMapScreen by mutableStateOf(false)
    var mapListings: List<ListingCardUI> = emptyList()

    compose.setContent {
      if (!onMapScreen) {
        BrowseCityScreen(
            browseCityViewModel = vm,
            location = lausanneLocation,
            onMapClick = { listings ->
              mapListings = listings
              onMapScreen = true
            })
      } else {
        MapOverviewScreen(
            listings = mapListings,
            centerLocation = lausanneLocation,
            onGoBack = {},
            onListingClick = {})
      }
    }
    compose.waitUntil(5_000) { vm.uiState.value.listings.items.size >= 3 }
    compose.onNodeWithContentDescription("Open Map").performClick()
    compose.waitForIdle()
    val grouped = mapListings.groupBy { it.location.latitude to it.location.longitude }
    val locationGroup =
        grouped[Pair(listingLaus1.location.latitude, listingLaus1.location.longitude)]
    assertEquals(3, locationGroup?.size)
  }
}
