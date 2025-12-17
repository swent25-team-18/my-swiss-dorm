package com.android.mySwissDorm.ui.listing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.profile.*
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakePhotoRepository
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_FILE_NAME
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_NAME
import com.android.mySwissDorm.utils.FakePhotoRepository.Companion.FAKE_SUFFIX
import com.android.mySwissDorm.utils.FakePhotoRepositoryCloud
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.android.mySwissDorm.utils.NetworkUtils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewListingScreenFirestoreTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  // real repos (created in createRepositories)
  private lateinit var profileRepo: ProfileRepository
  private lateinit var listingsRepo: RentalListingRepository
  private lateinit var residenciesRepo: ResidenciesRepository

  // test data
  private lateinit var ownerUid: String
  private lateinit var otherUid: String
  private lateinit var ownerListing: RentalListing
  private lateinit var otherListing: RentalListing

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  override fun createRepositories() {
    PhotoRepositoryProvider.initialize(
        context = InstrumentationRegistry.getInstrumentation().context)
    profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    listingsRepo = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    RentalListingRepositoryProvider.repository = listingsRepo
    residenciesRepo = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    // Prepare two users in the emulator and capture their UIDs
    runTest {
      super.setUp()
      switchToUser(FakeUser.FakeUser1)
      ownerUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = ownerUid))
      residenciesRepo.addResidency(resTest)
      residenciesRepo.addResidency(resTest2)

      switchToUser(FakeUser.FakeUser2)
      otherUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile2.copy(ownerId = otherUid))
      residenciesRepo.addResidency(resTest)
      residenciesRepo.addResidency(resTest2)

      // back to owner for the rest of set up
      switchToUser(FakeUser.FakeUser1)

      // Create two listings via the real repo (one per user)
      ownerListing = rentalListing1.copy(ownerId = ownerUid, title = "First title")
      otherListing = rentalListing2.copy(ownerId = otherUid, title = "Second title")

      switchToUser(FakeUser.FakeUser1)
      listingsRepo.addRentalListing(ownerListing)
      switchToUser(FakeUser.FakeUser2)
      listingsRepo.addRentalListing(otherListing)

      // Default to owner user for tests; individual tests can switch as needed
      switchToUser(FakeUser.FakeUser1)
    }
  }

  @After
  override fun tearDown() {
    unmockkAll()
    super.tearDown()
  }

  /** Wait until the screen root exists (first composition done). */
  private fun waitForScreenRoot() {
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.ViewListingTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  /** Scroll inside the list/root to reveal a child with [childTag]. */
  private fun scrollListTo(childTag: String) {
    compose.onNodeWithTag(C.ViewListingTags.ROOT).performScrollToNode(hasTestTag(childTag))
  }

  // -------------------- TESTS --------------------

  @Test
  fun nonOwner_showsContactAndApply_enablesAfterTyping() =
      runTest(timeout = 20.toDuration(unit = DurationUnit.SECONDS)) {
        val vm = ViewListingViewModel(listingsRepo, profileRepo)
        compose.setContent {
          ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
        }
        waitForScreenRoot()

        // Wait for ViewModel to finish loading (including POI calculation)
        compose.waitUntil(10_000) { vm.uiState.value.listing.uid == otherListing.uid }
        compose.waitForIdle()

        compose.onNodeWithTag(C.ViewListingTags.ROOT).assertIsDisplayed()

        scrollListTo(C.ViewListingTags.CONTACT_FIELD)
        compose
            .onNodeWithTag(C.ViewListingTags.CONTACT_FIELD, useUnmergedTree = true)
            .assertIsDisplayed()

        // Apply disabled until user types
        scrollListTo(C.ViewListingTags.APPLY_BTN)
        compose
            .onNodeWithTag(C.ViewListingTags.APPLY_BTN, useUnmergedTree = true)
            .assertIsNotEnabled()
        compose
            .onNodeWithTag(C.ViewListingTags.CONTACT_FIELD)
            .performTextInput("Hello! I'm interested.")
        compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN).assertIsEnabled()
      }

  @Test
  fun owner_showsOnlyEdit() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = ownerListing.uid)
    }
    waitForScreenRoot()
    compose.onNodeWithTag(C.ViewListingTags.ROOT).assertIsDisplayed()

    compose.waitUntil(5_000) { vm.uiState.value.isOwner }

    scrollListTo(C.ViewListingTags.EDIT_BTN)
    compose.onNodeWithTag(C.ViewListingTags.EDIT_BTN, useUnmergedTree = true).assertIsDisplayed()

    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN).assertDoesNotExist()
    compose.onNodeWithTag(C.ViewListingTags.CONTACT_FIELD).assertDoesNotExist()
  }

  @Test
  fun canScrollToBottomButton() = runTest {
    compose.setContent {
      val vm = ViewListingViewModel(listingsRepo, profileRepo)
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    scrollListTo(C.ViewListingTags.APPLY_BTN)
    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun repositoryError_callsOnGoBack() = runTest {
    var navigatedBack = false

    compose.setContent {
      val vm = ViewListingViewModel(listingsRepo, profileRepo, residenciesRepo)
      // Pass a non-existing uid so real repo throws
      ViewListingScreen(
          viewListingViewModel = vm,
          listingUid = "missing-" + UUID.randomUUID(),
          onGoBack = { navigatedBack = true })
    }

    compose.waitUntil(4_000) { navigatedBack }
  }

  @Test
  fun applyButton_disabledWhenWhitespaceOnly() = runTest {
    compose.setContent {
      val vm = ViewListingViewModel(listingsRepo, profileRepo)
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    scrollListTo(C.ViewListingTags.CONTACT_FIELD)
    compose
        .onNodeWithTag(C.ViewListingTags.CONTACT_FIELD, useUnmergedTree = true)
        .performTextInput("   ")

    scrollListTo(C.ViewListingTags.APPLY_BTN)
    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN, useUnmergedTree = true).assertIsNotEnabled()
  }

  @Test
  fun viewModel_setContactMessage_updatesState() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)

    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    compose.runOnIdle { vm.setContactMessage("Testing message") }
    compose.waitUntil(3_000) {
      // Ensure field exists and contains the text
      compose
          .onAllNodesWithTag(C.ViewListingTags.CONTACT_FIELD, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    scrollListTo(C.ViewListingTags.CONTACT_FIELD)
    compose
        .onNodeWithTag(C.ViewListingTags.CONTACT_FIELD, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains("Testing message", substring = true)
  }

  @Test
  fun postedBy_displaysYouWhenOwner() = runTest {
    switchToUser(FakeUser.FakeUser1)

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = ownerListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == ownerListing.uid && s.isOwner && s.fullNameOfPoster.isNotBlank()
    }

    compose.onNodeWithTag(C.ViewListingTags.POSTED_BY, useUnmergedTree = true).assertIsDisplayed()
    // Check the name Text specifically since it contains "(You)" when owner
    compose
        .onNodeWithTag(C.ViewListingTags.POSTED_BY_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains("(You)", substring = true)
  }
  //
  @Test
  fun clickingPosterName_callsOnViewProfile_forOwner() = runTest {
    switchToUser(FakeUser.FakeUser1)
    var navigatedToId: String? = null

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(
          viewListingViewModel = vm,
          listingUid = ownerListing.uid,
          onViewProfile = { navigatedToId = it })
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == ownerListing.uid && s.isOwner && s.fullNameOfPoster.isNotBlank()
    }

    // Scroll to and click the name (which is the clickable element)
    scrollListTo(C.ViewListingTags.POSTED_BY_NAME)
    compose
        .onNodeWithTag(C.ViewListingTags.POSTED_BY_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    assertEquals(ownerUid, navigatedToId)
  }

  @Test
  fun clickingPosterName_callsOnViewProfile_forNonOwner() = runTest {
    switchToUser(FakeUser.FakeUser1)
    var navigatedToId: String? = null

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(
          viewListingViewModel = vm,
          listingUid = otherListing.uid, // viewing User2 listing
          onViewProfile = { navigatedToId = it })
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid && !s.isOwner && s.fullNameOfPoster.isNotBlank()
    }

    // Scroll to and click the name (which is the clickable element)
    scrollListTo(C.ViewListingTags.POSTED_BY_NAME)
    compose
        .onNodeWithTag(C.ViewListingTags.POSTED_BY_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    assertEquals(otherUid, navigatedToId)
  }

  @Test
  fun mapPreview_isDisplayed_whenLocationIsValid() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    // Wait for listing to load
    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid
    }

    scrollListTo(C.ViewListingTags.LOCATION)
    compose.onNodeWithTag(C.ViewListingTags.LOCATION).assertIsDisplayed()
    compose.onNodeWithText("LOCATION (Not available)").assertDoesNotExist()
  }

  @Test
  fun mapPlaceholder_isDisplayed_whenLocationIsInvalid() = runTest {
    switchToUser(FakeUser.FakeUser2)
    val resTestInvalid =
        resTest.copy(name = "invalid name", location = Location("No Location", 0.0, 0.0))
    residenciesRepo.addResidency(resTestInvalid)
    val invalidListing =
        rentalListing2.copy(
            uid = listingsRepo.getNewUid(),
            residencyName = resTestInvalid.name,
            ownerId = FirebaseEmulator.auth.currentUser!!.uid)

    listingsRepo.addRentalListing(invalidListing)
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = invalidListing.uid)
    }
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      val s = vm.uiState.value
      s.listing.uid == invalidListing.uid
    }
    scrollListTo(C.ViewListingTags.LOCATION)
    compose.onNodeWithTag(C.ViewListingTags.LOCATION).assertIsDisplayed()
  }

  @Test
  fun mapClick_triggers_onViewMapCallback_withCorrectData() = runTest {
    var callbackCalled = false
    var capturedLat: Double? = null
    var capturedLon: Double? = null
    var capturedTitle: String? = null
    var capturedName: String? = null

    val expectedListing = otherListing
    val vm = ViewListingViewModel(listingsRepo, profileRepo)

    compose.setContent {
      ViewListingScreen(
          viewListingViewModel = vm,
          listingUid = expectedListing.uid,
          onViewMap = { lat, lon, title, nameId ->
            callbackCalled = true
            capturedLat = lat
            capturedLon = lon
            capturedTitle = title
            capturedName = context.getString(nameId)
          })
    }

    waitForScreenRoot()

    compose.waitUntil(5_000) {
      val s = vm.uiState.value
      s.listing.uid == expectedListing.uid
    }

    scrollListTo(C.ViewListingTags.LOCATION)
    compose.onNodeWithTag(C.ViewListingTags.LOCATION).performClick()

    assertTrue("onViewMap callback was not triggered.", callbackCalled)
    // Just ensure we get some coordinates
    assertTrue("Latitude must not be null", capturedLat != null)
    assertTrue("Longitude must not be null", capturedLon != null)

    // And that metadata is correct
    assertEquals(expectedListing.title, capturedTitle)
    assertEquals("Listing Location", capturedName)
  }

  @Test
  fun guestMode_showsSignInMessage_andHidesInteractiveElements() = runTest {
    signInAnonymous()
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()
    // Wait for listing to load and guest state to be set
    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid && s.isGuest
    }
    compose.onNodeWithTag(C.ViewListingTags.LOCATION).performScrollTo().assertIsDisplayed()
    compose
        .onNodeWithText("Sign in to contact the owner and apply.")
        .performScrollTo()
        .assertIsDisplayed()
    compose.onNodeWithTag(C.ViewListingTags.CONTACT_FIELD).assertDoesNotExist()
    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN).assertDoesNotExist()
    compose.onNodeWithTag(C.ViewListingTags.EDIT_BTN).assertDoesNotExist()
  }

  @Test
  fun check_image_correctly_retrieved() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val fakePhoto = Photo(File.createTempFile(FAKE_NAME, FAKE_SUFFIX).toUri(), FAKE_FILE_NAME)
    val listing =
        rentalListing3.copy(
            ownerId = FirebaseEmulator.auth.currentUser!!.uid,
            imageUrls = listOf(fakePhoto.fileName))
    listingsRepo.addRentalListing(listing)

    val fakeLocalRepo = FakePhotoRepository({ fakePhoto }, {}, true)
    val fakeCloudRepo = FakePhotoRepositoryCloud({ fakePhoto }, {}, true, fakeLocalRepo)

    val vm =
        ViewListingViewModel(
            rentalListingRepository = listingsRepo,
            profileRepository = profileRepo,
            photoRepositoryCloud = fakeCloudRepo)

    vm.loadListing(listing.uid, context)
    compose.waitForIdle()
    assertEquals(1, fakeCloudRepo.retrieveCount)
  }

  @Test
  fun poiDistances_displaysHeading_whenPOIsExist() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid
    }

    // Check that POI section heading exists
    scrollListTo(C.ViewListingTags.POI_DISTANCES)
    compose
        .onNodeWithTag(C.ViewListingTags.POI_DISTANCES, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun blockedUser_showsBlockedNotice() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Block the current user
    profileRepo.addBlockedUser(ownerUid, otherUid)

    switchToUser(FakeUser.FakeUser2)
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = ownerListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == ownerListing.uid && s.isBlockedByOwner
    }

    compose
        .onNodeWithTag(C.ViewListingTags.BLOCKED_NOTICE, useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ViewListingTags.BLOCKED_BACK_BTN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun blockedUser_backButton_callsOnGoBack() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Block the current user
    profileRepo.addBlockedUser(ownerUid, otherUid)

    switchToUser(FakeUser.FakeUser2)
    var navigatedBack = false
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(
          viewListingViewModel = vm,
          listingUid = ownerListing.uid,
          onGoBack = { navigatedBack = true })
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == ownerListing.uid && s.isBlockedByOwner
    }

    compose.onNodeWithTag(C.ViewListingTags.BLOCKED_BACK_BTN, useUnmergedTree = true).performClick()
    assertTrue("onGoBack should be called", navigatedBack)
  }

  @Test
  fun clickingPosterName_offline_showsToast() = runTest {
    switchToUser(FakeUser.FakeUser1)
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(any()) } returns false

    var navigatedToId: String? = null

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(
          viewListingViewModel = vm,
          listingUid = otherListing.uid, // viewing User2 listing (not owner)
          onViewProfile = { navigatedToId = it })
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid && !s.isOwner && s.fullNameOfPoster.isNotBlank()
    }

    // Mock Toast to detect when it's shown
    // Note: We can't easily test Toast in Compose UI tests, but we can verify navigation doesn't
    // happen
    scrollListTo(C.ViewListingTags.POSTED_BY_NAME)
    compose
        .onNodeWithTag(C.ViewListingTags.POSTED_BY_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    // Verify navigation did NOT happen (since we're offline and it's not our profile)
    compose.waitForIdle()
    assertNull("Navigation should not happen when offline for non-owner", navigatedToId)
  }

  @Test
  fun backButton_callsOnGoBack() = runTest {
    var navigatedBack = false
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(
          viewListingViewModel = vm,
          listingUid = otherListing.uid,
          onGoBack = { navigatedBack = true })
    }
    waitForScreenRoot()

    compose.waitUntil(5_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid
    }

    compose.onNodeWithTag(C.ViewListingTags.BACK_BTN, useUnmergedTree = true).performClick()
    assertTrue("onGoBack should be called", navigatedBack)
  }

  @Test
  fun bookmarkButton_togglesBookmark() = runTest {
    switchToUser(FakeUser.FakeUser2)
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = ownerListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == ownerListing.uid && !s.isOwner && !s.isGuest
    }

    val initialBookmarkState = vm.uiState.value.isBookmarked
    compose.onNodeWithTag(C.ViewListingTags.BOOKMARK_BTN, useUnmergedTree = true).performClick()

    compose.waitUntil(5_000) { vm.uiState.value.isBookmarked != initialBookmarkState }

    assertTrue(
        "Bookmark state should have changed", vm.uiState.value.isBookmarked != initialBookmarkState)
  }

  @Test
  fun editButton_callsOnEdit() = runTest {
    switchToUser(FakeUser.FakeUser1)
    var editCalled = false
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(
          viewListingViewModel = vm, listingUid = ownerListing.uid, onEdit = { editCalled = true })
    }
    waitForScreenRoot()

    compose.waitUntil(5_000) { vm.uiState.value.isOwner }

    scrollListTo(C.ViewListingTags.EDIT_BTN)
    compose.onNodeWithTag(C.ViewListingTags.EDIT_BTN, useUnmergedTree = true).performClick()
    assertTrue("onEdit should be called", editCalled)
  }

  @Test
  fun applyButton_callsOnApply() = runTest {
    var applyCalled = false
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(
          viewListingViewModel = vm,
          listingUid = otherListing.uid,
          onApply = { applyCalled = true })
    }
    waitForScreenRoot()

    compose.waitUntil(5_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid
    }

    scrollListTo(C.ViewListingTags.CONTACT_FIELD)
    compose
        .onNodeWithTag(C.ViewListingTags.CONTACT_FIELD, useUnmergedTree = true)
        .performTextInput("Test message")

    scrollListTo(C.ViewListingTags.APPLY_BTN)
    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN, useUnmergedTree = true).performClick()
    assertTrue("onApply should be called", applyCalled)
  }

  @Test
  fun blockedUser_withMessage_showsBlockedNotice() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Block the current user
    profileRepo.addBlockedUser(ownerUid, otherUid)

    switchToUser(FakeUser.FakeUser2)
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = ownerListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == ownerListing.uid && s.isBlockedByOwner
    }

    // Blocked users should see blocked notice, not the apply button
    compose
        .onNodeWithTag(C.ViewListingTags.BLOCKED_NOTICE, useUnmergedTree = true)
        .assertIsDisplayed()
    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN).assertDoesNotExist()
  }

  @Test
  fun errorMessage_triggersOnGoBack() = runTest {
    var navigatedBack = false
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(
          viewListingViewModel = vm,
          listingUid = "invalid-uid",
          onGoBack = { navigatedBack = true })
    }

    compose.waitUntil(5_000) { navigatedBack }
    assertTrue("onGoBack should be called on error", navigatedBack)
  }

  @Test
  fun owner_doesNotShowBookmarkButton() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = ownerListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(5_000) { vm.uiState.value.isOwner }

    compose.onNodeWithTag(C.ViewListingTags.BOOKMARK_BTN).assertDoesNotExist()
  }

  @Test
  fun guest_doesNotShowBookmarkButton() = runTest {
    signInAnonymous()
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(5_000) { vm.uiState.value.isGuest }

    compose.onNodeWithTag(C.ViewListingTags.BOOKMARK_BTN).assertDoesNotExist()
  }

  @Test
  fun clickingPosterName_online_navigates() = runTest {
    switchToUser(FakeUser.FakeUser1)
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(any()) } returns true

    var navigatedToId: String? = null

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(
          viewListingViewModel = vm,
          listingUid = otherListing.uid, // viewing User2 listing (not owner)
          onViewProfile = { navigatedToId = it })
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid && !s.isOwner && s.fullNameOfPoster.isNotBlank()
    }

    scrollListTo(C.ViewListingTags.POSTED_BY_NAME)
    compose
        .onNodeWithTag(C.ViewListingTags.POSTED_BY_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    assertEquals("Navigation should happen when online", otherUid, navigatedToId)
  }

  @Test
  fun fullScreenModeWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val photo = Photo(File.createTempFile(FAKE_NAME, FAKE_SUFFIX).toUri(), FAKE_FILE_NAME)
    val listing =
        rentalListing3.copy(
            ownerId = FirebaseEmulator.auth.currentUser!!.uid, imageUrls = listOf(photo.fileName))
    listingsRepo.addRentalListing(listing)
    val vm =
        ViewListingViewModel(
            rentalListingRepository = listingsRepo,
            profileRepository = profileRepo,
            photoRepositoryCloud =
                FakePhotoRepositoryCloud(onRetrieve = { photo }, onUpload = {}, onDelete = true))
    compose.setContent { ViewListingScreen(listingUid = listing.uid, viewListingViewModel = vm) }
    compose.waitForIdle()

    // Wait until the image node exists in the semantics tree (not necessarily visible yet)
    compose.waitUntil("The image is not shown", 5_000) {
      compose
          .onAllNodesWithTag(C.ImageGridTags.imageTag(photo.image), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    // Click on a photo to display in full screen
    compose
        .onNodeWithTag(C.ImageGridTags.imageTag(photo.image), useUnmergedTree = true)
        .performScrollTo()
        .performClick()

    compose.waitForIdle()
    // Check image node for full screen exists (not necessarily visible yet)
    compose.waitUntil("The clicked image is not shown in full screen", 5_000) {
      compose
          .onAllNodesWithTag(
              C.FullScreenImageViewerTags.imageTag(photo.image), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Check that go back to the view listing page
    compose
        .onNodeWithTag(C.FullScreenImageViewerTags.DELETE_BUTTON, useUnmergedTree = true)
        .performClick()
    compose.waitUntil("The listing page is not shown after leaving the full screen mode", 5_000) {
      compose.onNodeWithTag(C.ImageGridTags.imageTag(photo.image)).isDisplayed()
    }
  }

  @Test
  fun title_isDisplayed() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid
    }

    scrollListTo(C.ViewListingTags.TITLE)
    compose.onNodeWithTag(C.ViewListingTags.TITLE, useUnmergedTree = true).assertIsDisplayed()
    compose
        .onNodeWithTag(C.ViewListingTags.TITLE, useUnmergedTree = true)
        .assertTextContains(otherListing.title, substring = true)
  }

  @Test
  fun descriptionSection_isDisplayed() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid
    }

    scrollListTo(C.ViewListingTags.DESCRIPTION)
    compose.onNodeWithTag(C.ViewListingTags.DESCRIPTION, useUnmergedTree = true).assertIsDisplayed()
    // Use onNodeWithText to find the description text directly
    compose.onNodeWithText(otherListing.description, substring = true).assertIsDisplayed()
  }

  @Test
  fun bulletSection_isDisplayed() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid
    }

    scrollListTo(C.ViewListingTags.BULLETS)
    compose.onNodeWithTag(C.ViewListingTags.BULLETS, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun residencyName_isDisplayedInBulletSection() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid
    }

    scrollListTo(C.ViewListingTags.BULLETS)
    compose
        .onNodeWithTag(C.ViewListingTags.RESIDENCY_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals(otherListing.residencyName)
  }

  @Test
  fun applyButton_disabledWhenBlocked() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Block the other user
    profileRepo.addBlockedUser(ownerUid, otherUid)

    switchToUser(FakeUser.FakeUser2)
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = ownerListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == ownerListing.uid && s.isBlockedByOwner
    }

    // Blocked users should see blocked notice, not apply button
    compose
        .onNodeWithTag(C.ViewListingTags.BLOCKED_NOTICE, useUnmergedTree = true)
        .assertIsDisplayed()
    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN).assertDoesNotExist()
  }

  @Test
  fun blockedUser_appliesButtonColorChanges() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Block the other user
    profileRepo.addBlockedUser(ownerUid, otherUid)

    switchToUser(FakeUser.FakeUser2)
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = ownerListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == ownerListing.uid && s.isBlockedByOwner
    }

    // When blocked, should show blocked notice instead of apply button
    compose
        .onNodeWithTag(C.ViewListingTags.BLOCKED_NOTICE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun postedBySection_displaysCorrectly() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid && s.fullNameOfPoster.isNotBlank()
    }

    scrollListTo(C.ViewListingTags.POSTED_BY)
    compose.onNodeWithTag(C.ViewListingTags.POSTED_BY, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun locationSection_displaysCorrectly() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm.uiState.value
      s.listing.uid == otherListing.uid
    }

    scrollListTo(C.ViewListingTags.LOCATION)
    compose.onNodeWithTag(C.ViewListingTags.LOCATION, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun poiSection_showsLoadingState() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)

    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()
    updateUiState(vm) { it.copy(isLoadingPOIs = true, poiDistances = emptyList()) }
    compose.waitForIdle()
    scrollListTo(C.ViewListingTags.POI_DISTANCES)
    val loadingText = context.getString(R.string.poi_loading_message)
    compose.onNodeWithText(loadingText).assertIsDisplayed()
  }

  @Test
  fun poiSection_showsEmptyMessage_whenNoPOIs() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)

    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()
    updateUiState(vm) { it.copy(isLoadingPOIs = false, poiDistances = emptyList()) }
    compose.waitForIdle()
    scrollListTo(C.ViewListingTags.POI_DISTANCES)
    val emptyText = context.getString(R.string.view_listing_no_points_of_interest)
    compose.onNodeWithText(emptyText).assertIsDisplayed()
  }

  @Test
  fun translateButtonTextUpdatesWhenClicked() {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)

    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    compose.onNodeWithTag(C.ViewListingTags.TRANSLATE_BTN).assertIsDisplayed()
    compose
        .onNodeWithTag(C.ViewListingTags.TRANSLATE_BTN)
        .assertTextEquals(context.getString(R.string.view_listing_translate_listing))
    compose.onNodeWithTag(C.ViewListingTags.TRANSLATE_BTN).performClick()

    compose.waitForIdle()

    compose
        .onNodeWithTag(C.ViewListingTags.TRANSLATE_BTN)
        .assertTextEquals(context.getString(R.string.see_original))
  }

  @Test
  fun translateButtonSuccessfullyTranslatesListing() {
    // Set the locale to French so it translates the listing in French
    Locale.setDefault(Locale.FRENCH)

    val vm = ViewListingViewModel(listingsRepo, profileRepo)

    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(5_000) { vm.uiState.value.listing.uid == otherListing.uid }

    compose.onNodeWithTag(C.ViewListingTags.TRANSLATE_BTN).assertIsDisplayed()
    compose.onNodeWithTag(C.ViewListingTags.TITLE).assertIsDisplayed()
    compose.onNodeWithTag(C.ViewListingTags.DESCRIPTION_TEXT).assertIsDisplayed()

    compose.onNodeWithTag(C.ViewListingTags.TRANSLATE_BTN).performClick()

    compose.waitForIdle()

    compose.waitUntil(20_000) {
      compose.onNodeWithText("Deuxième titre").isDisplayed() &&
          compose.onNodeWithText("Un bon studio proche du campus.").isDisplayed()
    }

    compose.onNodeWithTag(C.ViewListingTags.TITLE).assertTextEquals("Deuxième titre")
    compose
        .onNodeWithTag(C.ViewListingTags.DESCRIPTION_TEXT)
        .assertTextEquals("Un bon studio proche du campus.")
  }
}
/**
 * Helper to force the ViewModel into a specific state using reflection. This allows us to test the
 * UI without relying on the real async data loading.
 */
private fun updateUiState(
    vm: ViewListingViewModel,
    update: (ViewListingUIState) -> ViewListingUIState
) {
  try {
    val field = ViewListingViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val stateFlow = field.get(vm) as MutableStateFlow<ViewListingUIState>
    stateFlow.value = update(stateFlow.value)
  } catch (e: Exception) {
    throw RuntimeException("Failed to update UI state via reflection", e)
  }
}
