package com.android.mySwissDorm.model.database

import androidx.room.Room
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
class ReviewDaoTest {
  private lateinit var database: AppDatabase
  private lateinit var reviewDao: ReviewDao

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    reviewDao = database.reviewDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun insertReview_insertsSuccessfully() = runTest {
    val entity = createTestReviewEntity("review-1")
    reviewDao.insertReview(entity)

    val retrieved = reviewDao.getReview("review-1")
    assertNotNull(retrieved)
    assertEquals("review-1", retrieved!!.uid)
  }

  @Test
  fun getReview_returnsNullWhenNotFound() = runTest {
    val result = reviewDao.getReview("non-existent")
    assertNull(result)
  }

  @Test
  fun getAllReviews_returnsAllReviews() = runTest {
    val entity1 = createTestReviewEntity("review-1")
    val entity2 = createTestReviewEntity("review-2")
    val entity3 = createTestReviewEntity("review-3")

    reviewDao.insertReview(entity1)
    reviewDao.insertReview(entity2)
    reviewDao.insertReview(entity3)

    val allReviews = reviewDao.getAllReviews()
    assertEquals(3, allReviews.size)
  }

  @Test
  fun getAllReviews_returnsEmptyListWhenNoReviews() = runTest {
    val allReviews = reviewDao.getAllReviews()
    assertTrue(allReviews.isEmpty())
  }

  @Test
  fun getAllReviewsByUser_returnsOnlyUserReviews() = runTest {
    val entity1 = createTestReviewEntity("review-1", ownerId = "user-1")
    val entity2 = createTestReviewEntity("review-2", ownerId = "user-2")
    val entity3 = createTestReviewEntity("review-3", ownerId = "user-1")

    reviewDao.insertReview(entity1)
    reviewDao.insertReview(entity2)
    reviewDao.insertReview(entity3)

    val userReviews = reviewDao.getAllReviewsByUser("user-1")
    assertEquals(2, userReviews.size)
    assertTrue(userReviews.all { it.ownerId == "user-1" })
  }

  @Test
  fun getAllReviewsByResidency_returnsOnlyResidencyReviews() = runTest {
    val entity1 = createTestReviewEntity("review-1", residencyName = "Residency A")
    val entity2 = createTestReviewEntity("review-2", residencyName = "Residency B")
    val entity3 = createTestReviewEntity("review-3", residencyName = "Residency A")

    reviewDao.insertReview(entity1)
    reviewDao.insertReview(entity2)
    reviewDao.insertReview(entity3)

    val residencyReviews = reviewDao.getAllReviewsByResidency("Residency A")
    assertEquals(2, residencyReviews.size)
    assertTrue(residencyReviews.all { it.residencyName == "Residency A" })
  }

  @Test
  fun insertReviews_insertsMultipleReviews() = runTest {
    val entities =
        listOf(
            createTestReviewEntity("review-1"),
            createTestReviewEntity("review-2"),
            createTestReviewEntity("review-3"))

    reviewDao.insertReviews(entities)

    val allReviews = reviewDao.getAllReviews()
    assertEquals(3, allReviews.size)
  }

  @Test
  fun updateReview_updatesExistingReview() = runTest {
    val entity = createTestReviewEntity("review-1", title = "Original Title")
    reviewDao.insertReview(entity)

    val updated = entity.copy(title = "Updated Title")
    reviewDao.updateReview(updated)

    val retrieved = reviewDao.getReview("review-1")
    assertNotNull(retrieved)
    assertEquals("Updated Title", retrieved!!.title)
  }

  @Test
  fun insertReview_replacesOnConflict() = runTest {
    val entity1 = createTestReviewEntity("review-1", title = "Original")
    reviewDao.insertReview(entity1)

    val entity2 = createTestReviewEntity("review-1", title = "Replaced")
    reviewDao.insertReview(entity2)

    val retrieved = reviewDao.getReview("review-1")
    assertNotNull(retrieved)
    assertEquals("Replaced", retrieved!!.title)
  }

  @Test
  fun deleteReview_deletesSpecificReview() = runTest {
    val entity1 = createTestReviewEntity("review-1")
    val entity2 = createTestReviewEntity("review-2")
    reviewDao.insertReview(entity1)
    reviewDao.insertReview(entity2)

    reviewDao.deleteReview("review-1")

    assertNull(reviewDao.getReview("review-1"))
    assertNotNull(reviewDao.getReview("review-2"))
  }

  @Test
  fun getAllReviewsByUser_returnsEmptyWhenUserHasNoReviews() = runTest {
    reviewDao.insertReview(createTestReviewEntity("review-1", ownerId = "user-1"))

    val userReviews = reviewDao.getAllReviewsByUser("user-2")
    assertTrue(userReviews.isEmpty())
  }

  @Test
  fun getAllReviewsByResidency_returnsEmptyWhenResidencyHasNoReviews() = runTest {
    reviewDao.insertReview(createTestReviewEntity("review-1", residencyName = "Residency A"))

    val residencyReviews = reviewDao.getAllReviewsByResidency("Residency B")
    assertTrue(residencyReviews.isEmpty())
  }

  private fun createTestReviewEntity(
      uid: String,
      ownerId: String = "user-1",
      residencyName: String = "EPFL Residency",
      title: String = "Test Review"
  ): ReviewEntity {
    return ReviewEntity(
        uid = uid,
        ownerId = ownerId,
        postedAt = Timestamp.now(),
        title = title,
        reviewText = "Test review text",
        grade = 4.0,
        residencyName = residencyName,
        roomType = RoomType.STUDIO.name,
        pricePerMonth = 500.0,
        areaInM2 = 20,
        imageUrls = emptyList(),
        upvotedBy = emptyList(),
        downvotedBy = emptyList(),
        isAnonymous = false)
  }
}
