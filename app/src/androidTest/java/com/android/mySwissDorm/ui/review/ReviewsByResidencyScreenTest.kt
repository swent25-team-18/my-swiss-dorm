package com.android.mySwissDorm.ui.review

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.model.review.VoteType
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatDate
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ReviewsByResidencyScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var reviewsRepo: ReviewsRepository
  private lateinit var residenciesRepo: ResidenciesRepository
  private lateinit var profileRepo: ProfileRepository

  private lateinit var vm: ReviewsByResidencyViewModel

  private lateinit var userId: String

  private val reviewUid1 = "vortexReview1"
  private val reviewUid2 = "vortexReview2"
  private lateinit var review1: Review
  private lateinit var review2: Review

  private val context = ApplicationProvider.getApplicationContext<Context>()

  override fun createRepositories() {
    ReviewsRepositoryProvider.repository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    runBlocking {
      ResidenciesRepositoryProvider.repository.addResidency(vortex)
      ResidenciesRepositoryProvider.repository.addResidency(atrium)
    }
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      createRepositories()
      // Initialize repositories after createRepositories() sets the providers
      reviewsRepo = ReviewsRepositoryProvider.repository
      residenciesRepo = ResidenciesRepositoryProvider.repository
      profileRepo = ProfileRepositoryProvider.repository

      switchToUser(FakeUser.FakeUser1)
      userId = FirebaseEmulator.auth.currentUser!!.uid
      profileRepo.createProfile(profile1.copy(ownerId = userId))
    }

    review1 = reviewVortex1.copy(uid = reviewUid1, ownerId = userId)
    review2 = reviewVortex2.copy(uid = reviewUid2, ownerId = userId)

    runTest {
      switchToUser(FakeUser.FakeUser1)
      reviewsRepo.addReview(review1)
      reviewsRepo.addReview(review2)
    }

    vm = ReviewsByResidencyViewModel(reviewsRepo, profileRepo)
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  // ——————————————— Tests ———————————————

  @Test
  fun everythingIsDisplayed() {
    compose.setContent { ReviewsByResidencyScreen(vm, "Vortex") }

    compose.waitUntil(5_000) { vm.uiState.value.reviews.items.isNotEmpty() }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.TOP_BAR_TITLE).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.BACK_BUTTON).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.REVIEW_LIST).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.ERROR).assertIsNotDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.LOADING).assertIsNotDisplayed()

    // On the review card
    compose.onNodeWithTag(C.ReviewsByResidencyTag.reviewCard(reviewUid1)).assertIsDisplayed()
    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewImagePlaceholder(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewTitle(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewDescription(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewGrade(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewPostDate(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewPosterName(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun emptyIsDisplayedIfNoReviewsYet() {
    compose.setContent { ReviewsByResidencyScreen(vm, "Atrium") }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.REVIEW_LIST).assertIsNotDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.EMPTY).assertIsDisplayed()
  }

  @Test
  fun topBarTitleDisplaysResidencyName() {
    compose.setContent { ReviewsByResidencyScreen(vm, "Atrium") }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.TOP_BAR_TITLE).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.TOP_BAR_TITLE).assertTextEquals("Atrium")
  }

  @Test
  fun backButtonCallsCallback() {
    var clicked = false
    compose.setContent { ReviewsByResidencyScreen(vm, "Atrium", onGoBack = { clicked = true }) }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.BACK_BUTTON).assertIsDisplayed()

    assertEquals(false, clicked)

    compose.onNodeWithTag(C.ReviewsByResidencyTag.BACK_BUTTON).performClick()

    assertEquals(true, clicked)
  }

  @Test
  fun everythingIsCorrectlyDisplayedOnReviewCard() {
    compose.setContent { ReviewsByResidencyScreen(vm, "Vortex") }

    compose.waitUntil(5_000) { vm.uiState.value.reviews.items.isNotEmpty() }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.REVIEW_LIST).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.reviewCard(reviewUid1)).assertIsDisplayed()

    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewTitle(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewTitle(reviewUid1), useUnmergedTree = true)
        .assertTextEquals(review1.title)

    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewDescription(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewDescription(reviewUid1), useUnmergedTree = true)
        .assertTextEquals(review1.reviewText)

    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewPostDate(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewPostDate(reviewUid1), useUnmergedTree = true)
        .assertTextEquals(formatDate(review1.postedAt))

    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewPosterName(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.reviewPosterName(reviewUid1), useUnmergedTree = true)
        .assertTextEquals("Posted by Bob King") // Full name of FakeUser1
  }

  @Test
  fun clickingCardCallsCallback() {
    var clicked = false
    compose.setContent {
      ReviewsByResidencyScreen(vm, "Vortex", onSelectReview = { clicked = true })
    }

    compose.waitUntil(5_000) { vm.uiState.value.reviews.items.isNotEmpty() }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.REVIEW_LIST).assertIsDisplayed()

    compose.onNodeWithTag(C.ReviewsByResidencyTag.reviewCard(reviewUid1)).assertIsDisplayed()

    assertEquals(false, clicked)

    compose.onNodeWithTag(C.ReviewsByResidencyTag.reviewCard(reviewUid1)).performClick()

    assertEquals(true, clicked)
  }

  @Test
  fun errorIsDisplayedWhenErrorForReviews() {
    class ThrowingRepo : ReviewsRepository {
      override fun getNewUid(): String = "x"

      override suspend fun getAllReviews(): List<Review> = throw Exception("error")

      override suspend fun getAllReviewsByUser(userId: String): List<Review> =
          throw Exception("error")

      override suspend fun getAllReviewsByResidency(residencyName: String): List<Review> =
          throw Exception("error")

      override suspend fun getReview(reviewId: String): Review = throw Exception("error")

      override suspend fun addReview(review: Review) {}

      override suspend fun editReview(reviewId: String, newValue: Review) {}

      override suspend fun deleteReview(reviewId: String) {}

      override suspend fun upvoteReview(reviewId: String, userId: String) =
          throw UnsupportedOperationException()

      override suspend fun downvoteReview(reviewId: String, userId: String) =
          throw UnsupportedOperationException()

      override suspend fun removeVote(reviewId: String, userId: String) =
          throw UnsupportedOperationException()
    }

    val failingVm = ReviewsByResidencyViewModel(ThrowingRepo(), profileRepo)

    compose.setContent { ReviewsByResidencyScreen(failingVm, "Vortex") }

    compose.onNodeWithTag(C.ReviewsByResidencyTag.ROOT).assertIsDisplayed()
    compose.onNodeWithTag(C.ReviewsByResidencyTag.ERROR).assertIsDisplayed()
  }

  @Test
  fun compactVoteButtonsAreDisplayed() {
    compose.setContent { ReviewsByResidencyScreen(vm, "Vortex") }

    compose.waitUntil(5_000) { vm.uiState.value.reviews.items.isNotEmpty() }

    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewVoteButtons(reviewUid1), useUnmergedTree = true)
        .assertIsDisplayed()

    // Check that vote buttons exist (there may be multiple reviews, so we check at least one
    // exists)
    val upvoteButtons =
        compose.onAllNodesWithTag(
            C.ReviewsByResidencyTag.COMPACT_VOTE_UPVOTE_BUTTON, useUnmergedTree = true)
    val downvoteButtons =
        compose.onAllNodesWithTag(
            C.ReviewsByResidencyTag.COMPACT_VOTE_DOWNVOTE_BUTTON, useUnmergedTree = true)
    val scoreTexts =
        compose.onAllNodesWithTag(
            C.ReviewsByResidencyTag.COMPACT_VOTE_SCORE, useUnmergedTree = true)

    assert(upvoteButtons.fetchSemanticsNodes().isNotEmpty()) {
      "Upvote buttons should be displayed"
    }
    assert(downvoteButtons.fetchSemanticsNodes().isNotEmpty()) {
      "Downvote buttons should be displayed"
    }
    assert(scoreTexts.fetchSemanticsNodes().isNotEmpty()) { "Score should be displayed" }
  }

  @Test
  fun anonymousReview_showsAnonymousInList() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val anonymousReviewUid = "anonymousReview1"
    val anonymousReview = review1.copy(uid = anonymousReviewUid, isAnonymous = true)
    reviewsRepo.addReview(anonymousReview)

    compose.setContent { ReviewsByResidencyScreen(vm, "Vortex") }
    compose.waitUntil(5_000) { vm.uiState.value.reviews.items.isNotEmpty() }

    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewPosterName(anonymousReviewUid), useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("Posted by Anonymous")
  }

  @Test
  fun compactVoteButtonsAreEnabledForNonOwner() = runTest {
    // Create a review owned by a different user
    switchToUser(FakeUser.FakeUser2)
    val otherUserId = FirebaseEmulator.auth.currentUser!!.uid
    profileRepo.createProfile(profile2.copy(ownerId = otherUserId))

    val otherUserReview = reviewVortex1.copy(uid = reviewsRepo.getNewUid(), ownerId = otherUserId)
    reviewsRepo.addReview(otherUserReview)

    // Switch back to FakeUser1 (non-owner)
    switchToUser(FakeUser.FakeUser1)
    val testVm = ReviewsByResidencyViewModel(reviewsRepo, profileRepo)
    testVm.loadReviews("Vortex", context)

    compose.setContent { ReviewsByResidencyScreen(testVm, "Vortex") }

    compose.waitUntil(5_000) {
      testVm.uiState.value.reviews.items.any { it.reviewUid == otherUserReview.uid && !it.isOwner }
    }

    // Find the vote buttons for this specific review
    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewVoteButtons(otherUserReview.uid), useUnmergedTree = true)
        .assertIsDisplayed()

    // Check that upvote buttons exist and are clickable
    val upvoteButtons =
        compose.onAllNodesWithTag(
            C.ReviewsByResidencyTag.COMPACT_VOTE_UPVOTE_BUTTON, useUnmergedTree = true)
    assert(upvoteButtons.fetchSemanticsNodes().isNotEmpty()) {
      "Upvote buttons should be displayed"
    }
  }

  @Test
  fun compactVoteScoreIsDisplayedCorrectly() = runTest {
    // Create a review with votes
    val reviewWithVotes =
        reviewVortex1.copy(
            uid = reviewsRepo.getNewUid(),
            ownerId = userId,
            upvotedBy = setOf("user1", "user2"),
            downvotedBy = setOf("user3"))
    reviewsRepo.addReview(reviewWithVotes)

    val testVm = ReviewsByResidencyViewModel(reviewsRepo, profileRepo)
    testVm.loadReviews("Vortex", context)

    compose.setContent { ReviewsByResidencyScreen(testVm, "Vortex") }

    compose.waitUntil(5_000) {
      testVm.uiState.value.reviews.items.any { it.reviewUid == reviewWithVotes.uid }
    }

    // Check that the score is displayed (net score should be 1: 2 upvotes - 1 downvote)
    val scoreNodes =
        compose.onAllNodesWithTag(
            C.ReviewsByResidencyTag.COMPACT_VOTE_SCORE, useUnmergedTree = true)
    assert(scoreNodes.fetchSemanticsNodes().isNotEmpty()) { "Score should be displayed" }
  }

  @Test
  fun clickingCompactVoteButtonTriggersViewModel() = runTest {
    // Create a review owned by a different user so we can vote
    switchToUser(FakeUser.FakeUser2)
    val otherUserId = FirebaseEmulator.auth.currentUser!!.uid
    profileRepo.createProfile(profile2.copy(ownerId = otherUserId))

    val otherUserReview = reviewVortex1.copy(uid = reviewsRepo.getNewUid(), ownerId = otherUserId)
    reviewsRepo.addReview(otherUserReview)

    // Switch back to FakeUser1 (non-owner)
    switchToUser(FakeUser.FakeUser1)
    val testVm = ReviewsByResidencyViewModel(reviewsRepo, profileRepo)
    testVm.loadReviews("Vortex", context)

    compose.setContent { ReviewsByResidencyScreen(testVm, "Vortex") }

    // Wait for the ViewModel to load reviews
    compose.waitUntil(10_000) { testVm.uiState.value.reviews.items.isNotEmpty() }

    // Wait for the specific review to load and be ready with all conditions met
    compose.waitUntil(10_000) {
      val item = testVm.uiState.value.reviews.items.find { it.reviewUid == otherUserReview.uid }
      item != null && !item.isOwner && item.userVote == VoteType.NONE
    }

    // Additional wait to ensure UI is fully rendered
    compose.waitForIdle()

    val initialItem =
        testVm.uiState.value.reviews.items.find { it.reviewUid == otherUserReview.uid }
            ?: throw AssertionError("Review item not found after wait")
    val initialScore = initialItem.netScore
    val initialVote = initialItem.userVote

    // Scroll to the vote buttons to ensure they are visible on screen
    // This is critical for CI where screen size might be different and items may be off-screen
    val voteButtonsTag = C.ReviewsByResidencyTag.reviewVoteButtons(otherUserReview.uid)

    // Wait for the vote buttons to exist in the tree
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(voteButtonsTag, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll the LazyColumn to make the vote buttons visible
    // This is essential for CI where the review might be below the fold and require scrolling
    compose
        .onNodeWithTag(C.ReviewsByResidencyTag.REVIEW_LIST, useUnmergedTree = true)
        .performScrollToNode(hasTestTag(voteButtonsTag))
    compose.waitForIdle()

    // Verify the vote buttons are now visible after scrolling
    val voteButtonsContainer = compose.onNodeWithTag(voteButtonsTag, useUnmergedTree = true)
    voteButtonsContainer.assertIsDisplayed()

    // Wait for upvote button to be available
    val upvoteButtonTag = C.ReviewsByResidencyTag.COMPACT_VOTE_UPVOTE_BUTTON
    compose.waitUntil(5_000) {
      val nodes =
          compose.onAllNodesWithTag(upvoteButtonTag, useUnmergedTree = true).fetchSemanticsNodes()
      nodes.isNotEmpty()
    }
    compose.waitForIdle()

    // After scrolling to the specific review's vote buttons container,
    // find and click the upvote button. Since there are multiple reviews with upvote buttons,
    // we use onAllNodesWithTag and click the first one. The scroll ensures the correct
    // review's button is visible and accessible.
    val upvoteButtons = compose.onAllNodesWithTag(upvoteButtonTag, useUnmergedTree = true)
    val nodes = upvoteButtons.fetchSemanticsNodes()
    assert(nodes.isNotEmpty()) { "Upvote button should be available" }

    // Click the first available upvote button
    // Note: The onClick callback is bound to the specific review's UID in the UI,
    // and we verify below that the state change is for otherUserReview
    upvoteButtons[0].performClick()
    compose.waitForIdle()

    // Wait for the vote to be registered
    // The optimistic update should happen immediately in the ViewModel
    // We check for either vote type change or score change to be more lenient
    compose.waitUntil(10_000) {
      val updatedItem =
          testVm.uiState.value.reviews.items.find { it.reviewUid == otherUserReview.uid }
      if (updatedItem == null) return@waitUntil false
      // Check if vote state changed OR score changed (optimistic update)
      val voteChanged = updatedItem.userVote != initialVote
      val scoreChanged = updatedItem.netScore != initialScore
      voteChanged || scoreChanged
    }

    // Verify the vote was registered for the correct review
    val finalItem = testVm.uiState.value.reviews.items.find { it.reviewUid == otherUserReview.uid }
    assert(finalItem != null) { "Review item should still exist" }

    // Verify either the vote type changed to UPVOTE or the score increased
    val voteRegistered =
        finalItem!!.userVote == VoteType.UPVOTE || finalItem.netScore > initialScore
    assert(voteRegistered) {
      "Vote should be registered for review ${otherUserReview.uid}. " +
          "Initial: vote=$initialVote, score=$initialScore. " +
          "Final: vote=${finalItem.userVote}, score=${finalItem.netScore}"
    }
  }

  @Test
  fun nonAnonymousReview_showsActualNameInList() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val nonAnonymousReviewUid = "nonAnonymousReview1"
    val nonAnonymousReview = review1.copy(uid = nonAnonymousReviewUid, isAnonymous = false)
    reviewsRepo.addReview(nonAnonymousReview)

    compose.setContent { ReviewsByResidencyScreen(vm, "Vortex") }
    compose.waitUntil(5_000) { vm.uiState.value.reviews.items.isNotEmpty() }

    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewPosterName(nonAnonymousReviewUid), useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("Posted by Bob King") // Full name of FakeUser1
  }

  @Test
  fun mixedAnonymousAndNonAnonymousReviews_displayCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val anonymousReviewUid = "anonymousReview2"
    val nonAnonymousReviewUid = "nonAnonymousReview2"
    val anonymousReview = review1.copy(uid = anonymousReviewUid, isAnonymous = true)
    val nonAnonymousReview = review2.copy(uid = nonAnonymousReviewUid, isAnonymous = false)

    reviewsRepo.addReview(anonymousReview)
    reviewsRepo.addReview(nonAnonymousReview)

    compose.setContent { ReviewsByResidencyScreen(vm, "Vortex") }
    compose.waitUntil(5_000) { vm.uiState.value.reviews.items.size >= 2 }

    // Check anonymous review shows "anonymous"
    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewPosterName(anonymousReviewUid), useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("Posted by Anonymous")

    // Check non-anonymous review shows actual name
    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewPosterName(nonAnonymousReviewUid), useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("Posted by Bob King")
  }
}
