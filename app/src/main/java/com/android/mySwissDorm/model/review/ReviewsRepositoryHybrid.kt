package com.android.mySwissDorm.model.review

import android.content.Context
import android.util.Log
import com.android.mySwissDorm.utils.NetworkUtils
import kotlin.NoSuchElementException
import kotlinx.coroutines.TimeoutCancellationException
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
    private val context: Context,
    private val remoteRepository: ReviewsRepositoryFirestore,
    private val localRepository: ReviewsRepositoryLocal
) : ReviewsRepository {

  private val TAG = "ReviewsRepositoryHybrid"
  private val TIMEOUT_MS = 5000L

  /**
   * Performs a read operation with fast-fail and fallback logic.
   *
   * Strategy:
   * 1. Fast-fail: If offline, return local data immediately
   * 2. Timeout safety: Try remote with timeout
   * 3. Sync: Save successful remote results to local
   * 4. Fallback: On network error/timeout, return local data
   */
  private suspend fun <T> performRead(
      operationName: String,
      remoteCall: suspend () -> T,
      localFallback: suspend () -> T,
      syncToLocal: suspend (T) -> Unit
  ): T {
    // Fast-fail: If offline, go straight to local DB
    if (!NetworkUtils.isNetworkAvailable(context)) {
      Log.i(TAG, "Device offline, returning local data immediately for $operationName")
      return localFallback()
    }

    return try {
      // Timeout safety: Try network with time limit
      val result = withTimeout(TIMEOUT_MS) { remoteCall() }
      // Sync: Save to local for next time
      syncToLocal(result)
      result
    } catch (e: Throwable) {
      // Fallback: On network error/timeout/not found, return local data
      if (isNetworkOrTimeout(e) || e is NoSuchElementException) {
        Log.w(
            TAG, "Network error, timeout, or not found during $operationName, using local data", e)
        localFallback()
      } else {
        throw e
      }
    }
  }

  /**
   * Performs a write operation with fast-fail logic.
   *
   * Strategy:
   * 1. Block offline writes: Throw immediately if offline
   * 2. Remote first: Attempt remote operation with timeout
   * 3. Local sync: Sync to local after successful remote operation
   */
  private suspend fun performWrite(
      operationName: String,
      remoteCall: suspend () -> Unit,
      localSync: suspend () -> Unit
  ) {
    // Block offline writes
    if (!NetworkUtils.isNetworkAvailable(context)) {
      throw UnsupportedOperationException(
          "ReviewsRepositoryHybrid: Cannot $operationName offline. Please connect to the internet.")
    }

    try {
      // Remote first
      withTimeout(TIMEOUT_MS) { remoteCall() }
      // Local sync (best effort - don't fail if this fails)
      try {
        localSync()
      } catch (e: Exception) {
        Log.w(TAG, "Error syncing $operationName to local DB", e)
        // Don't crash if local sync fails, main action succeeded
      }
    } catch (e: Throwable) {
      if (isNetworkOrTimeout(e)) {
        Log.w(TAG, "Network error or timeout during $operationName", e)
        throw UnsupportedOperationException(
            "ReviewsRepositoryHybrid: Cannot $operationName offline. Please connect to the internet.",
            e)
      }
      throw e
    }
  }

  /** Checks if the exception is network-related or a timeout. */
  private fun isNetworkOrTimeout(e: Throwable): Boolean {
    val exception = e as? Exception ?: Exception(e.message)
    return NetworkUtils.isNetworkException(exception) || e is TimeoutCancellationException
  }

  override fun getNewUid(): String {
    // getNewUid() in Firestore doesn't make network calls, so it should always work when available
    // But if somehow it fails, we know local doesn't support it, so throw directly
    return try {
      remoteRepository.getNewUid()
    } catch (e: Exception) {
      if (NetworkUtils.isNetworkException(e)) {
        Log.w(TAG, "Network error getting new UID", e)
        throw UnsupportedOperationException(
            "ReviewsRepositoryHybrid: Cannot generate new UIDs offline. Please connect to the internet.",
            e)
      } else {
        throw e
      }
    }
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
   * Uses the existing [ReviewsRepositoryLocal.addReview] method which handles syncing.
   *
   * @param reviews The reviews to sync to local storage.
   */
  private suspend fun syncReviewsToLocal(reviews: List<Review>) {
    if (reviews.isEmpty()) return

    try {
      reviews.forEach { review ->
        try {
          localRepository.addReview(review)
        } catch (e: Exception) {
          Log.w(TAG, "Error syncing review ${review.uid} to local", e)
          // Continue with other reviews even if one fails
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error syncing reviews to local", e)
      // Don't throw - syncing is best effort
    }
  }
}
