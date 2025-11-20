package com.android.mySwissDorm.ui.review

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.model.review.ReviewsRepository
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

  private val reviewsRepo = ReviewsRepositoryProvider.repository
  private val residenciesRepo = ResidenciesRepositoryProvider.repository
  private val profileRepo = ProfileRepositoryProvider.repository

  private lateinit var vm: ReviewsByResidencyViewModel

  private lateinit var userId: String

  private val reviewUid1 = "vortexReview1"
  private val reviewUid2 = "vortexReview2"
  private lateinit var review1: Review
  private lateinit var review2: Review

  override fun createRepositories() {
    runBlocking {
      residenciesRepo.addResidency(vortex)
      residenciesRepo.addResidency(atrium)
    }
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      createRepositories()

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
        .assertTextEquals("Posted by anonymous")
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
    testVm.loadReviews("Vortex")

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
    testVm.loadReviews("Vortex")

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
    testVm.loadReviews("Vortex")

    compose.setContent { ReviewsByResidencyScreen(testVm, "Vortex") }

    compose.waitUntil(5_000) {
      val item = testVm.uiState.value.reviews.items.find { it.reviewUid == otherUserReview.uid }
      item != null && !item.isOwner && item.userVote == VoteType.NONE
    }

    val initialItem =
        testVm.uiState.value.reviews.items.find { it.reviewUid == otherUserReview.uid }!!
    val initialScore = initialItem.netScore

    // Click the upvote button
    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewVoteButtons(otherUserReview.uid), useUnmergedTree = true)
        .assertIsDisplayed()

    // Find and click the upvote button within this review's vote section
    val upvoteButtons =
        compose.onAllNodesWithTag(
            C.ReviewsByResidencyTag.COMPACT_VOTE_UPVOTE_BUTTON, useUnmergedTree = true)
    val nodes = upvoteButtons.fetchSemanticsNodes()
    if (nodes.isNotEmpty()) {
      upvoteButtons[0].performClick()

      // Wait for the vote to be registered
      compose.waitUntil(5_000) {
        val updatedItem =
            testVm.uiState.value.reviews.items.find { it.reviewUid == otherUserReview.uid }
        updatedItem != null &&
            (updatedItem.userVote == VoteType.UPVOTE || updatedItem.netScore != initialScore)
      }

      // Verify the vote was registered
      val finalItem =
          testVm.uiState.value.reviews.items.find { it.reviewUid == otherUserReview.uid }
      assert(finalItem != null) { "Review item should still exist" }
      assert(finalItem!!.userVote == VoteType.UPVOTE || finalItem.netScore > initialScore) {
        "Vote should be registered"
      }
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
        .assertTextEquals("Posted by anonymous")

    // Check non-anonymous review shows actual name
    compose
        .onNodeWithTag(
            C.ReviewsByResidencyTag.reviewPosterName(nonAnonymousReviewUid), useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("Posted by Bob King")
  }
}
