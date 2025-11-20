package com.android.mySwissDorm.ui.review

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
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
class ViewReviewViewModelVoteTest : FirestoreTest() {

  override fun createRepositories() {
    ReviewsRepositoryProvider.repository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
  }

  private lateinit var ownerId: String
  private lateinit var voterId: String

  @Before
  override fun setUp() = runTest {
    super.setUp()
    ResidenciesRepositoryProvider.repository.addResidency(vortex)

    switchToUser(FakeUser.FakeUser1)
    ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    ProfileRepositoryProvider.repository.createProfile(profile1.copy(ownerId = ownerId))

    switchToUser(FakeUser.FakeUser2)
    voterId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    ProfileRepositoryProvider.repository.createProfile(profile2.copy(ownerId = voterId))
  }

  private suspend fun waitForReviewToLoad(
      vm: ViewReviewViewModel,
      reviewId: String,
      timeoutMs: Long = 5000
  ) {
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeoutMs) {
      val state = vm.uiState.value
      if (state.review.uid == reviewId && state.errorMsg == null) {
        return
      }
      delay(50)
    }
  }

  private suspend fun waitForVoteStateUpdate(
      vm: ViewReviewViewModel,
      expectedNetScore: Int,
      expectedVote: VoteType,
      timeoutMs: Long = 5000
  ) {
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeoutMs) {
      val state = vm.uiState.value
      if (state.netScore == expectedNetScore &&
          state.userVote == expectedVote &&
          state.errorMsg == null) {
        // Give a bit more time to ensure updateVoteState completes
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
    val vm = ViewReviewViewModel()
    vm.loadReview(review.uid)

    waitForReviewToLoad(vm, review.uid)
    val initialState = vm.uiState.value
    assertEquals(0, initialState.netScore)
    assertEquals(VoteType.NONE, initialState.userVote)

    vm.upvoteReview()

    delay(100)
    val optimisticState = vm.uiState.value
    assertEquals(1, optimisticState.netScore)
    assertEquals(VoteType.UPVOTE, optimisticState.userVote)

    // Wait for updateVoteState to complete (server synchronization)
    waitForVoteStateUpdate(vm, 1, VoteType.UPVOTE)
    val finalState = vm.uiState.value
    assertEquals(1, finalState.netScore)
    assertEquals(VoteType.UPVOTE, finalState.userVote)
  }

  @Test
  fun downvoteReviewOptimisticUpdate() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review = reviewVortex1.copy(ownerId = ownerId)
    ReviewsRepositoryProvider.repository.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val vm = ViewReviewViewModel()
    vm.loadReview(review.uid)

    waitForReviewToLoad(vm, review.uid)
    vm.downvoteReview()

    delay(100)
    val optimisticState = vm.uiState.value
    assertEquals(-1, optimisticState.netScore)
    assertEquals(VoteType.DOWNVOTE, optimisticState.userVote)

    // Wait for updateVoteState to complete (server synchronization)
    waitForVoteStateUpdate(vm, -1, VoteType.DOWNVOTE)
    val finalState = vm.uiState.value
    assertEquals(-1, finalState.netScore)
    assertEquals(VoteType.DOWNVOTE, finalState.userVote)
  }

  @Test
  fun upvoteTogglesOff() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review = reviewVortex1.copy(ownerId = ownerId, upvotedBy = setOf(voterId))
    ReviewsRepositoryProvider.repository.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val vm = ViewReviewViewModel()
    vm.loadReview(review.uid)

    waitForReviewToLoad(vm, review.uid)
    assertEquals(VoteType.UPVOTE, vm.uiState.value.userVote)

    vm.upvoteReview()

    // Wait for updateVoteState to complete (server synchronization)
    waitForVoteStateUpdate(vm, 0, VoteType.NONE)
    assertEquals(VoteType.NONE, vm.uiState.value.userVote)
    assertEquals(0, vm.uiState.value.netScore)
  }

  @Test
  fun upvoteSwitchesFromDownvote() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review = reviewVortex1.copy(ownerId = ownerId, downvotedBy = setOf(voterId))
    ReviewsRepositoryProvider.repository.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val vm = ViewReviewViewModel()
    vm.loadReview(review.uid)

    waitForReviewToLoad(vm, review.uid)
    assertEquals(VoteType.DOWNVOTE, vm.uiState.value.userVote)
    assertEquals(-1, vm.uiState.value.netScore)

    vm.upvoteReview()

    // Wait for updateVoteState to complete (server synchronization)
    waitForVoteStateUpdate(vm, 1, VoteType.UPVOTE)
    assertEquals(VoteType.UPVOTE, vm.uiState.value.userVote)
    assertEquals(1, vm.uiState.value.netScore)
  }

  @Test
  fun loadReviewComputesVoteStateCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review =
        reviewVortex1.copy(
            ownerId = ownerId, upvotedBy = setOf("user1", voterId), downvotedBy = setOf("user2"))
    ReviewsRepositoryProvider.repository.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val vm = ViewReviewViewModel()
    vm.loadReview(review.uid)

    waitForReviewToLoad(vm, review.uid)
    val state = vm.uiState.value
    assertEquals(1, state.netScore) // 2 upvotes - 1 downvote = 1
    assertEquals(VoteType.UPVOTE, state.userVote) // voterId is in upvotedBy
  }
}
