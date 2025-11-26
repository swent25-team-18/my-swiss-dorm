package com.android.mySwissDorm.model.review

import androidx.room.withTransaction
import com.android.mySwissDorm.model.database.AppDatabase
import com.android.mySwissDorm.model.database.ReviewDao
import com.android.mySwissDorm.model.database.ReviewEntity

/**
 * Room-backed local implementation of [ReviewsRepository].
 *
 * This repository provides offline access to reviews by storing them in a local Room database. It
 * implements the same interface as [ReviewsRepositoryFirestore], allowing seamless switching
 * between local and remote data sources.
 */
class ReviewsRepositoryLocal(private val reviewDao: ReviewDao, private val database: AppDatabase) :
    ReviewsRepository {

  /**
   * Generates a new unique identifier for a review.
   *
   * This method is not supported when using the local repository, as new content cannot be created
   * offline.
   *
   * @throws UnsupportedOperationException Always, as new content cannot be created offline.
   */
  override fun getNewUid(): String {
    throw UnsupportedOperationException(
        "ReviewsRepositoryLocal: Cannot create new reviews offline. Please connect to the internet.")
  }

  /**
   * Retrieves all reviews from the local database.
   *
   * @return A list of all reviews stored locally.
   */
  override suspend fun getAllReviews(): List<Review> {
    return reviewDao.getAllReviews().map { it.toReview() }
  }

  /**
   * Retrieves all reviews created by a specific user.
   *
   * @param userId The unique identifier of the user.
   * @return A list of reviews created by the specified user.
   */
  override suspend fun getAllReviewsByUser(userId: String): List<Review> {
    return reviewDao.getAllReviewsByUser(userId).map { it.toReview() }
  }

  /**
   * Retrieves all reviews for a specific residency.
   *
   * @param residencyName The name of the residency.
   * @return A list of reviews for the specified residency.
   */
  override suspend fun getAllReviewsByResidency(residencyName: String): List<Review> {
    return reviewDao.getAllReviewsByResidency(residencyName).map { it.toReview() }
  }

  /**
   * Retrieves a specific review by its unique identifier.
   *
   * @param reviewId The unique identifier of the review.
   * @return The review with the specified identifier.
   * @throws NoSuchElementException if the review is not found.
   */
  override suspend fun getReview(reviewId: String): Review {
    return reviewDao.getReview(reviewId)?.toReview()
        ?: throw NoSuchElementException("ReviewsRepositoryLocal: Review $reviewId not found")
  }

  /**
   * Adds a review to the local database.
   *
   * This method can be used to store reviews that already have a UID (e.g., when syncing from
   * remote). It cannot be used to create new reviews offline, as [getNewUid] will throw an
   * exception.
   *
   * @param review The review to add. Must have a valid UID.
   */
  override suspend fun addReview(review: Review) {
    reviewDao.insertReview(ReviewEntity.fromReview(review))
  }

  /**
   * Edits an existing review in the local database.
   *
   * @param reviewId The unique identifier of the review to edit.
   * @param newValue The new value for the review.
   * @throws IllegalArgumentException if the reviewId doesn't match newValue.uid.
   * @throws NoSuchElementException if the review is not found.
   */
  override suspend fun editReview(reviewId: String, newValue: Review) {
    if (newValue.uid != reviewId) {
      throw IllegalArgumentException(
          "ReviewsRepositoryLocal: Provided reviewId does not match newValue.uid")
    }
    // Verify the review exists before updating
    if (reviewDao.getReview(reviewId) == null) {
      throw NoSuchElementException("ReviewsRepositoryLocal: Review $reviewId not found")
    }
    reviewDao.updateReview(ReviewEntity.fromReview(newValue))
  }

  /**
   * Deletes a review from the local database.
   *
   * @param reviewId The unique identifier of the review to delete.
   */
  override suspend fun deleteReview(reviewId: String) {
    reviewDao.deleteReview(reviewId)
  }

  /**
   * Applies an upvote to the review by the given user.
   *
   * If the user has already upvoted, the upvote is removed. If the user has downvoted, the downvote
   * is removed and replaced with an upvote.
   *
   * This operation is performed atomically within a transaction to prevent race conditions.
   *
   * @param reviewId The unique identifier of the review to upvote.
   * @param userId The unique identifier of the user casting the vote.
   * @throws NoSuchElementException if the review is not found.
   * @throws IllegalArgumentException if the user is the owner of the review.
   */
  override suspend fun upvoteReview(reviewId: String, userId: String) {
    database.withTransaction {
      val entity =
          reviewDao.getReview(reviewId)
              ?: throw NoSuchElementException("ReviewsRepositoryLocal: Review $reviewId not found")

      val review = entity.toReview()

      if (review.ownerId == userId) {
        throw IllegalArgumentException(
            "ReviewsRepositoryLocal: Users cannot vote on their own reviews")
      }

      val newUpvotedBy = review.upvotedBy.toMutableSet()
      val newDownvotedBy = review.downvotedBy.toMutableSet()

      when (userId) {
        in newUpvotedBy -> {
          // Already upvoted: remove upvote
          newUpvotedBy.remove(userId)
        }
        in newDownvotedBy -> {
          // Downvoted: remove downvote and add upvote
          newDownvotedBy.remove(userId)
          newUpvotedBy.add(userId)
        }
        else -> {
          // No vote: add upvote
          newUpvotedBy.add(userId)
        }
      }

      val updatedReview = review.copy(upvotedBy = newUpvotedBy, downvotedBy = newDownvotedBy)
      reviewDao.updateReview(ReviewEntity.fromReview(updatedReview))
    }
  }

  /**
   * Applies a downvote to the review by the given user.
   *
   * If the user has already downvoted, the downvote is removed. If the user has upvoted, the upvote
   * is removed and replaced with a downvote.
   *
   * This operation is performed atomically within a transaction to prevent race conditions.
   *
   * @param reviewId The unique identifier of the review to downvote.
   * @param userId The unique identifier of the user casting the vote.
   * @throws NoSuchElementException if the review is not found.
   * @throws IllegalArgumentException if the user is the owner of the review.
   */
  override suspend fun downvoteReview(reviewId: String, userId: String) {
    database.withTransaction {
      val entity =
          reviewDao.getReview(reviewId)
              ?: throw NoSuchElementException("ReviewsRepositoryLocal: Review $reviewId not found")

      val review = entity.toReview()

      if (review.ownerId == userId) {
        throw IllegalArgumentException(
            "ReviewsRepositoryLocal: Users cannot vote on their own reviews")
      }

      val newUpvotedBy = review.upvotedBy.toMutableSet()
      val newDownvotedBy = review.downvotedBy.toMutableSet()

      when (userId) {
        in newDownvotedBy -> {
          // Already downvoted: remove downvote
          newDownvotedBy.remove(userId)
        }
        in newUpvotedBy -> {
          // Upvoted: remove upvote and add downvote
          newUpvotedBy.remove(userId)
          newDownvotedBy.add(userId)
        }
        else -> {
          // No vote: add downvote
          newDownvotedBy.add(userId)
        }
      }

      val updatedReview = review.copy(upvotedBy = newUpvotedBy, downvotedBy = newDownvotedBy)
      reviewDao.updateReview(ReviewEntity.fromReview(updatedReview))
    }
  }

  /**
   * Removes any existing vote (upvote or downvote) from the review by the given user.
   *
   * This operation is performed atomically within a transaction to prevent race conditions.
   *
   * @param reviewId The unique identifier of the review to remove the vote from.
   * @param userId The unique identifier of the user whose vote should be removed.
   * @throws NoSuchElementException if the review is not found.
   * @throws IllegalArgumentException if the user is the owner of the review.
   */
  override suspend fun removeVote(reviewId: String, userId: String) {
    database.withTransaction {
      val entity =
          reviewDao.getReview(reviewId)
              ?: throw NoSuchElementException("ReviewsRepositoryLocal: Review $reviewId not found")

      val review = entity.toReview()

      if (review.ownerId == userId) {
        throw IllegalArgumentException(
            "ReviewsRepositoryLocal: Users cannot vote on their own reviews")
      }

      val newUpvotedBy = review.upvotedBy.toMutableSet()
      val newDownvotedBy = review.downvotedBy.toMutableSet()

      newUpvotedBy.remove(userId)
      newDownvotedBy.remove(userId)

      val updatedReview = review.copy(upvotedBy = newUpvotedBy, downvotedBy = newDownvotedBy)
      reviewDao.updateReview(ReviewEntity.fromReview(updatedReview))
    }
  }
}
