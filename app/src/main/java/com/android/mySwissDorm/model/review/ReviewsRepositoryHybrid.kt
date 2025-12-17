package com.android.mySwissDorm.model.review

import android.content.Context
import android.util.Log
import com.android.mySwissDorm.model.HybridRepositoryBase
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.utils.LastSyncTracker
import com.android.mySwissDorm.utils.NetworkUtils
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
          syncToLocal = { reviews -> syncReviewsToLocal(reviews, isFullSync = true) })

  override suspend fun getAllReviewsByUser(userId: String): List<Review> =
      performRead(
          operationName = "getAllReviewsByUser",
          remoteCall = { remoteRepository.getAllReviewsByUser(userId) },
          localFallback = { localRepository.getAllReviewsByUser(userId) },
          syncToLocal = { reviews -> syncReviewsToLocal(reviews, isFullSync = false) })

  override suspend fun getAllReviewsByResidency(residencyName: String): List<Review> =
      performRead(
          operationName = "getAllReviewsByResidency",
          remoteCall = { remoteRepository.getAllReviewsByResidency(residencyName) },
          localFallback = { localRepository.getAllReviewsByResidency(residencyName) },
          syncToLocal = { reviews -> syncReviewsToLocal(reviews, isFullSync = false) })

  override suspend fun getReview(reviewId: String): Review =
      performRead(
          operationName = "getReview",
          remoteCall = { remoteRepository.getReview(reviewId) },
          localFallback = { localRepository.getReview(reviewId) },
          syncToLocal = { review -> syncReviewsToLocal(listOf(review), isFullSync = false) })

  override suspend fun addReview(review: Review) =
      performWrite(
          operationName = "addReview",
          remoteCall = { remoteRepository.addReview(review) },
          localSync = { syncReviewsToLocal(listOf(review), isFullSync = false) })

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
              syncReviewsToLocal(listOf(updatedReview), isFullSync = false)
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
              syncReviewsToLocal(listOf(updatedReview), isFullSync = false)
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
              syncReviewsToLocal(listOf(updatedReview), isFullSync = false)
            } catch (e: Exception) {
              Log.w(TAG, "Error fetching updated review after removeVote for local sync", e)
              // Continue - main operation succeeded
            }
          })

  override suspend fun getAllReviewsByResidencyForUser(
      residencyName: String,
      userId: String?
  ): List<Review> {
    val allReviews = getAllReviewsByResidency(residencyName)
    if (userId == null) return allReviews

    return filterReviewsByBlocking(allReviews, userId)
  }

  override suspend fun getReviewForUser(reviewId: String, userId: String?): Review {
    val review = getReview(reviewId)
    if (userId == null || userId == review.ownerId) return review

    // Always allow anonymous reviews to preserve anonymity (security/privacy requirement)
    if (review.isAnonymous) return review

    // Check bidirectional blocking for non-anonymous reviews
    val isBlocked = isBlockedBidirectionally(review.ownerId, userId)
    if (isBlocked) {
      throw NoSuchElementException(
          "ReviewsRepositoryHybrid: Review $reviewId is not available due to blocking restrictions")
    }
    return review
  }

  /**
   * Filters reviews based on bidirectional blocking.
   *
   * @param reviews The reviews to filter.
   * @param userId The current user's ID.
   * @return Filtered list of reviews visible to the user.
   */
  private suspend fun filterReviewsByBlocking(reviews: List<Review>, userId: String): List<Review> {
    // Load current user's blocked list once for efficiency
    val currentUserBlockedList =
        runCatching { ProfileRepositoryProvider.repository.getBlockedUserIds(userId) }
            .onFailure { Log.w(TAG, "Failed to fetch current user's blocked list", it) }
            .getOrDefault(emptyList())

    val blockedCache = mutableMapOf<String, Boolean>()
    return reviews.filter { review ->
      // Always show own reviews
      if (review.ownerId == userId) return@filter true
      // Always show anonymous reviews to preserve anonymity (security/privacy requirement)
      if (review.isAnonymous) return@filter true
      // For non-anonymous reviews, apply blocking filter
      !isBlockedBidirectionally(review.ownerId, userId, currentUserBlockedList, blockedCache)
    }
  }

  /**
   * Syncs reviews to the local database for offline access.
   *
   * This method is called after successful remote operations to ensure data is available offline.
   * Fetches owner names for reviews that don't have them, then uses the existing
   * [ReviewsRepositoryLocal.addReview] method which handles syncing. Also records the sync
   * timestamp for the offline banner.
   *
   * When [isFullSync] is true (e.g., from [getAllReviews]), this method will also delete any local
   * reviews that are not in the remote list, ensuring the local database stays in sync with
   * Firestore deletions.
   *
   * @param reviews The reviews to sync to local storage.
   * @param isFullSync Whether this represents a complete sync of all reviews. If true, local
   *   reviews not in this list will be deleted.
   */
  private suspend fun syncReviewsToLocal(reviews: List<Review>, isFullSync: Boolean) {
    try {
      // If this is a full sync, delete local reviews that are no longer in Firestore
      if (isFullSync) {
        try {
          val remoteIds = reviews.map { it.uid }.toSet()
          val localReviewsBefore = localRepository.getAllReviews()
          val localIdsBefore = localReviewsBefore.map { it.uid }.toSet()
          localRepository.deleteReviewsNotIn(remoteIds.toList())
          val deletedIds = localIdsBefore - remoteIds
          if (deletedIds.isNotEmpty()) {
            Log.d(
                TAG,
                "[syncReviewsToLocal] Deleted ${deletedIds.size} stale reviews during full sync")
          }
        } catch (e: Exception) {
          Log.w(TAG, "[syncReviewsToLocal] Error deleting stale reviews during full sync", e)
          // Continue with sync even if deletion fails
        }
      }

      if (reviews.isEmpty()) {
        // Record sync even if no reviews to sync (might have just deleted stale data)
        LastSyncTracker.recordSync(context)
        return
      }

      reviews.forEach { review ->
        try {
          // Fetch owner name if missing (only when online)
          val reviewWithOwnerName =
              if (review.ownerName == null && NetworkUtils.isNetworkAvailable(context)) {
                try {
                  val profile = ProfileRepositoryProvider.repository.getProfile(review.ownerId)
                  val ownerName = "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
                  review.copy(ownerName = ownerName.takeIf { it.isNotEmpty() })
                } catch (e: Exception) {
                  Log.w(TAG, "Error fetching owner name for review ${review.uid}", e)
                  review // Store null if profile fetch fails - ViewModels will handle fallback
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
