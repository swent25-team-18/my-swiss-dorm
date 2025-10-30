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
          ownerId = "ownerId",
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
          ownerId = "ownerId2",
          postedAt = Timestamp.now(),
          title = "Second Title",
          reviewText = "My second review",
          grade = 4.0,
          residencyName = "Atrium",
          roomType = RoomType.APARTMENT,
          pricePerMonth = 500.0,
          areaInM2 = 32,
          imageUrls = emptyList())

  val editedReview = review1.copy(title = "Modified Title", grade = 3.0)

  val allReviews = listOf(review1, review2)

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
    repo.addReview(review1)
    assertEquals(review1, repo.getReview(review1.uid))
  }

  @Test
  fun getAllReviewsWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    allReviews.forEach { repo.addReview(it) }
    assertEquals(allReviews.toSet(), repo.getAllReviews().toSet())
  }

  @Test
  fun editReviewWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    repo.addReview(review1)
    assertEquals(true, runCatching { repo.editReview(review1.uid, editedReview) }.isSuccess)
    assertEquals(editedReview, repo.getReview(review1.uid))
  }

  @Test
  fun editReviewButDifferentUidFails() = runTest {
    switchToUser(FakeUser.FakeUser1)
    repo.addReview(review1)
    assertEquals(true, runCatching { repo.editReview(review1.uid, review2) }.isFailure)
  }

  @Test
  fun deleteReviewWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    repo.addReview(review1)
    assertEquals(true, runCatching { repo.getReview(review1.uid) }.isSuccess)
    repo.deleteReview(review1.uid)
    assertEquals(true, runCatching { repo.getReview(review1.uid) }.isFailure)
  }
}
