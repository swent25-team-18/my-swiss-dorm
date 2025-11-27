package com.android.mySwissDorm.ui.listing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.model.profile.*
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewListingScreenBookmarkTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var profileRepo: ProfileRepository
  private lateinit var listingsRepo: RentalListingRepository
  private lateinit var residenciesRepo: ResidenciesRepository

  private lateinit var ownerUid: String
  private lateinit var viewerUid: String
  private lateinit var listing: RentalListing

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  override fun createRepositories() {
    profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    listingsRepo = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    residenciesRepo = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      switchToUser(FakeUser.FakeUser1)
      ownerUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = ownerUid))
      residenciesRepo.addResidency(resTest)

      switchToUser(FakeUser.FakeUser2)
      viewerUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile2.copy(ownerId = viewerUid))

      switchToUser(FakeUser.FakeUser1)
      listing = rentalListing1.copy(ownerId = ownerUid)
      listingsRepo.addRentalListing(listing)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  private fun waitForScreenRoot() {
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.ViewListingTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  private fun scrollListTo(childTag: String) {
    compose.onNodeWithTag(C.ViewListingTags.ROOT).performScrollToNode(hasTestTag(childTag))
  }

  @Test
  fun bookmarkButton_displayed_whenNonOwnerNonGuest() = runTest {
    switchToUser(FakeUser.FakeUser2)

    compose.setContent {
      val vm = ViewListingViewModel(listingsRepo, profileRepo)
      ViewListingScreen(viewListingViewModel = vm, listingUid = listing.uid)
    }
    waitForScreenRoot()

    // Wait for loading to complete
    compose.waitUntil(10_000) {
      compose
          .onAllNodesWithTag(C.ViewListingTags.BOOKMARK_BTN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose
        .onNodeWithTag(C.ViewListingTags.BOOKMARK_BTN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun bookmarkButton_notDisplayed_whenOwner() = runTest {
    switchToUser(FakeUser.FakeUser1)

    var vm: ViewListingViewModel? = null
    compose.setContent {
      vm = ViewListingViewModel(listingsRepo, profileRepo)
      ViewListingScreen(viewListingViewModel = vm!!, listingUid = listing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      val s = vm!!.uiState.value
      s.listing.uid == listing.uid && s.isOwner
    }

    compose.onNodeWithTag(C.ViewListingTags.BOOKMARK_BTN).assertDoesNotExist()
  }

  @Test
  fun bookmarkButton_notDisplayed_whenGuest() = runTest {
    signInAnonymous()

    var vm: ViewListingViewModel? = null
    compose.setContent {
      vm = ViewListingViewModel(listingsRepo, profileRepo)
      ViewListingScreen(viewListingViewModel = vm!!, listingUid = listing.uid)
    }
    waitForScreenRoot()

    compose.waitUntil(5_000) {
      val s = vm!!.uiState.value
      s.listing.uid == listing.uid
    }

    compose.onNodeWithTag(C.ViewListingTags.BOOKMARK_BTN).assertDoesNotExist()
  }

  @Test
  fun bookmarkButton_togglesBookmark_whenClicked() = runBlocking {
    switchToUser(FakeUser.FakeUser2)
    // Ensure listing is not bookmarked initially
    profileRepo.removeBookmark(viewerUid, listing.uid)

    var vm: ViewListingViewModel? = null
    compose.setContent {
      vm = ViewListingViewModel(listingsRepo, profileRepo)
      ViewListingScreen(viewListingViewModel = vm!!, listingUid = listing.uid)
    }
    waitForScreenRoot()

    // Wait for bookmark button to appear and listing to load
    compose.waitUntil(10_000) {
      val currentVm = vm ?: return@waitUntil false
      currentVm.uiState.value.listing.uid == listing.uid &&
          compose
              .onAllNodesWithTag(C.ViewListingTags.BOOKMARK_BTN, useUnmergedTree = true)
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    // Click bookmark button
    compose.onNodeWithTag(C.ViewListingTags.BOOKMARK_BTN, useUnmergedTree = true).performClick()

    // Wait for bookmark to be added - wait for UI state to reflect the change
    compose.waitUntil(5_000) {
      val currentVm = vm ?: return@waitUntil false
      currentVm.uiState.value.isBookmarked
    }

    // Verify bookmark was added
    val bookmarkedIds = profileRepo.getBookmarkedListingIds(viewerUid)
    assertTrue("Listing should be bookmarked in repository", bookmarkedIds.contains(listing.uid))
    assertTrue("UI state should reflect bookmark", vm!!.uiState.value.isBookmarked)
  }
}
