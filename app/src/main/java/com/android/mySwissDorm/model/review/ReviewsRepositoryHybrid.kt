package com.android.mySwissDorm.model.review

import android.content.Context
import android.util.Log
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.HybridRepositoryBase
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.utils.LastSyncTracker
import kotlinx.coroutines.withTimeout

/**
 * Hybrid implementation of [ReviewsRepository] that combines remote (Firestore) and local (Room)
 * data sources.
 *
 * This repository uses a fast-fail approach: if the device is offline, it immediately falls back to
 * the local repository without attempting network operations. When online, it attempts remote
 * operations first and falls back to local on network errors or timeouts.
 *
 * @property context The application context for checking network connectivity.
 * @property remoteRepository The Firestore-backed repository for online operations.
 * @property localRepository The Room-backed repository for offline operations.
 */
class ReviewsRepositoryHybrid(
    context: Context,
    private val remoteRepository: ReviewsRepositoryFirestore,
    private val localRepository: ReviewsRepositoryLocal
) : HybridRepositoryBase<Review>(context, "ReviewsRepository"), ReviewsRepository {

  override fun getNewUid(): String {
    return getNewUidWithNetworkCheck { remoteRepository.getNewUid() }
  }

  override suspend fun getAllReviews(): List<Review> =
      performRead(
          operationName = "getAllReviews",
          remoteCall = { remoteRepository.getAllReviews() },
          localFallback = { localRepository.getAllReviews() },
          syncToLocal = { reviews -> syncReviewsToLocal(reviews) })

  override suspend fun getAllReviewsByUser(userId: String): List<Review> =
      performRead(
          operationName = "getAllReviewsByUser",
          remoteCall = { remoteRepository.getAllReviewsByUser(userId) },
          localFallback = { localRepository.getAllReviewsByUser(userId) },
          syncToLocal = { reviews -> syncReviewsToLocal(reviews) })

  override suspend fun getAllReviewsByResidency(residencyName: String): List<Review> =
      performRead(
          operationName = "getAllReviewsByResidency",
          remoteCall = { remoteRepository.getAllReviewsByResidency(residencyName) },
          localFallback = { localRepository.getAllReviewsByResidency(residencyName) },
          syncToLocal = { reviews -> syncReviewsToLocal(reviews) })

  override suspend fun getReview(reviewId: String): Review =
      performRead(
          operationName = "getReview",
          remoteCall = { remoteRepository.getReview(reviewId) },
          localFallback = { localRepository.getReview(reviewId) },
          syncToLocal = { review -> syncReviewsToLocal(listOf(review)) })

  override suspend fun addReview(review: Review) =
      performWrite(
          operationName = "addReview",
          remoteCall = { remoteRepository.addReview(review) },
          localSync = { syncReviewsToLocal(listOf(review)) })

  override suspend fun editReview(reviewId: String, newValue: Review) =
      performWrite(
          operationName = "editReview",
          remoteCall = { remoteRepository.editReview(reviewId, newValue) },
          localSync = { localRepository.editReview(reviewId, newValue) })

  override suspend fun deleteReview(reviewId: String) =
      performWrite(
          operationName = "deleteReview",
          remoteCall = { remoteRepository.deleteReview(reviewId) },
          localSync = { localRepository.deleteReview(reviewId) })

  override suspend fun upvoteReview(reviewId: String, userId: String) =
      performWrite(
          operationName = "upvoteReview",
          remoteCall = { remoteRepository.upvoteReview(reviewId, userId) },
          localSync = {
            // Fetch updated review to sync the new vote count
            try {
              val updatedReview = withTimeout(TIMEOUT_MS) { remoteRepository.getReview(reviewId) }
              syncReviewsToLocal(listOf(updatedReview))
            } catch (e: Exception) {
              Log.w(TAG, "Error fetching updated review after upvote for local sync", e)
              // Continue - main operation succeeded
            }
          })

  override suspend fun downvoteReview(reviewId: String, userId: String) =
      performWrite(
          operationName = "downvoteReview",
          remoteCall = { remoteRepository.downvoteReview(reviewId, userId) },
          localSync = {
            try {
              val updatedReview = withTimeout(TIMEOUT_MS) { remoteRepository.getReview(reviewId) }
              syncReviewsToLocal(listOf(updatedReview))
            } catch (e: Exception) {
              Log.w(TAG, "Error fetching updated review after downvote for local sync", e)
              // Continue - main operation succeeded
            }
          })

  override suspend fun removeVote(reviewId: String, userId: String) =
      performWrite(
          operationName = "removeVote",
          remoteCall = { remoteRepository.removeVote(reviewId, userId) },
          localSync = {
            try {
              val updatedReview = withTimeout(TIMEOUT_MS) { remoteRepository.getReview(reviewId) }
              syncReviewsToLocal(listOf(updatedReview))
            } catch (e: Exception) {
              Log.w(TAG, "Error fetching updated review after removeVote for local sync", e)
              // Continue - main operation succeeded
            }
          })

  /**
   * Syncs reviews to the local database for offline access.
   *
   * This method is called after successful remote operations to ensure data is available offline.
   * Fetches owner names for reviews that don't have them, then uses the existing
   * [ReviewsRepositoryLocal.addReview] method which handles syncing. Also records the sync
   * timestamp for the offline banner.
   *
   * @param reviews The reviews to sync to local storage.
   */
  private suspend fun syncReviewsToLocal(reviews: List<Review>) {
    if (reviews.isEmpty()) return

    try {
      reviews.forEach { review ->
        try {
          // Fetch owner name if missing
          val reviewWithOwnerName =
              if (review.ownerName == null) {
                try {
                  val profile = ProfileRepositoryProvider.repository.getProfile(review.ownerId)
                  val ownerName = "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
                  review.copy(
                      ownerName =
                          ownerName.takeIf { it.isNotEmpty() }
                              ?: context.getString(R.string.unknown_owner_name))
                } catch (e: Exception) {
                  Log.w(TAG, "Error fetching owner name for review ${review.uid}", e)
                  review.copy(ownerName = context.getString(R.string.unknown_owner_name))
                }
              } else {
                review
              }

          localRepository.addReview(reviewWithOwnerName)
        } catch (e: Exception) {
          Log.w(TAG, "Error syncing review ${review.uid} to local", e)
          // Continue with other reviews even if one fails
        }
      }
      // Record successful sync timestamp
      LastSyncTracker.recordSync(context)
    } catch (e: Exception) {
      Log.w(TAG, "Error syncing reviews to local", e)
      // Don't throw - syncing is best effort
    }
  }
}
