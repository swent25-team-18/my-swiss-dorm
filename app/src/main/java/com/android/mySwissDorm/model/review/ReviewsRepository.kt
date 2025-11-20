package com.android.mySwissDorm.model.review

interface ReviewsRepository {
  /** Generates and returns a new unique identifier for a Review. */
  fun getNewUid(): String

  /**
   * Retrieves all Reviews from the repository.
   *
   * @return A list of all Reviews.
   */
  suspend fun getAllReviews(): List<Review>

  /**
   * Retrieves all Review created by a specific user from the repository.
   *
   * @param userId The unique identifier of the user whose review to retrieve.
   * @return A list of all Reviews created by the specified user.
   */
  suspend fun getAllReviewsByUser(userId: String): List<Review>

  /**
   * Retrieves all Review related to a specific residency from the repository.
   *
   * @param residencyName The name of the residency whose reviews to retrieve.
   * @return A list of all Reviews related to the specified residency.
   */
  suspend fun getAllReviewsByResidency(residencyName: String): List<Review>

  /**
   * Retrieves a specific Review by its unique identifier.
   *
   * @param reviewId The unique identifier of the review item to retrieve.
   * @return The review item with the specified identifier.
   * @throws Exception if the review item is not found.
   */
  suspend fun getReview(reviewId: String): Review

  /**
   * Adds a new review item to the repository.
   *
   * @param review The review item to add.
   */
  suspend fun addReview(review: Review)

  /**
   * Edits an existing review item in the repository.
   *
   * @param reviewId The unique identifier of the review item to edit.
   * @param newValue The new value for the review item.
   * @throws Exception if the review item is not found.
   */
  suspend fun editReview(reviewId: String, newValue: Review)

  /**
   * Deletes a review item from the repository.
   *
   * @param reviewId The unique identifier of the review item to delete.
   * @throws Exception if the review item is not found.
   */
  suspend fun deleteReview(reviewId: String)

  /**
   * Applies an upvote to the review by the given user.
   *
   * If the user has already upvoted, the upvote is removed. If the user has downvoted, the downvote
   * is removed and replaced with an upvote.
   *
   * @param reviewId The unique identifier of the review to upvote.
   * @param userId The unique identifier of the user casting the vote.
   * @throws Exception if the review is not found or if the user is the owner of the review.
   */
  suspend fun upvoteReview(reviewId: String, userId: String)

  /**
   * Applies a downvote to the review by the given user.
   *
   * If the user has already downvoted, the downvote is removed. If the user has upvoted, the upvote
   * is removed and replaced with a downvote.
   *
   * @param reviewId The unique identifier of the review to downvote.
   * @param userId The unique identifier of the user casting the vote.
   * @throws Exception if the review is not found or if the user is the owner of the review.
   */
  suspend fun downvoteReview(reviewId: String, userId: String)

  /**
   * Removes any existing vote (upvote or downvote) from the review by the given user.
   *
   * @param reviewId The unique identifier of the review to remove the vote from.
   * @param userId The unique identifier of the user whose vote should be removed.
   * @throws Exception if the review is not found or if the user is the owner of the review.
   */
  suspend fun removeVote(reviewId: String, userId: String)
}
