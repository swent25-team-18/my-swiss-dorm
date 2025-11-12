package com.android.mySwissDorm.model.review

import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
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

  val review1 =
      Review(
          uid = repo.getNewUid(),
          ownerId = "",
          postedAt = Timestamp.now(),
          title = "First Title",
          reviewText = "My first review",
          grade = 5.0,
          residencyName = "Vortex",
          roomType = RoomType.STUDIO,
          pricePerMonth = 300.0,
          areaInM2 = 64,
          imageUrls = emptyList())

  val review2 =
      Review(
          uid = repo.getNewUid(),
          ownerId = "",
          postedAt = Timestamp.now(),
          title = "Second Title",
          reviewText = "My second review",
          grade = 4.0,
          residencyName = "Atrium",
          roomType = RoomType.APARTMENT,
          pricePerMonth = 500.0,
          areaInM2 = 32,
          imageUrls = emptyList())

  val review3 =
      Review(
          uid = repo.getNewUid(),
          ownerId = "",
          postedAt = Timestamp.now(),
          title = "Third Title",
          reviewText = "My third review",
          grade = 2.0,
          residencyName = "Vortex",
          roomType = RoomType.COLOCATION,
          pricePerMonth = 100.0,
          areaInM2 = 16,
          imageUrls = emptyList())

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
        review1.copy(
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
        review1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    val secondReview =
        review1.copy(
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
    val firstReview = review1.copy(ownerId = firstUserId)
    val secondReview = review2.copy(ownerId = firstUserId)
    repo.addReview(firstReview)
    repo.addReview(secondReview)

    switchToUser(FakeUser.FakeUser2)
    val secondUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val thirdReview = review3.copy(ownerId = secondUserId)
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
    val firstReview = review1.copy(ownerId = firstUserId)
    val secondReview = review2.copy(ownerId = firstUserId)
    repo.addReview(firstReview)
    repo.addReview(secondReview)

    switchToUser(FakeUser.FakeUser2)
    val secondUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val thirdReview = review3.copy(ownerId = secondUserId)
    repo.addReview(thirdReview)

    switchToUser(FakeUser.FakeUser1)
    assertEquals(
        listOf(firstReview, thirdReview).toSet(), repo.getAllReviewsByResidency("Vortex").toSet())
  }

  @Test
  fun editReviewWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review =
        review1.copy(
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
        review1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    val secondReview =
        review2.copy(
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
        review1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.addReview(review)
    assertEquals(true, runCatching { repo.getReview(review.uid) }.isSuccess)
    repo.deleteReview(review.uid)
    assertEquals(true, runCatching { repo.getReview(review.uid) }.isFailure)
  }
}
