package com.android.mySwissDorm.model.review

import com.android.mySwissDorm.model.database.ReviewDao
import com.android.mySwissDorm.model.database.ReviewEntity

/**
 * Room-backed local implementation of [ReviewsRepository].
 *
 * This repository provides offline access to reviews by storing them in a local Room database. It
 * implements the same interface as [ReviewsRepositoryFirestore], allowing seamless switching
 * between local and remote data sources.
 */
class ReviewsRepositoryLocal(private val reviewDao: ReviewDao) : ReviewsRepository {

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
   * This method is not supported in the local repository. Voting operations are only allowed when
   * online, and the hybrid repository handles voting by fetching the updated review from the server
   * and syncing it to local storage.
   *
   * @throws UnsupportedOperationException Always, as voting is not supported offline.
   */
  override suspend fun upvoteReview(reviewId: String, userId: String) {
    throw UnsupportedOperationException(
        "ReviewsRepositoryLocal: Cannot vote on reviews offline. Please connect to the internet.")
  }

  /**
   * Applies a downvote to the review by the given user.
   *
   * This method is not supported in the local repository. Voting operations are only allowed when
   * online, and the hybrid repository handles voting by fetching the updated review from the server
   * and syncing it to local storage.
   *
   * @throws UnsupportedOperationException Always, as voting is not supported offline.
   */
  override suspend fun downvoteReview(reviewId: String, userId: String) {
    throw UnsupportedOperationException(
        "ReviewsRepositoryLocal: Cannot vote on reviews offline. Please connect to the internet.")
  }

  /**
   * Removes any existing vote (upvote or downvote) from the review by the given user.
   *
   * This method is not supported in the local repository. Voting operations are only allowed when
   * online, and the hybrid repository handles voting by fetching the updated review from the server
   * and syncing it to local storage.
   *
   * @throws UnsupportedOperationException Always, as voting is not supported offline.
   */
  override suspend fun removeVote(reviewId: String, userId: String) {
    throw UnsupportedOperationException(
        "ReviewsRepositoryLocal: Cannot vote on reviews offline. Please connect to the internet.")
  }
}
