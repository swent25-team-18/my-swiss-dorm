package com.android.mySwissDorm.model.review

import androidx.room.Room
import com.android.mySwissDorm.model.database.AppDatabase
import com.android.mySwissDorm.model.rental.RoomType
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReviewsRepositoryLocalTest {
  private lateinit var database: AppDatabase
  private lateinit var repository: ReviewsRepositoryLocal

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    repository = ReviewsRepositoryLocal(database.reviewDao(), database)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun getNewUid_throwsException() {
    assertThrows(UnsupportedOperationException::class.java) { repository.getNewUid() }
  }

  @Test
  fun addAndGetReview_works() = runTest {
    val review = createTestReview("review-1", title = "Test Review")
    repository.addReview(review)

    val retrieved = repository.getReview("review-1")
    assertEquals(review, retrieved)
  }

  @Test
  fun getAllReviews_returnsAllReviews() = runTest {
    val review1 = createTestReview("review-1")
    val review2 = createTestReview("review-2")
    repository.addReview(review1)
    repository.addReview(review2)

    val allReviews = repository.getAllReviews()
    assertEquals(2, allReviews.size)
  }

  @Test
  fun getAllReviewsByUser_filtersByUser() = runTest {
    val review1 = createTestReview("review-1", ownerId = "user-1")
    val review2 = createTestReview("review-2", ownerId = "user-2")
    repository.addReview(review1)
    repository.addReview(review2)

    val userReviews = repository.getAllReviewsByUser("user-1")
    assertEquals(1, userReviews.size)
    assertEquals("review-1", userReviews[0].uid)
  }

  @Test
  fun getAllReviewsByResidency_filtersByResidency() = runTest {
    val review1 = createTestReview("review-1", residencyName = "Vortex")
    val review2 = createTestReview("review-2", residencyName = "WOKO")
    repository.addReview(review1)
    repository.addReview(review2)

    val residencyReviews = repository.getAllReviewsByResidency("Vortex")
    assertEquals(1, residencyReviews.size)
    assertEquals("review-1", residencyReviews[0].uid)
  }

  @Test
  fun editReview_updatesReview() = runTest {
    val review = createTestReview("review-1", title = "Original")
    repository.addReview(review)

    val updated = review.copy(title = "Updated")
    repository.editReview("review-1", updated)

    val retrieved = repository.getReview("review-1")
    assertEquals("Updated", retrieved.title)
  }

  @Test
  fun deleteReview_removesReview() = runTest {
    val review = createTestReview("review-1")
    repository.addReview(review)

    repository.deleteReview("review-1")

    assertTrue(runCatching { repository.getReview("review-1") }.isFailure)
  }

  @Test
  fun upvoteReview_addsUpvote() = runTest {
    val ownerId = "owner-1"
    val voterId = "voter-1"
    val review = createTestReview("review-1", ownerId = ownerId)
    repository.addReview(review)

    repository.upvoteReview("review-1", voterId)
    val retrieved = repository.getReview("review-1")

    assertTrue(voterId in retrieved.upvotedBy)
  }

  @Test
  fun upvoteReview_togglesOffIfAlreadyUpvoted() = runTest {
    val ownerId = "owner-1"
    val voterId = "voter-1"
    val review = createTestReview("review-1", ownerId = ownerId, upvotedBy = setOf(voterId))
    repository.addReview(review)

    repository.upvoteReview("review-1", voterId)
    val retrieved = repository.getReview("review-1")

    assertFalse(voterId in retrieved.upvotedBy)
  }

  @Test
  fun upvoteReview_switchesFromDownvoteToUpvote() = runTest {
    val ownerId = "owner-1"
    val voterId = "voter-1"
    val review = createTestReview("review-1", ownerId = ownerId, downvotedBy = setOf(voterId))
    repository.addReview(review)

    repository.upvoteReview("review-1", voterId)
    val retrieved = repository.getReview("review-1")

    assertTrue(voterId in retrieved.upvotedBy)
    assertFalse(voterId in retrieved.downvotedBy)
  }

  @Test
  fun downvoteReview_addsDownvote() = runTest {
    val ownerId = "owner-1"
    val voterId = "voter-1"
    val review = createTestReview("review-1", ownerId = ownerId)
    repository.addReview(review)

    repository.downvoteReview("review-1", voterId)
    val retrieved = repository.getReview("review-1")

    assertTrue(voterId in retrieved.downvotedBy)
  }

  @Test
  fun downvoteReview_switchesFromUpvoteToDownvote() = runTest {
    val ownerId = "owner-1"
    val voterId = "voter-1"
    val review = createTestReview("review-1", ownerId = ownerId, upvotedBy = setOf(voterId))
    repository.addReview(review)

    repository.downvoteReview("review-1", voterId)
    val retrieved = repository.getReview("review-1")

    assertFalse(voterId in retrieved.upvotedBy)
    assertTrue(voterId in retrieved.downvotedBy)
  }

  @Test
  fun removeVote_removesVote() = runTest {
    val ownerId = "owner-1"
    val voterId = "voter-1"
    val review = createTestReview("review-1", ownerId = ownerId, upvotedBy = setOf(voterId))
    repository.addReview(review)

    repository.removeVote("review-1", voterId)
    val retrieved = repository.getReview("review-1")

    assertFalse(voterId in retrieved.upvotedBy)
    assertFalse(voterId in retrieved.downvotedBy)
  }

  @Test
  fun upvoteReview_preventsSelfVoting() = runTest {
    val ownerId = "owner-1"
    val review = createTestReview("review-1", ownerId = ownerId)
    repository.addReview(review)

    assertTrue(runCatching { repository.upvoteReview("review-1", ownerId) }.isFailure)
  }

  @Test
  fun downvoteReview_preventsSelfVoting() = runTest {
    val ownerId = "owner-1"
    val review = createTestReview("review-1", ownerId = ownerId)
    repository.addReview(review)

    assertTrue(runCatching { repository.downvoteReview("review-1", ownerId) }.isFailure)
  }

  @Test
  fun removeVote_preventsSelfVoting() = runTest {
    val ownerId = "owner-1"
    val review = createTestReview("review-1", ownerId = ownerId)
    repository.addReview(review)

    assertTrue(runCatching { repository.removeVote("review-1", ownerId) }.isFailure)
  }

  @Test
  fun upvoteReview_throwsWhenReviewNotFound() = runTest {
    assertTrue(runCatching { repository.upvoteReview("non-existent", "voter-1") }.isFailure)
  }

  @Test
  fun downvoteReview_throwsWhenReviewNotFound() = runTest {
    assertTrue(runCatching { repository.downvoteReview("non-existent", "voter-1") }.isFailure)
  }

  @Test
  fun removeVote_throwsWhenReviewNotFound() = runTest {
    assertTrue(runCatching { repository.removeVote("non-existent", "voter-1") }.isFailure)
  }

  @Test
  fun editReview_throwsWhenUidMismatch() = runTest {
    val review = createTestReview("review-1")
    repository.addReview(review)

    val updated = review.copy(uid = "review-2")
    assertTrue(runCatching { repository.editReview("review-1", updated) }.isFailure)
  }

  @Test
  fun editReview_throwsWhenNotFound() = runTest {
    val review = createTestReview("review-1")
    assertTrue(runCatching { repository.editReview("review-1", review) }.isFailure)
  }

  @Test
  fun getReview_throwsWhenNotFound() = runTest {
    assertTrue(runCatching { repository.getReview("non-existent") }.isFailure)
  }

  private fun createTestReview(
      uid: String,
      ownerId: String = "user-1",
      title: String = "Test Review",
      residencyName: String = "Vortex",
      upvotedBy: Set<String> = emptySet(),
      downvotedBy: Set<String> = emptySet()
  ): Review {
    val fixedTimestamp = Timestamp(1000000L, 0) // Fixed timestamp
    return Review(
        uid = uid,
        ownerId = ownerId,
        postedAt = fixedTimestamp,
        title = title,
        reviewText = "Test text",
        grade = 4.0,
        residencyName = residencyName,
        roomType = RoomType.STUDIO,
        pricePerMonth = 1200.0,
        areaInM2 = 20,
        imageUrls = emptyList(),
        upvotedBy = upvotedBy,
        downvotedBy = downvotedBy,
        isAnonymous = false)
  }
}
