package com.android.mySwissDorm.model.database

import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.review.Review
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class ReviewEntityTest {
  @Test
  fun fromReview_createsEntityCorrectly() {
    val review = createTestReview()
    val entity = ReviewEntity.fromReview(review)

    assertEquals(review.uid, entity.uid)
    assertEquals(review.ownerId, entity.ownerId)
    assertEquals(review.postedAt, entity.postedAt)
    assertEquals(review.title, entity.title)
    assertEquals(review.reviewText, entity.reviewText)
    assertEquals(review.grade, entity.grade, 0.01)
    assertEquals(review.residencyName, entity.residencyName)
    assertEquals(review.roomType.name, entity.roomType)
    assertEquals(review.pricePerMonth, entity.pricePerMonth, 0.01)
    assertEquals(review.areaInM2, entity.areaInM2)
    assertEquals(review.imageUrls, entity.imageUrls)
    assertEquals(review.upvotedBy.toList(), entity.upvotedBy)
    assertEquals(review.downvotedBy.toList(), entity.downvotedBy)
    assertEquals(review.isAnonymous, entity.isAnonymous)
  }

  @Test
  fun toReview_createsReviewCorrectly() {
    val entity = createTestReviewEntity()
    val review = entity.toReview()

    assertEquals(entity.uid, review.uid)
    assertEquals(entity.ownerId, review.ownerId)
    assertEquals(entity.postedAt, review.postedAt)
    assertEquals(entity.title, review.title)
    assertEquals(entity.reviewText, review.reviewText)
    assertEquals(entity.grade, review.grade, 0.01)
    assertEquals(entity.residencyName, review.residencyName)
    assertEquals(entity.roomType, review.roomType.name)
    assertEquals(entity.pricePerMonth, review.pricePerMonth, 0.01)
    assertEquals(entity.areaInM2, review.areaInM2)
    assertEquals(entity.imageUrls, review.imageUrls)
    assertEquals(entity.upvotedBy.toSet(), review.upvotedBy)
    assertEquals(entity.downvotedBy.toSet(), review.downvotedBy)
    assertEquals(entity.isAnonymous, review.isAnonymous)
  }

  @Test
  fun roundTrip_preservesAllFields() {
    val originalReview = createTestReview()
    val entity = ReviewEntity.fromReview(originalReview)
    val convertedReview = entity.toReview()

    assertEquals(originalReview.uid, convertedReview.uid)
    assertEquals(originalReview.ownerId, convertedReview.ownerId)
    assertEquals(originalReview.postedAt.seconds, convertedReview.postedAt.seconds)
    assertEquals(originalReview.postedAt.nanoseconds, convertedReview.postedAt.nanoseconds)
    assertEquals(originalReview.title, convertedReview.title)
    assertEquals(originalReview.reviewText, convertedReview.reviewText)
    assertEquals(originalReview.grade, convertedReview.grade, 0.01)
    assertEquals(originalReview.residencyName, convertedReview.residencyName)
    assertEquals(originalReview.roomType, convertedReview.roomType)
    assertEquals(originalReview.pricePerMonth, convertedReview.pricePerMonth, 0.01)
    assertEquals(originalReview.areaInM2, convertedReview.areaInM2)
    assertEquals(originalReview.imageUrls, convertedReview.imageUrls)
    assertEquals(originalReview.upvotedBy, convertedReview.upvotedBy)
    assertEquals(originalReview.downvotedBy, convertedReview.downvotedBy)
    assertEquals(originalReview.isAnonymous, convertedReview.isAnonymous)
  }

  @Test
  fun toReview_handlesEmptyLists() {
    val entity =
        ReviewEntity(
            uid = "test-uid",
            ownerId = "owner-1",
            postedAt = Timestamp.now(),
            title = "Test",
            reviewText = "Text",
            grade = 4.5,
            residencyName = "Residency",
            roomType = RoomType.STUDIO.name,
            pricePerMonth = 500.0,
            areaInM2 = 20,
            imageUrls = emptyList(),
            upvotedBy = emptyList(),
            downvotedBy = emptyList(),
            isAnonymous = false)

    val review = entity.toReview()

    assertTrue(review.imageUrls.isEmpty())
    assertTrue(review.upvotedBy.isEmpty())
    assertTrue(review.downvotedBy.isEmpty())
  }

  @Test
  fun toReview_handlesInvalidRoomType() {
    val entity =
        ReviewEntity(
            uid = "test-uid",
            ownerId = "owner-1",
            postedAt = Timestamp.now(),
            title = "Test",
            reviewText = "Text",
            grade = 4.5,
            residencyName = "Residency",
            roomType = "INVALID_TYPE",
            pricePerMonth = 500.0,
            areaInM2 = 20,
            imageUrls = emptyList(),
            upvotedBy = emptyList(),
            downvotedBy = emptyList())

    val review = entity.toReview()

    assertEquals(RoomType.STUDIO, review.roomType)
  }

  @Test
  fun fromReview_handlesAllRoomTypes() {
    RoomType.entries.forEach { roomType ->
      val review = createTestReview().copy(roomType = roomType)
      val entity = ReviewEntity.fromReview(review)
      val converted = entity.toReview()

      assertEquals(roomType, converted.roomType)
    }
  }

  private fun createTestReview(): Review {
    return Review(
        uid = "review-1",
        ownerId = "user-1",
        postedAt = Timestamp(1000, 500000),
        title = "Great place",
        reviewText = "I really enjoyed my stay here.",
        grade = 4.5,
        residencyName = "EPFL Residency",
        roomType = RoomType.APARTMENT,
        pricePerMonth = 800.0,
        areaInM2 = 30,
        imageUrls = listOf("url1", "url2"),
        upvotedBy = setOf("user-2", "user-3"),
        downvotedBy = setOf("user-4"),
        isAnonymous = false)
  }

  private fun createTestReviewEntity(): ReviewEntity {
    return ReviewEntity(
        uid = "review-1",
        ownerId = "user-1",
        postedAt = Timestamp(1000, 500000),
        title = "Great place",
        reviewText = "I really enjoyed my stay here.",
        grade = 4.5,
        residencyName = "EPFL Residency",
        roomType = RoomType.APARTMENT.name,
        pricePerMonth = 800.0,
        areaInM2 = 30,
        imageUrls = listOf("url1", "url2"),
        upvotedBy = listOf("user-2", "user-3"),
        downvotedBy = listOf("user-4"),
        isAnonymous = false)
  }
}
