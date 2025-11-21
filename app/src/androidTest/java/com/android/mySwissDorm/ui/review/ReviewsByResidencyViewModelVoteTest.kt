package com.android.mySwissDorm.ui.review

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
    vm.loadReviews("Vortex")

    // Wait for load to complete
    waitForReviewsToLoad(vm)
    val initialState = vm.uiState.value
    val initialCard = initialState.reviews.items.find { it.reviewUid == review.uid }!!

    // Perform upvote
    vm.upvoteReview(review.uid)

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
    vm.loadReviews("Vortex")

    waitForReviewsToLoad(vm)
    val initialCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(VoteType.UPVOTE, initialCard.userVote)

    vm.upvoteReview(review.uid)

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
    vm.loadReviews("Vortex")

    waitForReviewsToLoad(vm)
    val initialCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!

    vm.downvoteReview(review.uid)

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
    vm.loadReviews("Vortex")

    waitForReviewsToLoad(vm)
    val initialCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(VoteType.DOWNVOTE, initialCard.userVote)
    assertEquals(-1, initialCard.netScore)

    vm.upvoteReview(review.uid)

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
    vm.loadReviews("Vortex")

    waitForReviewsToLoad(vm)
    val card = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(1, card.netScore) // 2 upvotes - 1 downvote = 1
  }
}
