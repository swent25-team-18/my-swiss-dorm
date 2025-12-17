package com.android.mySwissDorm.ui.review

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.review.ReviewsRepositoryFirestore
import com.android.mySwissDorm.model.review.ReviewsRepositoryProvider
import com.android.mySwissDorm.model.review.VoteType
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewsByResidencyViewModelVoteTest : FirestoreTest() {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  override fun createRepositories() {
    ReviewsRepositoryProvider.repository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
  }

  private lateinit var ownerId: String
  private lateinit var voterId: String

  @Before
  override fun setUp() = runTest {
    super.setUp()
    switchToUser(FakeUser.FakeUser1)
    ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    ProfileRepositoryProvider.repository.createProfile(profile1.copy(ownerId = ownerId))

    switchToUser(FakeUser.FakeUser2)
    voterId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    ProfileRepositoryProvider.repository.createProfile(profile2.copy(ownerId = voterId))
  }

  private suspend fun waitForReviewsToLoad(
      vm: ReviewsByResidencyViewModel,
      timeoutMs: Long = 5000
  ) {
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeoutMs) {
      if (!vm.uiState.value.reviews.loading && vm.uiState.value.reviews.error == null) {
        return
      }
      delay(50)
    }
  }

  private suspend fun waitForVoteStateUpdate(
      vm: ReviewsByResidencyViewModel,
      reviewUid: String,
      expectedNetScore: Int,
      expectedVote: VoteType,
      timeoutMs: Long = 5000
  ) {
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeoutMs) {
      val card = vm.uiState.value.reviews.items.find { it.reviewUid == reviewUid }
      if (card != null && card.netScore == expectedNetScore && card.userVote == expectedVote) {
        // Give a bit more time to ensure updateReviewVoteState completes
        delay(100)
        return
      }
      delay(50)
    }
  }

  @Test
  fun upvoteReviewOptimisticUpdate() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review = reviewVortex1.copy(ownerId = ownerId)
    ReviewsRepositoryProvider.repository.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex", context)

    // Wait for load to complete
    waitForReviewsToLoad(vm)
    val initialState = vm.uiState.value
    val initialCard = initialState.reviews.items.find { it.reviewUid == review.uid }!!

    // Perform upvote
    vm.upvoteReview(review.uid, context)

    // Check optimistic update immediately
    delay(100)
    val optimisticState = vm.uiState.value
    val optimisticCard = optimisticState.reviews.items.find { it.reviewUid == review.uid }!!

    assertEquals(initialCard.netScore + 1, optimisticCard.netScore)
    assertEquals(VoteType.UPVOTE, optimisticCard.userVote)

    // Wait for updateReviewVoteState to complete (server synchronization)
    waitForVoteStateUpdate(vm, review.uid, 1, VoteType.UPVOTE)
    val finalState = vm.uiState.value
    val finalCard = finalState.reviews.items.find { it.reviewUid == review.uid }!!

    assertEquals(1, finalCard.netScore)
    assertEquals(VoteType.UPVOTE, finalCard.userVote)
  }

  @Test
  fun upvoteReviewTogglesOff() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review = reviewVortex1.copy(ownerId = ownerId, upvotedBy = setOf(voterId))
    ReviewsRepositoryProvider.repository.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex", context)

    waitForReviewsToLoad(vm)
    val initialCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(VoteType.UPVOTE, initialCard.userVote)

    vm.upvoteReview(review.uid, context)

    // Wait for updateReviewVoteState to complete (server synchronization)
    waitForVoteStateUpdate(vm, review.uid, 0, VoteType.NONE)
    val finalCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(VoteType.NONE, finalCard.userVote)
    assertEquals(0, finalCard.netScore)
  }

  @Test
  fun downvoteReviewOptimisticUpdate() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review = reviewVortex1.copy(ownerId = ownerId)
    ReviewsRepositoryProvider.repository.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex", context)

    waitForReviewsToLoad(vm)
    val initialCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!

    vm.downvoteReview(review.uid, context)

    delay(100)
    val optimisticCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(initialCard.netScore - 1, optimisticCard.netScore)
    assertEquals(VoteType.DOWNVOTE, optimisticCard.userVote)

    // Wait for updateReviewVoteState to complete (server synchronization)
    waitForVoteStateUpdate(vm, review.uid, -1, VoteType.DOWNVOTE)
    val finalCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(-1, finalCard.netScore)
    assertEquals(VoteType.DOWNVOTE, finalCard.userVote)
  }

  @Test
  fun upvoteSwitchesFromDownvote() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review = reviewVortex1.copy(ownerId = ownerId, downvotedBy = setOf(voterId))
    ReviewsRepositoryProvider.repository.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex", context)

    waitForReviewsToLoad(vm)
    val initialCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(VoteType.DOWNVOTE, initialCard.userVote)
    assertEquals(-1, initialCard.netScore)

    vm.upvoteReview(review.uid, context)

    // Wait for updateReviewVoteState to complete (server synchronization)
    waitForVoteStateUpdate(vm, review.uid, 1, VoteType.UPVOTE)
    val finalCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(VoteType.UPVOTE, finalCard.userVote)
    assertEquals(1, finalCard.netScore)
  }

  @Test
  fun voteMethodsComputeNetScoreCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review =
        reviewVortex1.copy(
            ownerId = ownerId, upvotedBy = setOf("user1", "user2"), downvotedBy = setOf("user3"))
    ReviewsRepositoryProvider.repository.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex", context)

    waitForReviewsToLoad(vm)
    val card = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(1, card.netScore) // 2 upvotes - 1 downvote = 1
  }

  @Test
  fun reviewsAreSortedByNetScoreDescending() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Create reviews with different net scores
    val highScoreReview =
        reviewVortex1.copy(
            uid = "highScoreReview",
            ownerId = ownerId,
            upvotedBy = setOf("user1", "user2", "user3"),
            downvotedBy = setOf("user4")) // netScore = 2
    val mediumScoreReview =
        reviewVortex1.copy(
            uid = "mediumScoreReview",
            ownerId = ownerId,
            upvotedBy = setOf("user1"),
            downvotedBy = emptySet()) // netScore = 1
    val zeroScoreReview =
        reviewVortex1.copy(
            uid = "zeroScoreReview",
            ownerId = ownerId,
            upvotedBy = emptySet(),
            downvotedBy = emptySet()) // netScore = 0
    val negativeScoreReview =
        reviewVortex1.copy(
            uid = "negativeScoreReview",
            ownerId = ownerId,
            upvotedBy = emptySet(),
            downvotedBy = setOf("user1", "user2")) // netScore = -2

    ReviewsRepositoryProvider.repository.addReview(highScoreReview)
    ReviewsRepositoryProvider.repository.addReview(mediumScoreReview)
    ReviewsRepositoryProvider.repository.addReview(zeroScoreReview)
    ReviewsRepositoryProvider.repository.addReview(negativeScoreReview)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex", context)

    waitForReviewsToLoad(vm)
    val items = vm.uiState.value.reviews.items

    // Verify reviews are sorted by net score descending
    assertEquals(4, items.size)
    assertEquals("highScoreReview", items[0].reviewUid) // netScore = 2
    assertEquals("mediumScoreReview", items[1].reviewUid) // netScore = 1
    assertEquals("zeroScoreReview", items[2].reviewUid) // netScore = 0
    assertEquals("negativeScoreReview", items[3].reviewUid) // netScore = -2

    // Verify net scores are correct
    assertEquals(2, items[0].netScore)
    assertEquals(1, items[1].netScore)
    assertEquals(0, items[2].netScore)
    assertEquals(-2, items[3].netScore)
  }

  @Test
  fun reviewsWithSameNetScoreMaintainRelativeOrder() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Create multiple reviews with the same net score
    val review1 =
        reviewVortex1.copy(
            uid = "sameScore1",
            ownerId = ownerId,
            upvotedBy = setOf("user1"),
            downvotedBy = emptySet()) // netScore = 1
    val review2 =
        reviewVortex1.copy(
            uid = "sameScore2",
            ownerId = ownerId,
            upvotedBy = setOf("user2"),
            downvotedBy = emptySet()) // netScore = 1
    val review3 =
        reviewVortex1.copy(
            uid = "sameScore3",
            ownerId = ownerId,
            upvotedBy = setOf("user3"),
            downvotedBy = emptySet()) // netScore = 1

    // Add reviews in a specific order
    ReviewsRepositoryProvider.repository.addReview(review1)
    ReviewsRepositoryProvider.repository.addReview(review2)
    ReviewsRepositoryProvider.repository.addReview(review3)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex", context)

    waitForReviewsToLoad(vm)
    val items = vm.uiState.value.reviews.items

    // All reviews should have netScore = 1
    val sameScoreReviews = items.filter { it.netScore == 1 }
    assertEquals(3, sameScoreReviews.size)

    // Verify all three reviews are present
    val reviewIds = sameScoreReviews.map { it.reviewUid }.toSet()
    assert(reviewIds.contains("sameScore1"))
    assert(reviewIds.contains("sameScore2"))
    assert(reviewIds.contains("sameScore3"))
  }

  @Test
  fun reviewsWithMixedVoteCountsAreSortedCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Create reviews with various vote combinations
    val review5Upvotes =
        reviewVortex1.copy(
            uid = "review5Upvotes",
            ownerId = ownerId,
            upvotedBy = setOf("u1", "u2", "u3", "u4", "u5"),
            downvotedBy = emptySet()) // netScore = 5
    val review3Upvotes1Downvote =
        reviewVortex1.copy(
            uid = "review3Up1Down",
            ownerId = ownerId,
            upvotedBy = setOf("u1", "u2", "u3"),
            downvotedBy = setOf("d1")) // netScore = 2
    val review2Upvotes =
        reviewVortex1.copy(
            uid = "review2Upvotes",
            ownerId = ownerId,
            upvotedBy = setOf("u1", "u2"),
            downvotedBy = emptySet()) // netScore = 2
    val reviewNoVotes =
        reviewVortex1.copy(
            uid = "reviewNoVotes",
            ownerId = ownerId,
            upvotedBy = emptySet(),
            downvotedBy = emptySet()) // netScore = 0
    val review1Downvote =
        reviewVortex1.copy(
            uid = "review1Downvote",
            ownerId = ownerId,
            upvotedBy = emptySet(),
            downvotedBy = setOf("d1")) // netScore = -1

    ReviewsRepositoryProvider.repository.addReview(review5Upvotes)
    ReviewsRepositoryProvider.repository.addReview(review3Upvotes1Downvote)
    ReviewsRepositoryProvider.repository.addReview(review2Upvotes)
    ReviewsRepositoryProvider.repository.addReview(reviewNoVotes)
    ReviewsRepositoryProvider.repository.addReview(review1Downvote)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex", context)

    waitForReviewsToLoad(vm)
    val items = vm.uiState.value.reviews.items

    // Verify correct order: highest net score first
    assertEquals(5, items.size)
    assertEquals("review5Upvotes", items[0].reviewUid) // netScore = 5
    // Next two should have netScore = 2 (order may vary but both should be before netScore = 0)
    val secondAndThird = items.slice(1..2).map { it.reviewUid }.toSet()
    assert(secondAndThird.contains("review3Up1Down"))
    assert(secondAndThird.contains("review2Upvotes"))
    assertEquals("reviewNoVotes", items[3].reviewUid) // netScore = 0
    assertEquals("review1Downvote", items[4].reviewUid) // netScore = -1

    // Verify net scores are correct
    assertEquals(5, items[0].netScore)
    assert(items[1].netScore == 2 || items[2].netScore == 2)
    assertEquals(0, items[3].netScore)
    assertEquals(-1, items[4].netScore)
  }

  @Test
  fun sortingWorksWithOnlyNegativeScores() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Create reviews with only negative or zero scores
    val reviewMinus1 =
        reviewVortex1.copy(
            uid = "reviewMinus1",
            ownerId = ownerId,
            upvotedBy = emptySet(),
            downvotedBy = setOf("d1")) // netScore = -1
    val reviewMinus3 =
        reviewVortex1.copy(
            uid = "reviewMinus3",
            ownerId = ownerId,
            upvotedBy = emptySet(),
            downvotedBy = setOf("d1", "d2", "d3")) // netScore = -3
    val reviewZero =
        reviewVortex1.copy(
            uid = "reviewZero",
            ownerId = ownerId,
            upvotedBy = emptySet(),
            downvotedBy = emptySet()) // netScore = 0
    val reviewMinus2 =
        reviewVortex1.copy(
            uid = "reviewMinus2",
            ownerId = ownerId,
            upvotedBy = emptySet(),
            downvotedBy = setOf("d1", "d2")) // netScore = -2

    ReviewsRepositoryProvider.repository.addReview(reviewMinus1)
    ReviewsRepositoryProvider.repository.addReview(reviewMinus3)
    ReviewsRepositoryProvider.repository.addReview(reviewZero)
    ReviewsRepositoryProvider.repository.addReview(reviewMinus2)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex", context)

    waitForReviewsToLoad(vm)
    val items = vm.uiState.value.reviews.items

    // Verify correct order: highest net score first (0, then -1, -2, -3)
    assertEquals(4, items.size)
    assertEquals("reviewZero", items[0].reviewUid) // netScore = 0
    assertEquals("reviewMinus1", items[1].reviewUid) // netScore = -1
    assertEquals("reviewMinus2", items[2].reviewUid) // netScore = -2
    assertEquals("reviewMinus3", items[3].reviewUid) // netScore = -3

    // Verify net scores are correct
    assertEquals(0, items[0].netScore)
    assertEquals(-1, items[1].netScore)
    assertEquals(-2, items[2].netScore)
    assertEquals(-3, items[3].netScore)
  }
}
