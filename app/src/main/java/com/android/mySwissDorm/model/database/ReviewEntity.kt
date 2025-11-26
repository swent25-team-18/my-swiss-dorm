package com.android.mySwissDorm.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.review.Review
import com.google.firebase.Timestamp

/**
 * Room entity representing a review in the local database.
 *
 * This entity is used to store reviews offline. It can be converted to and from the domain [Review]
 * model using [toReview] and [fromReview].
 *
 * @property uid Unique identifier of the review.
 * @property ownerId Identifier of the user who created the review.
 * @property postedAt Timestamp indicating when the review was created.
 * @property title Short textual title summarizing the review.
 * @property reviewText Full textual content of the review.
 * @property grade Rating of the room, between 1.0 and 5.0.
 * @property residencyName Name of the residency the review refers to.
 * @property roomType Type of room being reviewed (as a string representation of [RoomType]).
 * @property pricePerMonth Monthly rent price of the room.
 * @property areaInM2 Area of the room in square meters.
 * @property imageUrls List of image URLs attached to the review.
 * @property upvotedBy List of user IDs who upvoted this review (stored as List since Room doesn't
 *   support Set).
 * @property downvotedBy List of user IDs who downvoted this review (stored as List since Room
 *   doesn't support Set).
 * @property isAnonymous Whether the review was posted anonymously.
 */
@Entity(tableName = "reviews")
@TypeConverters(Converters::class)
data class ReviewEntity(
    @PrimaryKey val uid: String,
    val ownerId: String,
    val postedAt: Timestamp,
    val title: String,
    val reviewText: String,
    val grade: Double,
    val residencyName: String,
    val roomType: String,
    val pricePerMonth: Double,
    val areaInM2: Int,
    val imageUrls: List<String>,
    val upvotedBy: List<String>,
    val downvotedBy: List<String>,
    val isAnonymous: Boolean = false,
) {
  /**
   * Converts this entity to a domain [Review] model.
   *
   * @return A [Review] object with the same data as this entity.
   */
  fun toReview(): Review {
    return Review(
        uid = uid,
        ownerId = ownerId,
        postedAt = postedAt,
        title = title,
        reviewText = reviewText,
        grade = grade,
        residencyName = residencyName,
        roomType = RoomType.entries.firstOrNull { it.name == roomType } ?: RoomType.STUDIO,
        pricePerMonth = pricePerMonth,
        areaInM2 = areaInM2,
        imageUrls = imageUrls,
        upvotedBy = upvotedBy.toSet(),
        downvotedBy = downvotedBy.toSet(),
        isAnonymous = isAnonymous)
  }

  companion object {
    /**
     * Creates a [ReviewEntity] from a domain [Review] model.
     *
     * @param review The domain review model to convert.
     * @return A [ReviewEntity] with the same data as the review.
     */
    fun fromReview(review: Review): ReviewEntity {
      return ReviewEntity(
          uid = review.uid,
          ownerId = review.ownerId,
          postedAt = review.postedAt,
          title = review.title,
          reviewText = review.reviewText,
          grade = review.grade,
          residencyName = review.residencyName,
          roomType = review.roomType.name,
          pricePerMonth = review.pricePerMonth,
          areaInM2 = review.areaInM2,
          imageUrls = review.imageUrls,
          upvotedBy = review.upvotedBy.toList(),
          downvotedBy = review.downvotedBy.toList(),
          isAnonymous = review.isAnonymous)
    }
  }
}
