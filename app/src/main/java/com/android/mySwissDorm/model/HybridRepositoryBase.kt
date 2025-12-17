package com.android.mySwissDorm.model

import android.content.Context
import android.util.Log
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.utils.NetworkUtils
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Base class for hybrid repositories that combine remote and local data sources.
 *
 * This class provides common functionality for fast-fail offline detection, timeout handling, and
 * fallback logic. Subclasses should implement the specific repository interface and provide
 * type-specific sync operations.
 *
 * @param T The type of entity managed by this repository (e.g., Review, RentalListing).
 * @property context The application context for checking network connectivity.
 * @property repositoryName The name of the repository (used in error messages and logging).
 */
abstract class HybridRepositoryBase<T>(
    protected val context: Context,
    private val repositoryName: String
) {
  protected val tag = "${repositoryName}Hybrid"
  protected val timeoutMs = 2000L

  /**
   * Performs a read operation with fast-fail and fallback logic.
   *
   * Strategy:
   * 1. Fast-fail: If offline, return local data immediately
   * 2. Timeout safety: Try remote with timeout
   * 3. Sync: Save successful remote results to local
   * 4. Fallback: On network error/timeout, return local data
   */
  protected suspend fun <R> performRead(
      operationName: String,
      remoteCall: suspend () -> R,
      localFallback: suspend () -> R,
      syncToLocal: suspend (R) -> Unit
  ): R {
    // Fast-fail: If offline, go straight to local DB
    if (!NetworkUtils.isNetworkAvailable(context)) {
      Log.i(tag, "Device offline, returning local data immediately for $operationName")
      return localFallback()
    }

    return try {
      // Timeout safety: Try network with time limit
      val result = withTimeout(timeoutMs) { remoteCall() }
      // Sync: Save to local for next time
      syncToLocal(result)
      result
    } catch (e: Throwable) {
      // Fallback: On network error/timeout/not found, return local data
      if (isNetworkOrTimeout(e) || e is NoSuchElementException) {
        Log.w(
            tag, "Network error, timeout, or not found during $operationName, using local data", e)
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
  protected suspend fun performWrite(
      operationName: String,
      remoteCall: suspend () -> Unit,
      localSync: suspend () -> Unit
  ) {
    // Block offline writes
    if (!NetworkUtils.isNetworkAvailable(context)) {
      throw UnsupportedOperationException(
          "$repositoryName: Cannot $operationName offline. Please connect to the internet.")
    }

    try {
      // Remote first
      withTimeout(timeoutMs) { remoteCall() }
      // Local sync (best effort - don't fail if this fails)
      try {
        localSync()
      } catch (e: Exception) {
        Log.w(tag, "Error syncing $operationName to local DB", e)
        // Don't crash if local sync fails, main action succeeded
      }
    } catch (e: Throwable) {
      if (isNetworkOrTimeout(e)) {
        Log.w(tag, "Network error or timeout during $operationName", e)
        throw UnsupportedOperationException(
            "$repositoryName: Cannot $operationName offline. Please connect to the internet.", e)
      }
      throw e
    }
  }

  /**
   * Handles getNewUid() with network error detection.
   *
   * getNewUid() in Firestore doesn't make network calls, but we still check for network errors in
   * case of unexpected failures.
   */
  protected fun getNewUidWithNetworkCheck(remoteCall: () -> String): String {
    return try {
      remoteCall()
    } catch (e: Exception) {
      if (NetworkUtils.isNetworkException(e)) {
        Log.w(tag, "Network error getting new UID", e)
        throw UnsupportedOperationException(
            "$repositoryName: Cannot generate new UIDs offline. Please connect to the internet.", e)
      } else {
        throw e
      }
    }
  }

  /** Checks if the exception is network-related or a timeout. */
  private fun isNetworkOrTimeout(e: Throwable): Boolean {
    val exception = e as? Exception ?: Exception(e.message)
    return NetworkUtils.isNetworkException(exception) || e is TimeoutCancellationException
  }

  /**
   * Checks if two users have blocked each other bidirectionally.
   *
   * @param ownerId The owner of the content.
   * @param userId The current user's ID.
   * @param currentUserBlockedList Pre-loaded list of users blocked by current user (optional).
   * @param blockedCache Cache for owner's blocked lists (optional).
   * @return true if either user has blocked the other.
   */
  protected suspend fun isBlockedBidirectionally(
      ownerId: String,
      userId: String,
      currentUserBlockedList: List<String>? = null,
      blockedCache: MutableMap<String, Boolean>? = null
  ): Boolean {
    // Check if current user has blocked owner
    val currentUserBlockedOwner =
        currentUserBlockedList?.contains(ownerId)
            ?: runCatching { ProfileRepositoryProvider.repository.getBlockedUserIds(userId) }
                .getOrDefault(emptyList())
                .contains(ownerId)

    // Check if owner has blocked current user
    // Use cache if available, otherwise fetch directly
    val ownerBlockedCurrentUser =
        if (blockedCache != null) {
          blockedCache.getOrPut(ownerId) {
            runCatching { ProfileRepositoryProvider.repository.getBlockedUserIds(ownerId) }
                .onFailure { Log.w(tag, "Failed to fetch blocked list for owner $ownerId", it) }
                .getOrDefault(emptyList())
                .contains(userId)
          }
        } else {
          runCatching { ProfileRepositoryProvider.repository.getBlockedUserIds(ownerId) }
              .getOrDefault(emptyList())
              .contains(userId)
        }

    return currentUserBlockedOwner || ownerBlockedCurrentUser
  }
}
