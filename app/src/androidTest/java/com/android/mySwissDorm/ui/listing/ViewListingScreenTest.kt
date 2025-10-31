package com.android.mySwissDorm.ui.listing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.profile.*
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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

  // test data
  private lateinit var ownerUid: String
  private lateinit var otherUid: String
  private lateinit var ownerListing: RentalListing
  private lateinit var otherListing: RentalListing

  override fun createRepositories() {
    profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    listingsRepo = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    // Prepare two users in the emulator and capture their UIDs
    runTest {
      super.setUp()
      switchToUser(FakeUser.FakeUser1)
      ownerUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = ownerUid))

      switchToUser(FakeUser.FakeUser2)
      otherUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile2.copy(ownerId = otherUid))

      // back to owner for the rest of set up
      switchToUser(FakeUser.FakeUser1)

      // Create two listings via the real repo (one per user)
      ownerListing = rentalListing1.copy(ownerId = ownerUid)
      otherListing = rentalListing2.copy(ownerId = otherUid)

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
  fun nonOwner_showsContactAndApply_enablesAfterTyping() = runTest {
    compose.setContent {
      val vm = ViewListingViewModel(listingsRepo, profileRepo)
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    compose.onNodeWithTag(C.ViewListingTags.ROOT).assertIsDisplayed()

    scrollListTo(C.ViewListingTags.CONTACT_FIELD)
    compose
        .onNodeWithTag(C.ViewListingTags.CONTACT_FIELD, useUnmergedTree = true)
        .assertIsDisplayed()

    // Apply disabled until user types
    scrollListTo(C.ViewListingTags.APPLY_BTN)
    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN, useUnmergedTree = true).assertIsNotEnabled()
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
      val vm = ViewListingViewModel(listingsRepo, profileRepo)
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

    compose
        .onNodeWithTag(C.ViewListingTags.POSTED_BY, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains("(You)", substring = true)
  }

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

    // Scroll to and click the "Posted by ..." text
    scrollListTo(C.ViewListingTags.POSTED_BY)
    compose
        .onNodeWithTag(C.ViewListingTags.POSTED_BY, useUnmergedTree = true)
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

    // Scroll to and click the "Posted by ..." text
    scrollListTo(C.ViewListingTags.POSTED_BY)
    compose
        .onNodeWithTag(C.ViewListingTags.POSTED_BY, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    assertEquals(otherUid, navigatedToId)
  }
}
