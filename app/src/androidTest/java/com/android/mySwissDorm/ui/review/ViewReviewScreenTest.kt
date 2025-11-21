package com.android.mySwissDorm.ui.review

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.model.review.VoteType
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import java.util.Date
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ViewReviewScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var profilesRepo: ProfileRepository
  private lateinit var reviewsRepo: ReviewsRepository
  private lateinit var residenciesRepo: ResidenciesRepository
  private lateinit var ownerId: String
  private lateinit var otherId: String

  private lateinit var review1: Review
  private lateinit var review2: Review

  // Pre-seeded variants so tests don’t need runTest
  private lateinit var anonymousReviewOwned: Review
  private lateinit var nonAnonymousReviewOwned: Review

  private lateinit var vm: ViewReviewViewModel

  override fun createRepositories() {
    profilesRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    reviewsRepo = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    residenciesRepo = ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    super.setUp()
    createRepositories()
    vm = ViewReviewViewModel(reviewsRepo, profilesRepo)

    runTest {
      // Owner
      switchToUser(FakeUser.FakeUser1)
      residenciesRepo.addResidency(resTest)
      residenciesRepo.addResidency(resTest2)

      ownerId = FirebaseEmulator.auth.currentUser!!.uid
      profilesRepo.createProfile(profile1.copy(ownerId = ownerId))

      // Other user
      switchToUser(FakeUser.FakeUser2)
      residenciesRepo.addResidency(resTest)
      residenciesRepo.addResidency(resTest2)
      otherId = FirebaseEmulator.auth.currentUser!!.uid
      profilesRepo.createProfile(profile2.copy(ownerId = otherId))

      // Base reviews
      review1 =
          Review(
              uid = reviewsRepo.getNewUid(),
              ownerId = ownerId,
              postedAt = Timestamp.now(),
              title = "First Title",
              reviewText = "My first review",
              grade = 3.5,
              residencyName = "Vortex",
              roomType = RoomType.STUDIO,
              pricePerMonth = 300.0,
              areaInM2 = 64,
              imageUrls = emptyList(),
              upvotedBy = emptySet(),
              downvotedBy = emptySet())
      review2 =
          Review(
              uid = reviewsRepo.getNewUid(),
              ownerId = otherId,
              postedAt = Timestamp(Date(1678886400000L)),
              title = "Second Title",
              reviewText = "My second review",
              grade = 4.5,
              residencyName = "Atrium",
              roomType = RoomType.APARTMENT,
              pricePerMonth = 500.0,
              areaInM2 = 32,
              imageUrls = emptyList(),
              upvotedBy = emptySet(),
              downvotedBy = emptySet())

      reviewsRepo.addReview(review2)

      // Switch back to owner and add his reviews
      switchToUser(FakeUser.FakeUser1)
      reviewsRepo.addReview(review1)

      // Pre-seed an anonymous and a non-anonymous copy owned by the same user
      anonymousReviewOwned = review1.copy(uid = reviewsRepo.getNewUid(), isAnonymous = true)
      nonAnonymousReviewOwned = review1.copy(uid = reviewsRepo.getNewUid(), isAnonymous = false)

      reviewsRepo.addReview(anonymousReviewOwned)
      reviewsRepo.addReview(nonAnonymousReviewOwned)
    }
  }

  private fun setOwnerReview() {
    compose.setContent { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = review1.uid) }
  }

  private fun setOtherReview() {
    compose.setContent { ViewReviewScreen(viewReviewViewModel = vm, reviewUid = review2.uid) }
  }

  private fun setAnonymousOwnerReview() {
    compose.setContent {
      ViewReviewScreen(viewReviewViewModel = vm, reviewUid = anonymousReviewOwned.uid)
    }
  }

  private fun setNonAnonymousOwnerReview() {
    compose.setContent {
      ViewReviewScreen(viewReviewViewModel = vm, reviewUid = nonAnonymousReviewOwned.uid)
    }
  }

  /** Wait until the screen root exists (first composition done). */
  private fun waitForScreenRoot() {
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.ViewReviewTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  /** Scroll inside the list/root to reveal a child with [childTag]. */
  private fun scrollListTo(childTag: String) {
    compose.onNodeWithTag(C.ViewReviewTags.ROOT).performScrollToNode(hasTestTag(childTag))
  }

  // -------------------- TESTS --------------------

  @Test
  fun everythingIsDisplayed() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    compose.onNodeWithTag(C.ViewReviewTags.ROOT, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.TITLE)
    compose.onNodeWithTag(C.ViewReviewTags.TITLE, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.POSTED_BY)
    compose.onNodeWithTag(C.ViewReviewTags.POSTED_BY, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.BULLETS)
    compose.onNodeWithTag(C.ViewReviewTags.BULLETS, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.REVIEW_TEXT)
    compose.onNodeWithTag(C.ViewReviewTags.REVIEW_TEXT, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.PHOTOS)
    compose.onNodeWithTag(C.ViewReviewTags.PHOTOS, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.LOCATION)
    compose.onNodeWithTag(C.ViewReviewTags.LOCATION, useUnmergedTree = true).assertIsDisplayed()
    scrollListTo(C.ViewReviewTags.EDIT_BTN)
    compose.onNodeWithTag(C.ViewReviewTags.EDIT_BTN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun mapPreview_isDisplayed_whenLocationIsValid() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.locationOfReview.latitude != 0.0 &&
          vm.uiState.value.locationOfReview.longitude != 0.0
    }
    scrollListTo(C.ViewReviewTags.LOCATION)
    compose.onNodeWithTag(C.ViewReviewTags.LOCATION).assertIsDisplayed()
    compose.onNodeWithText("LOCATION (Not available)").assertDoesNotExist()
  }

  @Test
  fun mapPlaceholder_isDisplayed_whenLocationIsInvalid() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val resNoLocation =
        resTest.copy(
            name = "No Location",
            location = Location(name = "No Location", latitude = 0.0, longitude = 0.0))
    residenciesRepo.addResidency(resNoLocation)

    val reviewNoLocation =
        review1.copy(uid = reviewsRepo.getNewUid(), residencyName = "No Location")
    reviewsRepo.addReview(reviewNoLocation)
    compose.setContent {
      ViewReviewScreen(viewReviewViewModel = vm, reviewUid = reviewNoLocation.uid)
    }
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.review.uid == reviewNoLocation.uid &&
          vm.uiState.value.locationOfReview.latitude == 0.0 &&
          vm.uiState.value.locationOfReview.longitude == 0.0
    }
    scrollListTo(C.ViewReviewTags.LOCATION)
    compose.onNodeWithTag(C.ViewReviewTags.LOCATION).assertIsDisplayed()
    compose.onNodeWithText("LOCATION (Not available)").assertIsDisplayed()
  }

  @Test
  fun mapClick_triggers_onViewMapCallback_withCorrectData() {
    var callbackCalled = false
    var capturedLat: Double? = null
    var capturedLon: Double? = null
    var capturedTitle: String? = null
    var capturedName: String? = null
    compose.setContent {
      ViewReviewScreen(
          viewReviewViewModel = vm,
          reviewUid = review1.uid,
          onViewMap = { lat, lon, title, name ->
            callbackCalled = true
            capturedLat = lat
            capturedLon = lon
            capturedTitle = title
            capturedName = name
          })
    }
    waitForScreenRoot()
    compose.waitUntil(10_000) {
      vm.uiState.value.review.uid == review1.uid &&
          vm.uiState.value.locationOfReview.latitude != 0.0
    }
    val expectedLocation = vm.uiState.value.locationOfReview
    scrollListTo(C.ViewReviewTags.LOCATION)
    compose.onNodeWithTag(C.ViewReviewTags.LOCATION).performClick()
    compose.waitForIdle()
    assert(callbackCalled) { "onViewMap callback was not triggered." }
    assertEquals(expectedLocation.latitude, capturedLat)
    assertEquals(expectedLocation.longitude, capturedLon)
    assertEquals(review1.title, capturedTitle)
    assertEquals("Review", capturedName)
  }

  @Test
  fun showsEditBtnWhenOwner() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    scrollListTo(C.ViewReviewTags.EDIT_BTN)
    compose.onNodeWithTag(C.ViewReviewTags.EDIT_BTN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun doNotSeeEditBtnWhenNotOwner() {
    setOtherReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review2.uid }
    compose.onNodeWithTag(C.ViewReviewTags.EDIT_BTN, useUnmergedTree = true).assertIsNotDisplayed()
  }

  @Test
  fun showsPostedByYouWhenOwner() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.review.uid == review1.uid && vm.uiState.value.fullNameOfPoster.isNotBlank()
    }
    scrollListTo(C.ViewReviewTags.POSTED_BY)
    compose
        .onNodeWithTag(C.ViewReviewTags.POSTED_BY)
        .assertIsDisplayed()
        .assertTextContains("(You)", substring = true)
  }

  @Test
  fun doNotShowsPostedByYouWhenNotOwner() = runTest {
    // Ensure we're viewing as FakeUser1 (not the owner of review2)
    switchToUser(FakeUser.FakeUser1)
    val testVm = ViewReviewViewModel(reviewsRepo, profilesRepo, residenciesRepo)
    compose.setContent { ViewReviewScreen(viewReviewViewModel = testVm, reviewUid = review2.uid) }
    waitForScreenRoot()
    compose.waitUntil(10_000) { testVm.uiState.value.review.uid == review2.uid }
    // Wait for fullNameOfPoster to be loaded
    compose.waitUntil(10_000) { testVm.uiState.value.fullNameOfPoster.isNotEmpty() }
    val postedByNode = compose.onNodeWithTag(C.ViewReviewTags.POSTED_BY)
    postedByNode.assertIsDisplayed()
    postedByNode.assertTextContains("Posted by", substring = true)
    postedByNode.assertTextContains(testVm.uiState.value.fullNameOfPoster, substring = true)
    // Verify "(You)" is NOT present by checking the text doesn't contain it
    val text =
        postedByNode
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.firstOrNull()
            ?.text ?: ""
    assert(!text.contains("(You)")) { "Text should not contain '(You)' but was: $text" }
  }

  @Test
  fun showsCorrectNumberOfStars() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    compose.onAllNodesWithTag(C.ViewReviewTags.FILLED_STAR).assertCountEquals(3)
    compose.onAllNodesWithTag(C.ViewReviewTags.HALF_STAR).assertCountEquals(1)
    compose.onAllNodesWithTag(C.ViewReviewTags.EMPTY_STAR).assertCountEquals(1)
  }

  @Test
  fun editButton_calls_onEditCallback() {
    var editCalled = false
    compose.setContent {
      ViewReviewScreen(
          viewReviewViewModel = vm, reviewUid = review1.uid, onEdit = { editCalled = true })
    }
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    scrollListTo(C.ViewReviewTags.EDIT_BTN)
    compose.onNodeWithTag(C.ViewReviewTags.EDIT_BTN, useUnmergedTree = true).performClick()
    assert(editCalled) { "onEdit callback was not triggered." }
  }

  @Test
  fun backButton_calls_onGoBackCallback() {
    var backCalled = false
    compose.setContent {
      ViewReviewScreen(
          viewReviewViewModel = vm, reviewUid = review1.uid, onGoBack = { backCalled = true })
    }
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    compose.onNodeWithText("Review Details").assertIsDisplayed()
    // Click back button via content description
    compose.onNodeWithContentDescription("Back").performClick()
    assert(backCalled) { "onGoBack callback was not triggered." }
  }

  @Test
  fun postedBy_click_calls_onViewProfileCallback() {
    var profileCalled = false
    var capturedOwnerId: String? = null
    compose.setContent {
      ViewReviewScreen(
          viewReviewViewModel = vm,
          reviewUid = review1.uid,
          onViewProfile = { ownerId ->
            profileCalled = true
            capturedOwnerId = ownerId
          })
    }
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    scrollListTo(C.ViewReviewTags.POSTED_BY)
    // Click on the posted by text (which contains the name)
    compose.onNodeWithTag(C.ViewReviewTags.POSTED_BY).performClick()
    assert(profileCalled) { "onViewProfile callback was not triggered." }
    assertEquals(ownerId, capturedOwnerId)
  }

  @Test
  fun errorMsg_triggers_onGoBack_and_showsToast() = runTest {
    var backCalled = false
    // Create a ViewModel that will fail to load
    val failingVm = ViewReviewViewModel(reviewsRepo, profilesRepo, residenciesRepo)
    // Try to load a non-existent review to trigger error
    compose.setContent {
      ViewReviewScreen(
          viewReviewViewModel = failingVm,
          reviewUid = "non-existent-review",
          onGoBack = { backCalled = true })
    }
    waitForScreenRoot()
    // Wait for error to be set and handled
    compose.waitUntil(10_000) { backCalled || failingVm.uiState.value.errorMsg != null }
    // The error should trigger onGoBack
    assert(backCalled) { "onGoBack should be called when error occurs." }
  }

  @Test
  fun displaysReviewContent_correctly() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    // Wait a bit more for content to be fully rendered
    compose.waitUntil(5_000) { vm.uiState.value.review.title == "First Title" }
    scrollListTo(C.ViewReviewTags.TITLE)
    compose.onNodeWithTag(C.ViewReviewTags.TITLE, useUnmergedTree = true).assertIsDisplayed()
    // Use onNodeWithText to find text directly
    compose.onNodeWithText("First Title").assertIsDisplayed()
    // The text is "Review :" with a space before the colon
    compose.onNodeWithText("Review :").assertIsDisplayed()
    compose.onNodeWithText("My first review").assertIsDisplayed()
  }

  @Test
  fun displaysBulletPoints_correctly() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review1.uid }
    // Wait for content to be fully rendered
    compose.waitUntil(5_000) { vm.uiState.value.review.roomType == RoomType.STUDIO }
    scrollListTo(C.ViewReviewTags.BULLETS)
    compose.onNodeWithTag(C.ViewReviewTags.BULLETS, useUnmergedTree = true).assertIsDisplayed()
    // RoomType.toString() returns "Studio" (not "STUDIO")
    // Use onNodeWithText to find text directly - this works better for text in nested composables
    compose.onNodeWithText("Studio", substring = true).assertIsDisplayed()
    // The format is "${review.pricePerMonth}.-/month" so it will be "300.0.-/month"
    compose.onNodeWithText("300.0", substring = true).assertIsDisplayed()
    compose.onNodeWithText("/month", substring = true).assertIsDisplayed()
    // The format is "${review.areaInM2}m²" so it will be "64m²"
    compose.onNodeWithText("64", substring = true).assertIsDisplayed()
    compose.onNodeWithText("m²", substring = true).assertIsDisplayed()
  }

  @Test
  fun errorMsg_showsToast_and_clearsError() = runTest {
    var backCalled = false
    val failingVm = ViewReviewViewModel(reviewsRepo, profilesRepo, residenciesRepo)
    compose.setContent {
      ViewReviewScreen(
          viewReviewViewModel = failingVm,
          reviewUid = "non-existent-review-${System.currentTimeMillis()}",
          onGoBack = { backCalled = true })
    }
    waitForScreenRoot()
    // Wait for error to be set and LaunchedEffect to trigger
    compose.waitUntil(10_000) { backCalled && failingVm.uiState.value.errorMsg == null }
    // Verify error was cleared after showing toast
    assert(backCalled) { "onGoBack should be called when error occurs." }
  }

  @Test
  fun locationPlaceholder_shown_whenLocationIsZero() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val resNoLocation =
        resTest.copy(
            name = "ZeroLocation",
            location = Location(name = "ZeroLocation", latitude = 0.0, longitude = 0.0))
    residenciesRepo.addResidency(resNoLocation)
    delay(200)

    val reviewNoLocation =
        review1.copy(uid = reviewsRepo.getNewUid(), residencyName = "ZeroLocation")
    reviewsRepo.addReview(reviewNoLocation)
    delay(200)

    val testVm = ViewReviewViewModel(reviewsRepo, profilesRepo, residenciesRepo)
    compose.setContent {
      ViewReviewScreen(viewReviewViewModel = testVm, reviewUid = reviewNoLocation.uid)
    }
    waitForScreenRoot()
    compose.waitUntil(10_000) {
      testVm.uiState.value.review.uid == reviewNoLocation.uid &&
          testVm.uiState.value.locationOfReview.latitude == 0.0 &&
          testVm.uiState.value.locationOfReview.longitude == 0.0
    }
    scrollListTo(C.ViewReviewTags.LOCATION)
    compose.onNodeWithTag(C.ViewReviewTags.LOCATION).assertIsDisplayed()
    compose.onNodeWithText("LOCATION (Not available)").assertIsDisplayed()
  }

  @Test
  fun voteButtonsAreDisplayed() {
    setOtherReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == review2.uid }
    scrollListTo(C.ViewReviewTags.VOTE_BUTTONS)
    compose.onNodeWithTag(C.ViewReviewTags.VOTE_BUTTONS, useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("Was this review helpful?").assertIsDisplayed()
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_UPVOTE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_DOWNVOTE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
    compose.onNodeWithTag(C.ViewReviewTags.VOTE_SCORE, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun voteButtonsAreEnabledForNonOwner() {
    setOtherReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.review.uid == review2.uid && !vm.uiState.value.isOwner
    }
    scrollListTo(C.ViewReviewTags.VOTE_BUTTONS)
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_UPVOTE_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_DOWNVOTE_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
  }

  @Test
  fun voteButtonsAreDisabledForOwner() {
    setOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.review.uid == review1.uid && vm.uiState.value.isOwner
    }
    scrollListTo(C.ViewReviewTags.VOTE_BUTTONS)
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_UPVOTE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertIsNotEnabled()
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_DOWNVOTE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertIsNotEnabled()
  }

  @Test
  fun voteScoreIsDisplayedCorrectly() = runTest {
    // Create a review with votes
    // review2 is owned by otherId (FakeUser2), so we need to switch to that user to add it
    switchToUser(FakeUser.FakeUser2)
    val reviewWithVotes =
        review2.copy(
            uid = reviewsRepo.getNewUid(),
            upvotedBy = setOf("user1", "user2"),
            downvotedBy = setOf("user3"))
    reviewsRepo.addReview(reviewWithVotes)

    // Switch back to FakeUser1 to view the review
    switchToUser(FakeUser.FakeUser1)

    compose.setContent {
      ViewReviewScreen(viewReviewViewModel = vm, reviewUid = reviewWithVotes.uid)
    }
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == reviewWithVotes.uid }
    scrollListTo(C.ViewReviewTags.VOTE_SCORE)
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_SCORE, useUnmergedTree = true)
        .assertTextEquals("1") // 2 upvotes - 1 downvote = 1
  }

  @Test
  fun clickingUpvoteButtonTriggersViewModel() = runTest {
    setOtherReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.review.uid == review2.uid && vm.uiState.value.userVote == VoteType.NONE
    }

    val initialScore = vm.uiState.value.netScore
    scrollListTo(C.ViewReviewTags.VOTE_UPVOTE_BUTTON)
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_UPVOTE_BUTTON, useUnmergedTree = true)
        .performClick()

    // Wait for optimistic update
    compose.waitUntil(5_000) {
      vm.uiState.value.userVote == VoteType.UPVOTE || vm.uiState.value.netScore != initialScore
    }

    // Verify the vote was registered
    assert(vm.uiState.value.userVote == VoteType.UPVOTE || vm.uiState.value.netScore > initialScore)
  }

  @Test
  fun clickingDownvoteButtonTriggersViewModel() = runTest {
    setOtherReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) {
      vm.uiState.value.review.uid == review2.uid && vm.uiState.value.userVote == VoteType.NONE
    }

    val initialScore = vm.uiState.value.netScore
    scrollListTo(C.ViewReviewTags.VOTE_DOWNVOTE_BUTTON)
    compose
        .onNodeWithTag(C.ViewReviewTags.VOTE_DOWNVOTE_BUTTON, useUnmergedTree = true)
        .performClick()

    // Wait for optimistic update
    compose.waitUntil(5_000) {
      vm.uiState.value.userVote == VoteType.DOWNVOTE || vm.uiState.value.netScore != initialScore
    }

    // Verify the vote was registered
    assert(
        vm.uiState.value.userVote == VoteType.DOWNVOTE || vm.uiState.value.netScore < initialScore)
  }

  @Test
  fun anonymousReview_showsAnonymousInsteadOfName() {
    setAnonymousOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == anonymousReviewOwned.uid }

    scrollListTo(C.ViewReviewTags.POSTED_BY)

    val postedByText =
        compose
            .onNodeWithTag(C.ViewReviewTags.POSTED_BY)
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.firstOrNull()
            ?.text ?: ""

    assertTrue(
        "Should contain 'Posted by anonymous'",
        postedByText.contains("Posted by anonymous", ignoreCase = true))
    // After the fix, "(You)" should always show for the owner, even if anonymous
    assertTrue(
        "Should contain '(You)' for owner's anonymous review",
        postedByText.contains("(You)", ignoreCase = true))
  }

  @Test
  fun anonymousReview_showsYouTagWhenOwner() {
    setAnonymousOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == anonymousReviewOwned.uid }

    scrollListTo(C.ViewReviewTags.POSTED_BY)
    val postedByText =
        compose
            .onNodeWithTag(C.ViewReviewTags.POSTED_BY)
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.firstOrNull()
            ?.text ?: ""

    assertTrue("Should contain 'anonymous'", postedByText.contains("anonymous", ignoreCase = true))
    // After the fix, "(You)" should always show for the owner, even if anonymous
    assertTrue(
        "Should contain '(You)' when owner views their anonymous review",
        postedByText.contains("(You)", ignoreCase = true))
  }

  @Test
  fun nonAnonymousReview_showsActualName() {
    setNonAnonymousOwnerReview()
    waitForScreenRoot()
    compose.waitUntil(5_000) { vm.uiState.value.review.uid == nonAnonymousReviewOwned.uid }

    scrollListTo(C.ViewReviewTags.POSTED_BY)
    val postedByText =
        compose
            .onNodeWithTag(C.ViewReviewTags.POSTED_BY)
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.firstOrNull()
            ?.text ?: ""

    assertTrue(
        "Should contain actual name, not 'anonymous'",
        !postedByText.contains("anonymous", ignoreCase = true))
    assertTrue(
        "Should contain user name",
        postedByText.contains("Bob", ignoreCase = true) ||
            postedByText.contains("King", ignoreCase = true))
  }
}
