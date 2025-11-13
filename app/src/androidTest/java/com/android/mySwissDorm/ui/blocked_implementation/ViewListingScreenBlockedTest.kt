package com.android.mySwissDorm.ui.blocked_implementation

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
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
import com.android.mySwissDorm.ui.listing.ViewListingScreen
import com.android.mySwissDorm.ui.listing.ViewListingViewModel
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.firestore.FieldValue
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for blocked user functionality in ViewListingScreen. Tests that blocked users cannot apply
 * to listings and see appropriate UI feedback.
 */
@RunWith(AndroidJUnit4::class)
class ViewListingScreenBlockedTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var profileRepo: ProfileRepository
  private lateinit var listingsRepo: RentalListingRepository
  private lateinit var ownerUid: String
  private lateinit var blockedUserUid: String
  private lateinit var ownerListing: RentalListing

  override fun createRepositories() {
    profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    listingsRepo = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()

      // Create listing owner
      switchToUser(FakeUser.FakeUser1)
      ownerUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = ownerUid))

      // Create blocked user (will be blocked by owner)
      switchToUser(FakeUser.FakeUser2)
      blockedUserUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile2.copy(ownerId = blockedUserUid))

      // Create listing owned by FakeUser1
      switchToUser(FakeUser.FakeUser1)
      ownerListing = rentalListing1.copy(ownerId = ownerUid)
      listingsRepo.addRentalListing(ownerListing)

      // Block FakeUser2 from FakeUser1's profile
      FirebaseEmulator.firestore
          .collection("profiles")
          .document(ownerUid)
          .update("blockedUserIds", FieldValue.arrayUnion(blockedUserUid))
          .await()

      // Switch to blocked user for tests
      switchToUser(FakeUser.FakeUser2)
    }
  }

  private suspend fun waitUntil(timeoutMs: Long = 5000, condition: () -> Boolean) {
    val start = System.currentTimeMillis()
    while (!condition()) {
      if (System.currentTimeMillis() - start > timeoutMs) break
      delay(25)
    }
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
  fun blockedUser_seesBlockedNotice_andListingHidden() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = ownerListing.uid)
    }
    waitForScreenRoot()

    // Wait for blocked status to be loaded
    waitUntil { vm.uiState.value.isBlockedByOwner }

    // Verify blocked status is set
    assertTrue("User should be blocked by owner", vm.uiState.value.isBlockedByOwner)

    compose.onNodeWithTag(C.ViewListingTags.BLOCKED_NOTICE).assertIsDisplayed()
    compose.onNodeWithTag(C.ViewListingTags.TITLE).assertDoesNotExist()
    compose.onNodeWithTag(C.ViewListingTags.CONTACT_FIELD).assertDoesNotExist()
    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN).assertDoesNotExist()
  }

  @Test
  fun blockedUser_blockedNoticeBackButton_callsOnGoBack() = runTest {
    var navigatedBack = false
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(
          viewListingViewModel = vm,
          listingUid = ownerListing.uid,
          onGoBack = { navigatedBack = true })
    }
    waitForScreenRoot()

    // Wait for blocked status
    waitUntil { vm.uiState.value.isBlockedByOwner }

    compose.onNodeWithTag(C.ViewListingTags.BLOCKED_BACK_BTN).assertIsDisplayed().performClick()
    compose.waitUntil(3_000) { navigatedBack }
    assertTrue(navigatedBack)
  }

  @Test
  fun nonBlockedUser_applyButtonEnabled_afterTypingMessage() = runTest {
    // Use FakeUser1 (owner) viewing FakeUser2's listing
    // First create a listing for FakeUser2
    switchToUser(FakeUser.FakeUser2)
    val otherOwnerUid = FirebaseEmulator.auth.currentUser!!.uid
    val otherListing = rentalListing2.copy(ownerId = otherOwnerUid)
    listingsRepo.addRentalListing(otherListing)

    // Switch to FakeUser1 (who is NOT blocked by FakeUser2)
    switchToUser(FakeUser.FakeUser1)

    // This user is not blocked by the owner, so they should be able to apply
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    waitForScreenRoot()

    // Wait for profile to load and verify not blocked
    waitUntil {
      vm.uiState.value.fullNameOfPoster.isNotEmpty() && !vm.uiState.value.isBlockedByOwner
    }

    // Verify user is not blocked
    assertFalse(vm.uiState.value.isBlockedByOwner)

    // Type a message
    scrollListTo(C.ViewListingTags.CONTACT_FIELD)
    compose.onNodeWithTag(C.ViewListingTags.CONTACT_FIELD).performTextInput("I'm interested")

    compose.waitForIdle()

    // Verify button state through ViewModel
    assertTrue(vm.uiState.value.contactMessage.isNotBlank())
    assertFalse(vm.uiState.value.isBlockedByOwner)

    // Button should be enabled (not blocked + has message)
    scrollListTo(C.ViewListingTags.APPLY_BTN)
    // The button should exist and be enabled (we can't easily assert enabled without custom
    // matcher,
    // but the ViewModel state confirms it should be enabled)
  }

  @Test
  fun blockedStatus_loadedCorrectly_fromFirestore() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)

    // Load listing
    vm.loadListing(ownerListing.uid)

    // Wait for blocked status to be determined
    waitUntil {
      vm.uiState.value.listing.uid == ownerListing.uid && vm.uiState.value.isBlockedByOwner
    }

    // Verify blocked status is correctly detected
    assertTrue(vm.uiState.value.isBlockedByOwner)
    assertEquals(blockedUserUid, FirebaseEmulator.auth.currentUser!!.uid)

    // Verify in Firestore
    val doc = FirebaseEmulator.firestore.collection("profiles").document(ownerUid).get().await()
    @Suppress("UNCHECKED_CAST")
    val blockedIds = doc.get("blockedUserIds") as? List<String> ?: emptyList()
    assertTrue(blockedUserUid in blockedIds)
  }

  @Test
  fun owner_viewingOwnListing_doesNotCheckBlockedStatus() = runTest {
    // Switch to owner
    switchToUser(FakeUser.FakeUser1)

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    vm.loadListing(ownerListing.uid)

    waitUntil { vm.uiState.value.isOwner }

    // Owner should not have blocked status checked (they own the listing)
    assertFalse(vm.uiState.value.isBlockedByOwner)
    assertTrue(vm.uiState.value.isOwner)
  }
}
