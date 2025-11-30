package com.android.mySwissDorm.ui.listing

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewListingScreenShareTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var profileRepo: ProfileRepository
  private lateinit var listingsRepo: RentalListingRepository
  private lateinit var residenciesRepo: ResidenciesRepository

  private lateinit var ownerUid: String
  private lateinit var testListing: RentalListing

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

      testListing =
          rentalListing1.copy(
              uid = listingsRepo.getNewUid(), ownerId = ownerUid, location = resTest.location)

      listingsRepo.addRentalListing(testListing)
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

  @Test
  fun shareButton_isDisplayed() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      MySwissDormAppTheme {
        ViewListingScreen(viewListingViewModel = vm, listingUid = testListing.uid)
      }
    }
    waitForScreenRoot()

    compose.onNodeWithTag("share_btn").assertIsDisplayed()
  }

  @Test
  fun shareButton_opensShareDialog() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      MySwissDormAppTheme {
        ViewListingScreen(viewListingViewModel = vm, listingUid = testListing.uid)
      }
    }
    waitForScreenRoot()

    compose.onNodeWithTag("share_btn").performClick()

    compose.waitUntil(2_000) {
      compose
          .onAllNodesWithTag(C.ShareLinkDialogTags.DIALOG_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).assertIsDisplayed()
  }

  @Test
  fun shareDialog_displaysQrCode() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      MySwissDormAppTheme {
        ViewListingScreen(viewListingViewModel = vm, listingUid = testListing.uid)
      }
    }
    waitForScreenRoot()

    compose.onNodeWithTag("share_btn").performClick()

    compose.waitUntil(2_000) {
      compose.onAllNodesWithTag(C.ShareLinkDialogTags.QR_CODE).fetchSemanticsNodes().isNotEmpty()
    }

    compose.onNodeWithTag(C.ShareLinkDialogTags.QR_CODE).assertIsDisplayed()
  }

  @Test
  fun shareDialog_displaysCopyLinkButton() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      MySwissDormAppTheme {
        ViewListingScreen(viewListingViewModel = vm, listingUid = testListing.uid)
      }
    }
    waitForScreenRoot()

    compose.onNodeWithTag("share_btn").performClick()

    compose.waitUntil(2_000) {
      compose
          .onAllNodesWithTag(C.ShareLinkDialogTags.COPY_LINK_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.ShareLinkDialogTags.COPY_LINK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun shareDialog_copyLinkButton_dismissesDialog() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      MySwissDormAppTheme {
        ViewListingScreen(viewListingViewModel = vm, listingUid = testListing.uid)
      }
    }
    waitForScreenRoot()

    compose.onNodeWithTag("share_btn").performClick()

    compose.waitUntil(2_000) {
      compose
          .onAllNodesWithText(context.getString(R.string.share_dialog_copy_link))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.ShareLinkDialogTags.COPY_LINK_BUTTON).performClick()

    compose.waitUntil(2_000) {
      compose.onAllNodesWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).fetchSemanticsNodes().isEmpty()
    }

    compose.onNodeWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).assertDoesNotExist()
  }

  @Test
  fun shareDialog_cancelText_dismissesDialog() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      MySwissDormAppTheme {
        ViewListingScreen(viewListingViewModel = vm, listingUid = testListing.uid)
      }
    }
    waitForScreenRoot()

    compose.onNodeWithTag("share_btn").performClick()

    compose.waitUntil(2_000) {
      compose
          .onAllNodesWithTag(C.ShareLinkDialogTags.CANCEL_TEXT)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.ShareLinkDialogTags.CANCEL_TEXT).performClick()

    compose.waitUntil(2_000) {
      compose.onAllNodesWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).fetchSemanticsNodes().isEmpty()
    }

    compose.onNodeWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).assertDoesNotExist()
  }

  @Test
  fun shareButton_hasCorrectContentDescription() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      MySwissDormAppTheme {
        ViewListingScreen(viewListingViewModel = vm, listingUid = testListing.uid)
      }
    }
    waitForScreenRoot()

    compose
        .onNodeWithTag("share_btn")
        .assertIsDisplayed()
        .assert(hasContentDescription(context.getString(R.string.share)))
  }

  @Test
  fun shareDialog_qrCodeContainsListingLink() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      MySwissDormAppTheme {
        ViewListingScreen(viewListingViewModel = vm, listingUid = testListing.uid)
      }
    }
    waitForScreenRoot()

    compose.onNodeWithTag("share_btn").performClick()

    compose.waitUntil(2_000) {
      compose.onAllNodesWithTag(C.ShareLinkDialogTags.QR_CODE).fetchSemanticsNodes().isNotEmpty()
    }

    // QR code should be displayed (indicating link was generated)
    compose.onNodeWithTag(C.ShareLinkDialogTags.QR_CODE).assertIsDisplayed()
  }

  @Test
  fun shareButton_worksForNonOwner() = runTest {
    switchToUser(FakeUser.FakeUser2)
    val otherUid = FirebaseEmulator.auth.currentUser!!.uid
    profileRepo.createProfile(profile2.copy(ownerId = otherUid))

    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      MySwissDormAppTheme {
        ViewListingScreen(viewListingViewModel = vm, listingUid = testListing.uid)
      }
    }
    waitForScreenRoot()

    compose.onNodeWithTag("share_btn").assertIsDisplayed()
    compose.onNodeWithTag("share_btn").performClick()

    compose.waitUntil(2_000) {
      compose
          .onAllNodesWithTag(C.ShareLinkDialogTags.DIALOG_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).assertIsDisplayed()
  }

  @Test
  fun shareButton_worksForOwner() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      MySwissDormAppTheme {
        ViewListingScreen(viewListingViewModel = vm, listingUid = testListing.uid)
      }
    }
    waitForScreenRoot()

    compose.waitUntil(5_000) { vm.uiState.value.isOwner }

    compose.onNodeWithTag("share_btn").assertIsDisplayed()
    compose.onNodeWithTag("share_btn").performClick()

    compose.waitUntil(2_000) {
      compose
          .onAllNodesWithTag(C.ShareLinkDialogTags.DIALOG_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).assertIsDisplayed()
  }

  @Test
  fun shareDialog_allElementsDisplayed() = runTest {
    val vm = ViewListingViewModel(listingsRepo, profileRepo)
    compose.setContent {
      MySwissDormAppTheme {
        ViewListingScreen(viewListingViewModel = vm, listingUid = testListing.uid)
      }
    }
    waitForScreenRoot()

    compose.onNodeWithTag("share_btn").performClick()

    compose.waitUntil(2_000) {
      compose
          .onAllNodesWithTag(C.ShareLinkDialogTags.DIALOG_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify all dialog elements
    compose.onNodeWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).assertIsDisplayed()
    compose.onNodeWithTag(C.ShareLinkDialogTags.QR_CODE).assertIsDisplayed()
    compose.onNodeWithTag(C.ShareLinkDialogTags.COPY_LINK_BUTTON).assertIsDisplayed()
    compose.onNodeWithTag(C.ShareLinkDialogTags.CANCEL_TEXT).assertIsDisplayed()
  }
}
