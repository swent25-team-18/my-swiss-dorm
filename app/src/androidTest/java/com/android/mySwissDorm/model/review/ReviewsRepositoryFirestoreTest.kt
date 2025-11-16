package com.android.mySwissDorm.model.review

import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ReviewsRepositoryFirestoreTest : FirestoreTest() {
  override fun createRepositories() {
    ReviewsRepositoryProvider.repository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
  }

  private val repo = ReviewsRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun getNewUidReturnsUniqueIDs() = runTest {
    val numberIDs = 100
    val uids = (0 until numberIDs).toSet<Int>().map { repo.getNewUid() }.toSet()
    TestCase.assertEquals(uids.size, numberIDs)
  }

  @Test
  fun getFailsWithNotAddedUid() = runTest {
    switchToUser(FakeUser.FakeUser1)
    assertEquals(true, runCatching { repo.getReview("abc") }.isFailure)
  }

  @Test
  fun canAddAndGetReviewFromRepository() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val reviewToAdd =
        reviewVortex1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.addReview(reviewToAdd)
    assertEquals(reviewToAdd, repo.getReview(reviewToAdd.uid))
  }

  @Test
  fun getAllReviewsWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val firstReview =
        reviewVortex1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    val secondReview =
        reviewVortex2.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    val allReviews = listOf(firstReview, secondReview)
    allReviews.forEach { repo.addReview(it) }
    assertEquals(allReviews.toSet(), repo.getAllReviews().toSet())
  }

  @Test
  fun getAllReviewsByUserWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val firstUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val firstReview = reviewVortex1.copy(ownerId = firstUserId)
    val secondReview = reviewWoko1.copy(ownerId = firstUserId)
    repo.addReview(firstReview)
    repo.addReview(secondReview)

    switchToUser(FakeUser.FakeUser2)
    val secondUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val thirdReview = reviewVortex2.copy(ownerId = secondUserId)
    repo.addReview(thirdReview)

    switchToUser(FakeUser.FakeUser1)
    assertEquals(
        listOf(firstReview, secondReview).toSet(), repo.getAllReviewsByUser(firstUserId).toSet())
  }

  @Test
  fun getAllReviewsByResidencyWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val firstUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val firstReview = reviewVortex1.copy(ownerId = firstUserId)
    val secondReview = reviewWoko1.copy(ownerId = firstUserId)
    repo.addReview(firstReview)
    repo.addReview(secondReview)

    switchToUser(FakeUser.FakeUser2)
    val secondUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val thirdReview = reviewVortex2.copy(ownerId = secondUserId)
    repo.addReview(thirdReview)

    switchToUser(FakeUser.FakeUser1)
    assertEquals(
        listOf(firstReview, thirdReview).toSet(), repo.getAllReviewsByResidency("Vortex").toSet())
  }

  @Test
  fun editReviewWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review =
        reviewVortex1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    val editedReview =
        review.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            title = "Modified Title",
            grade = 3.0)
    repo.addReview(review)
    assertEquals(true, runCatching { repo.editReview(review.uid, editedReview) }.isSuccess)
    assertEquals(editedReview, repo.getReview(review.uid))
  }

  @Test
  fun editReviewButDifferentUidFails() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val firstReview =
        reviewVortex1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    val secondReview =
        reviewVortex2.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.addReview(firstReview)
    assertEquals(true, runCatching { repo.editReview(firstReview.uid, secondReview) }.isFailure)
  }

  @Test
  fun deleteReviewWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review =
        reviewVortex1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.addReview(review)
    assertEquals(true, runCatching { repo.getReview(review.uid) }.isSuccess)
    repo.deleteReview(review.uid)
    assertEquals(true, runCatching { repo.getReview(review.uid) }.isFailure)
  }

  @Test
  fun voteListsArePersistedAndLoaded() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    val original =
        reviewVortex1.copy(
            ownerId = ownerId,
            upvotedBy = listOf(ownerId, "other-user"),
            downvotedBy = listOf("downvoter-1"))

    repo.addReview(original)
    val loaded = repo.getReview(original.uid)

    assertEquals(original.upvotedBy, loaded.upvotedBy)
    assertEquals(original.downvotedBy, loaded.downvotedBy)
  }
}
