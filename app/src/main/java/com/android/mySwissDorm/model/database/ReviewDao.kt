package com.android.mySwissDorm.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object for [ReviewEntity] operations.
 *
 * Provides methods to query, insert, update, and delete reviews from the local database.
 */
@Dao
interface ReviewDao {
  /**
   * Retrieves all reviews from the database.
   *
   * @return A list of all [ReviewEntity] objects in the database.
   */
  @Query("SELECT * FROM reviews") suspend fun getAllReviews(): List<ReviewEntity>

  /**
   * Retrieves all reviews created by a specific user.
   *
   * @param userId The unique identifier of the user.
   * @return A list of [ReviewEntity] objects created by the specified user.
   */
  @Query("SELECT * FROM reviews WHERE ownerId = :userId")
  suspend fun getAllReviewsByUser(userId: String): List<ReviewEntity>

  /**
   * Retrieves all reviews for a specific residency.
   *
   * @param residencyName The name of the residency.
   * @return A list of [ReviewEntity] objects for the specified residency.
   */
  @Query("SELECT * FROM reviews WHERE residencyName = :residencyName")
  suspend fun getAllReviewsByResidency(residencyName: String): List<ReviewEntity>

  /**
   * Retrieves a single review by its unique identifier.
   *
   * @param reviewId The unique identifier of the review.
   * @return The [ReviewEntity] with the specified ID, or null if not found.
   */
  @Query("SELECT * FROM reviews WHERE uid = :reviewId")
  suspend fun getReview(reviewId: String): ReviewEntity?

  /**
   * Inserts a single review into the database.
   *
   * If a review with the same [ReviewEntity.uid] already exists, it will be replaced.
   *
   * @param review The [ReviewEntity] to insert.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReview(review: ReviewEntity)

  /**
   * Inserts multiple reviews into the database.
   *
   * If any review with the same [ReviewEntity.uid] already exists, it will be replaced.
   *
   * @param reviews The list of [ReviewEntity] objects to insert.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReviews(reviews: List<ReviewEntity>)

  /**
   * Updates an existing review in the database.
   *
   * @param review The [ReviewEntity] to update. Must have an existing [ReviewEntity.uid].
   */
  @Update suspend fun updateReview(review: ReviewEntity)

  /**
   * Deletes a specific review from the database.
   *
   * @param reviewId The unique identifier of the review to delete.
   */
  @Query("DELETE FROM reviews WHERE uid = :reviewId") suspend fun deleteReview(reviewId: String)
}
