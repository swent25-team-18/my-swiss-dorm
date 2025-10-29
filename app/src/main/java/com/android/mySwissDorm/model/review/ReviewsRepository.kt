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
}
