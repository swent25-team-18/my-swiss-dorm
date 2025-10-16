package com.android.mySwissDorm.ui.listing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
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
    super.setUp()
    // Prepare two users in the emulator and capture their UIDs
    runTest {
      switchToUser(FakeUser.FakeUser1)
      ownerUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = ownerUid))

      switchToUser(FakeUser.FakeUser2)
      otherUid = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile2.copy(ownerId = otherUid))

      // back to owner for the rest of set up
      switchToUser(FakeUser.FakeUser1)
    }

    // Write minimal but valid profiles with the real repo
    runTest {}

    // Create two listings via the real repo (one per user)
    ownerListing = rentalListing1.copy(ownerId = ownerUid)
    otherListing = rentalListing2.copy(ownerId = otherUid)

    runTest {
      switchToUser(FakeUser.FakeUser1)
      listingsRepo.addRentalListing(ownerListing)
      switchToUser(FakeUser.FakeUser2)
      listingsRepo.addRentalListing(otherListing)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  // -------------------- TESTS --------------------

  @Test
  fun nonOwner_showsContactAndApply_enablesAfterTyping() = run {
    // Log in as owner but open OTHER user's listing -> non-owner branch
    runTest { switchToUser(FakeUser.FakeUser1) }

    compose.setContent {
      val vm = ViewListingViewModel(listingsRepo, profileRepo)
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    compose.waitForIdle()

    compose.onNodeWithTag(C.ViewListingTags.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ViewListingTags.CONTACT_FIELD).performScrollTo().assertIsDisplayed()

    // Apply disabled until user types
    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN).performScrollTo().assertIsNotEnabled()
    compose
        .onNodeWithTag(C.ViewListingTags.CONTACT_FIELD)
        .performTextInput("Hello! I'm interested.")
    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN).assertIsEnabled()
  }

  @Test
  fun owner_showsOnlyEdit() = run {
    // Log in as owner and open his own listing -> owner branch
    runTest { switchToUser(FakeUser.FakeUser1) }

    compose.setContent {
      val vm = ViewListingViewModel(listingsRepo, profileRepo)
      ViewListingScreen(viewListingViewModel = vm, listingUid = ownerListing.uid)
    }
    compose.waitForIdle()

    compose.onNodeWithTag(C.ViewListingTags.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ViewListingTags.EDIT_BTN).performScrollTo().assertIsDisplayed()
    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN).assertDoesNotExist()
    compose.onNodeWithTag(C.ViewListingTags.CONTACT_FIELD).assertDoesNotExist()
  }

  @Test
  fun canScrollToBottomButton() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }

    compose.setContent {
      val vm = ViewListingViewModel(listingsRepo, profileRepo)
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    compose.waitForIdle()

    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun repositoryError_callsOnGoBack() = run {
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
  fun applyButton_disabledWhenWhitespaceOnly() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }

    compose.setContent {
      val vm = ViewListingViewModel(listingsRepo, profileRepo)
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }
    compose.waitForIdle()

    compose.onNodeWithTag(C.ViewListingTags.CONTACT_FIELD).performScrollTo().performTextInput("   ")
    compose.onNodeWithTag(C.ViewListingTags.APPLY_BTN).assertIsNotEnabled()
  }

  @Test
  fun viewModel_setContactMessage_updatesState() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }
    val vm = ViewListingViewModel(listingsRepo, profileRepo)

    compose.setContent {
      ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
    }

    compose.runOnIdle { vm.setContactMessage("Testing message") }
    compose.waitForIdle()

    compose
        .onNodeWithTag(C.ViewListingTags.CONTACT_FIELD)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains("Testing message", substring = true)
  }

  @Test
  fun postedBy_displaysYouWhenOwner() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }

    compose.setContent {
      val vm = ViewListingViewModel(listingsRepo, profileRepo)
      ViewListingScreen(viewListingViewModel = vm, listingUid = ownerListing.uid)
    }
    compose.waitForIdle()

    compose
        .onNodeWithTag(C.ViewListingTags.POSTED_BY)
        .assertIsDisplayed()
        .assertTextContains("(You)", substring = true)
  }
}
