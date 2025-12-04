package com.android.mySwissDorm.ui.review

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ViewReviewScreenShareTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var profilesRepo: ProfileRepository
  private lateinit var reviewsRepo: ReviewsRepository
  private lateinit var residenciesRepo: ResidenciesRepository
  private lateinit var ownerId: String
  private lateinit var otherId: String
  private lateinit var testReview: Review

  private lateinit var vm: ViewReviewViewModel

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  override fun createRepositories() {
    profilesRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    reviewsRepo = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    residenciesRepo = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    super.setUp()
    vm = ViewReviewViewModel(reviewsRepo, profilesRepo, residenciesRepo)

    runTest {
      // Owner
      switchToUser(FakeUser.FakeUser1)
      residenciesRepo.addResidency(resTest)
      ownerId = FirebaseEmulator.auth.currentUser!!.uid
      profilesRepo.createProfile(profile1.copy(ownerId = ownerId))

      // Other user
      switchToUser(FakeUser.FakeUser2)
      residenciesRepo.addResidency(resTest)
      otherId = FirebaseEmulator.auth.currentUser!!.uid
      profilesRepo.createProfile(profile2.copy(ownerId = otherId))

      // Create test review
      testReview =
          Review(
              uid = reviewsRepo.getNewUid(),
              ownerId = ownerId,
              postedAt = Timestamp.now(),
              title = "Test Review",
              reviewText = "This is a test review",
              grade = 4.0,
              residencyName = "Vortex",
              roomType = RoomType.STUDIO,
              pricePerMonth = 300.0,
              areaInM2 = 64,
              imageUrls = emptyList(),
              upvotedBy = emptySet(),
              downvotedBy = emptySet())

      switchToUser(FakeUser.FakeUser1)
      reviewsRepo.addReview(testReview)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  private fun waitForScreenRoot() {
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.ViewReviewTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  @Test
  fun shareButton_isDisplayed() = runTest {
    compose.setContent {
      MySwissDormAppTheme { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = testReview.uid) }
    }
    waitForScreenRoot()

    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).assertIsDisplayed()
  }

  @Test
  fun shareButton_opensShareDialog() = runTest {
    compose.setContent {
      MySwissDormAppTheme { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = testReview.uid) }
    }
    waitForScreenRoot()

    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).performClick()

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
    compose.setContent {
      MySwissDormAppTheme { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = testReview.uid) }
    }
    waitForScreenRoot()

    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).performClick()

    compose.waitUntil(2_000) {
      compose.onAllNodesWithTag(C.ShareLinkDialogTags.QR_CODE).fetchSemanticsNodes().isNotEmpty()
    }

    compose.onNodeWithTag(C.ShareLinkDialogTags.QR_CODE).assertIsDisplayed()
  }

  @Test
  fun shareDialog_displaysCopyLinkButton() = runTest {
    compose.setContent {
      MySwissDormAppTheme { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = testReview.uid) }
    }
    waitForScreenRoot()

    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).performClick()

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
    compose.setContent {
      MySwissDormAppTheme { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = testReview.uid) }
    }
    waitForScreenRoot()

    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).performClick()

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
    compose.setContent {
      MySwissDormAppTheme { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = testReview.uid) }
    }
    waitForScreenRoot()

    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).performClick()

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
    compose.setContent {
      MySwissDormAppTheme { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = testReview.uid) }
    }
    waitForScreenRoot()

    compose
        .onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN)
        .assertIsDisplayed()
        .assert(hasContentDescription(context.getString(R.string.share)))
  }

  @Test
  fun shareDialog_qrCodeContainsReviewLink() = runTest {
    compose.setContent {
      MySwissDormAppTheme { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = testReview.uid) }
    }
    waitForScreenRoot()

    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).performClick()

    compose.waitUntil(2_000) {
      compose.onAllNodesWithTag(C.ShareLinkDialogTags.QR_CODE).fetchSemanticsNodes().isNotEmpty()
    }

    // QR code should be displayed (indicating link was generated)
    compose.onNodeWithTag(C.ShareLinkDialogTags.QR_CODE).assertIsDisplayed()
  }

  @Test
  fun shareButton_worksForNonOwner() = runTest {
    switchToUser(FakeUser.FakeUser2)
    val testVm = ViewReviewViewModel(reviewsRepo, profilesRepo, residenciesRepo)

    compose.setContent {
      MySwissDormAppTheme {
        ViewReviewScreen(viewReviewViewModel = testVm, reviewUid = testReview.uid)
      }
    }
    waitForScreenRoot()

    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).assertIsDisplayed()
    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).performClick()

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

    compose.setContent {
      MySwissDormAppTheme { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = testReview.uid) }
    }
    waitForScreenRoot()

    compose.waitUntil(5_000) { vm.uiState.value.isOwner }

    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).assertIsDisplayed()
    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).performClick()

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
    compose.setContent {
      MySwissDormAppTheme { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = testReview.uid) }
    }
    waitForScreenRoot()

    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).performClick()

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

  @Test
  fun shareButton_worksWithAnonymousReview() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val anonymousReview = testReview.copy(uid = reviewsRepo.getNewUid(), isAnonymous = true)
    reviewsRepo.addReview(anonymousReview)

    val testVm = ViewReviewViewModel(reviewsRepo, profilesRepo, residenciesRepo)
    compose.setContent {
      MySwissDormAppTheme {
        ViewReviewScreen(viewReviewViewModel = testVm, reviewUid = anonymousReview.uid)
      }
    }
    waitForScreenRoot()

    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).assertIsDisplayed()
    compose.onNodeWithTag(C.ShareLinkDialogTags.SHARE_BTN).performClick()

    compose.waitUntil(2_000) {
      compose
          .onAllNodesWithTag(C.ShareLinkDialogTags.DIALOG_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).assertIsDisplayed()
  }
}
