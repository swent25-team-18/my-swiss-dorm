package com.android.mySwissDorm.model.review

import com.android.mySwissDorm.model.rental.RoomType
import com.google.firebase.Timestamp

/**
 * Domain model representing a review of a room or residency.
 *
 * This class is persisted in Firestore (see [ReviewsRepositoryFirestore]) and is used throughout
 * the UI layer. New fields should be given sensible defaults to keep backward compatibility with
 * existing documents.
 *
 * @property uid Unique identifier of the review document.
 * @property ownerId Identifier of the user who created the review.
 * @property ownerName Display name of the user who created the review (stored locally for offline
 *   access).
 * @property postedAt Timestamp indicating when the review was created.
 * @property title Short textual title summarizing the review.
 * @property reviewText Full textual content of the review.
 * @property grade Rating of the room, between 1.0 and 5.0.
 * @property residencyName Name of the residency the review refers to.
 * @property roomType Type of room being reviewed.
 * @property pricePerMonth Monthly rent price of the room.
 * @property areaInM2 Area of the room in square meters.
 * @property imageUrls List of image URLs attached to the review.
 * @property upvotedBy Set of user IDs who upvoted this review. Uses a Set to ensure each user ID
 *   appears only once.
 * @property downvotedBy Set of user IDs who downvoted this review. Uses a Set to ensure each user
 *   ID appears only once.
 * @property isAnonymous Whether the review was posted anonymously. If true, the username should not
 *   be displayed.
 */
data class Review(
    val uid: String,
    val ownerId: String,
    val ownerName: String? = null,
    val postedAt: Timestamp,
    val title: String,
    val reviewText: String,
    val grade: Double,
    val residencyName: String,
    val roomType: RoomType,
    val pricePerMonth: Double,
    val areaInM2: Int,
    val imageUrls: List<String>,
    val upvotedBy: Set<String> = emptySet(),
    val downvotedBy: Set<String> = emptySet(),
    val isAnonymous: Boolean = false,
) {
  /**
   * Computes the net vote score (upvotes - downvotes) for this review.
   *
   * @return The net score as an integer.
   */
  fun getNetScore(): Int = upvotedBy.size - downvotedBy.size

  /**
   * Determines the vote type for a given user on this review.
   *
   * @param userId The unique identifier of the user to check.
   * @return The [VoteType] for the user, or [VoteType.NONE] if the user hasn't voted.
   */
  fun getUserVote(userId: String?): VoteType =
      when {
        userId != null && userId in upvotedBy -> VoteType.UPVOTE
        userId != null && userId in downvotedBy -> VoteType.DOWNVOTE
        else -> VoteType.NONE
      }
}
