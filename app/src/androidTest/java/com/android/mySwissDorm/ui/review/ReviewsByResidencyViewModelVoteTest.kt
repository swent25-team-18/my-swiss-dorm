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
import org.junit.Assert.assertNotNull
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

    // Wait for server update
    delay(1000)
    val finalState = vm.uiState.value
    val finalCard = finalState.reviews.items.find { it.reviewUid == review.uid }!!

    assertEquals(1, finalCard.netScore)
    assertEquals(VoteType.UPVOTE, finalCard.userVote)
  }

  @Test
  fun upvoteReviewTogglesOff() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review = reviewVortex1.copy(ownerId = ownerId, upvotedBy = listOf(voterId))
    ReviewsRepositoryProvider.repository.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex")

    waitForReviewsToLoad(vm)
    val initialCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(VoteType.UPVOTE, initialCard.userVote)

    vm.upvoteReview(review.uid)

    delay(1500)
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

    delay(1000)
    val finalCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(-1, finalCard.netScore)
    assertEquals(VoteType.DOWNVOTE, finalCard.userVote)
  }

  @Test
  fun upvoteSwitchesFromDownvote() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review = reviewVortex1.copy(ownerId = ownerId, downvotedBy = listOf(voterId))
    ReviewsRepositoryProvider.repository.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex")

    waitForReviewsToLoad(vm)
    val initialCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(VoteType.DOWNVOTE, initialCard.userVote)
    assertEquals(-1, initialCard.netScore)

    vm.upvoteReview(review.uid)

    delay(1500)
    val finalCard = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(VoteType.UPVOTE, finalCard.userVote)
    assertEquals(1, finalCard.netScore)
  }

  @Test
  fun voteMethodsComputeNetScoreCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review =
        reviewVortex1.copy(
            ownerId = ownerId, upvotedBy = listOf("user1", "user2"), downvotedBy = listOf("user3"))
    ReviewsRepositoryProvider.repository.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex")

    waitForReviewsToLoad(vm)
    val card = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }!!
    assertEquals(1, card.netScore) // 2 upvotes - 1 downvote = 1
  }

  @Test
  fun upvoteReviewWhenNotLoggedIn_doesNothing() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review = reviewVortex1.copy(ownerId = ownerId)
    ReviewsRepositoryProvider.repository.addReview(review)

    // Sign out to test early return
    FirebaseEmulator.auth.signOut()

    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex")
    waitForReviewsToLoad(vm)

    val initialState = vm.uiState.value
    val initialCard = initialState.reviews.items.find { it.reviewUid == review.uid }!!

    // Try to upvote when not logged in - should do nothing
    vm.upvoteReview(review.uid)

    delay(200)
    val afterState = vm.uiState.value
    val afterCard = afterState.reviews.items.find { it.reviewUid == review.uid }!!

    // State should be unchanged
    assertEquals(initialCard.netScore, afterCard.netScore)
    assertEquals(initialCard.userVote, afterCard.userVote)
  }

  @Test
  fun downvoteReviewWhenNotLoggedIn_doesNothing() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review = reviewVortex1.copy(ownerId = ownerId)
    ReviewsRepositoryProvider.repository.addReview(review)

    // Sign out to test early return
    FirebaseEmulator.auth.signOut()

    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex")
    waitForReviewsToLoad(vm)

    val initialState = vm.uiState.value
    val initialCard = initialState.reviews.items.find { it.reviewUid == review.uid }!!

    // Try to downvote when not logged in - should do nothing
    vm.downvoteReview(review.uid)

    delay(200)
    val afterState = vm.uiState.value
    val afterCard = afterState.reviews.items.find { it.reviewUid == review.uid }!!

    // State should be unchanged
    assertEquals(initialCard.netScore, afterCard.netScore)
    assertEquals(initialCard.userVote, afterCard.userVote)
  }

  @Test
  fun loadReviewsHandlesProfileFetchFailure() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Create a review with a valid ownerId
    val review = reviewVortex1.copy(ownerId = ownerId)
    ReviewsRepositoryProvider.repository.addReview(review)

    // Delete the profile to simulate a missing profile scenario
    ProfileRepositoryProvider.repository.deleteProfile(ownerId)

    switchToUser(FakeUser.FakeUser2)
    val vm = ReviewsByResidencyViewModel()
    vm.loadReviews("Vortex")

    // Wait for load to complete (should handle profile fetch failure gracefully)
    waitForReviewsToLoad(vm)

    // Should still load the review, but with "Unknown" as the name
    val card = vm.uiState.value.reviews.items.find { it.reviewUid == review.uid }
    assertNotNull("Review should be loaded even if profile fetch fails", card)
    assertEquals("Unknown", card!!.fullNameOfPoster)
  }
}
