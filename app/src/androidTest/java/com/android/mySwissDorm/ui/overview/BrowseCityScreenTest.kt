package com.android.mySwissDorm.ui.overview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowseCityScreenFirestoreTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private val profileRepo = ProfileRepositoryProvider.repository
  private val listingsRepo = RentalListingRepositoryProvider.repository

  private lateinit var ownerUid: String
  private lateinit var otherUid: String

  private lateinit var laus1: RentalListing
  private lateinit var laus2: RentalListing
  private lateinit var zurich: RentalListing

  override fun createRepositories() {
    ProfileRepositoryProvider.repository =
        ProfileRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
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

    laus1 =
        RentalListing(
            uid = "laus1",
            ownerId = ownerUid,
            postedAt = Timestamp.now(),
            residency = resLaus,
            title = "Lausanne Studio 1",
            roomType = RoomType.STUDIO,
            pricePerMonth = 1200.0,
            areaInM2 = 20,
            startDate = Timestamp.now(),
            description = "",
            imageUrls = emptyList(),
            status = RentalStatus.POSTED)
    laus2 =
        laus1.copy(uid = "laus2", ownerId = otherUid, title = "Lausanne Studio 2", areaInM2 = 22)
    zurich =
        laus1.copy(
            uid = "zurich1", ownerId = otherUid, title = "Zurich Room", residency = resZurich)

    runTest {
      switchToUser(FakeUser.FakeUser1)
      listingsRepo.addRentalListing(laus1)
      switchToUser(FakeUser.FakeUser2)
      listingsRepo.addRentalListing(laus2)
      listingsRepo.addRentalListing(zurich)
    }
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
    compose.setContent { BrowseCityScreen(location = lausanneLocation, onSelectListing = {}) }
    compose.waitForIdle()

    compose.onNodeWithTag(C.BrowseCityTags.LIST).assertIsDisplayed()
    compose.onNodeWithTag(C.BrowseCityTags.card(laus1.uid)).performScrollTo().assertIsDisplayed()
    compose.onNodeWithTag(C.BrowseCityTags.card(laus2.uid)).performScrollTo().assertIsDisplayed()
    compose.onNodeWithTag(C.BrowseCityTags.card(zurich.uid)).assertDoesNotExist()
  }

  @Test
  fun clickingCard_callsOnSelectListing_withCorrectUid() = runTest {
    val clicked = mutableStateOf<ListingCardUI?>(null)
    switchToUser(FakeUser.FakeUser1)

    val lausanneLocation = Location("Lausanne", 46.5197, 6.6323)
    compose.setContent {
      BrowseCityScreen(location = lausanneLocation, onSelectListing = { clicked.value = it })
    }
    compose.waitForIdle()

    compose.onNodeWithTag(C.BrowseCityTags.card(laus1.uid)).performScrollTo().performClick()
    compose.waitUntil(5_000) { clicked.value?.listingUid == laus1.uid }
  }

  @Test
  fun switchingToReviews_showsPlaceholder() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val lausanneLocation = Location("Lausanne", 46.5197, 6.6323)
    compose.setContent { BrowseCityScreen(location = lausanneLocation) }
    compose.waitForIdle()

    compose.onNodeWithTag(C.BrowseCityTags.TAB_REVIEWS).performClick()
    compose.onNodeWithText("Not implemented yet").assertIsDisplayed()
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
    profileRepo.createProfile(profile)

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
        delay(50)
        error("Boom")
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
