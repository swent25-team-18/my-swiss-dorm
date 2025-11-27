package com.android.mySwissDorm.model.rental

import android.content.Context
import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.utils.NetworkUtils
import kotlin.NoSuchElementException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Hybrid implementation of [RentalListingRepository] that combines remote (Firestore) and local
 * (Room) data sources.
 *
 * This repository uses a fast-fail approach: if the device is offline, it immediately falls back to
 * the local repository without attempting network operations. When online, it attempts remote
 * operations first and falls back to local on network errors or timeouts.
 *
 * @property context The application context for checking network connectivity.
 * @property remoteRepository The Firestore-backed repository for online operations.
 * @property localRepository The Room-backed repository for offline operations.
 */
class RentalListingRepositoryHybrid(
    private val context: Context,
    private val remoteRepository: RentalListingRepositoryFirestore,
    private val localRepository: RentalListingRepositoryLocal
) : RentalListingRepository {

  private val TAG = "RentalListingRepositoryHybrid"
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
          "RentalListingRepositoryHybrid: Cannot $operationName offline. Please connect to the internet.")
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
            "RentalListingRepositoryHybrid: Cannot $operationName offline. Please connect to the internet.",
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
            "RentalListingRepositoryHybrid: Cannot generate new UIDs offline. Please connect to the internet.",
            e)
      } else {
        throw e
      }
    }
  }

  override suspend fun getAllRentalListings(): List<RentalListing> =
      performRead(
          operationName = "getAllRentalListings",
          remoteCall = { remoteRepository.getAllRentalListings() },
          localFallback = { localRepository.getAllRentalListings() },
          syncToLocal = { listings -> syncListingsToLocal(listings) })

  override suspend fun getAllRentalListingsByLocation(
      location: Location,
      radius: Double
  ): List<RentalListing> =
      performRead(
          operationName = "getAllRentalListingsByLocation",
          remoteCall = { remoteRepository.getAllRentalListingsByLocation(location, radius) },
          localFallback = { localRepository.getAllRentalListingsByLocation(location, radius) },
          syncToLocal = { listings -> syncListingsToLocal(listings) })

  override suspend fun getAllRentalListingsByUser(userId: String): List<RentalListing> =
      performRead(
          operationName = "getAllRentalListingsByUser",
          remoteCall = { remoteRepository.getAllRentalListingsByUser(userId) },
          localFallback = { localRepository.getAllRentalListingsByUser(userId) },
          syncToLocal = { listings -> syncListingsToLocal(listings) })

  override suspend fun getRentalListing(rentalPostId: String): RentalListing =
      performRead(
          operationName = "getRentalListing",
          remoteCall = { remoteRepository.getRentalListing(rentalPostId) },
          localFallback = { localRepository.getRentalListing(rentalPostId) },
          syncToLocal = { listing -> syncListingsToLocal(listOf(listing)) })

  override suspend fun addRentalListing(rentalPost: RentalListing) =
      performWrite(
          operationName = "addRentalListing",
          remoteCall = { remoteRepository.addRentalListing(rentalPost) },
          localSync = { syncListingsToLocal(listOf(rentalPost)) })

  override suspend fun editRentalListing(rentalPostId: String, newValue: RentalListing) =
      performWrite(
          operationName = "editRentalListing",
          remoteCall = { remoteRepository.editRentalListing(rentalPostId, newValue) },
          localSync = { localRepository.editRentalListing(rentalPostId, newValue) })

  override suspend fun deleteRentalListing(rentalPostId: String) =
      performWrite(
          operationName = "deleteRentalListing",
          remoteCall = { remoteRepository.deleteRentalListing(rentalPostId) },
          localSync = { localRepository.deleteRentalListing(rentalPostId) })

  /**
   * Syncs rental listings to the local database for offline access.
   *
   * This method is called after successful remote operations to ensure data is available offline.
   * Uses the existing [RentalListingRepositoryLocal.addRentalListing] method which handles syncing.
   *
   * @param listings The rental listings to sync to local storage.
   */
  private suspend fun syncListingsToLocal(listings: List<RentalListing>) {
    if (listings.isEmpty()) return

    try {
      listings.forEach { listing ->
        try {
          localRepository.addRentalListing(listing)
        } catch (e: Exception) {
          Log.w(TAG, "Error syncing listing ${listing.uid} to local", e)
          // Continue with other listings even if one fails
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error syncing listings to local", e)
      // Don't throw - syncing is best effort
    }
  }
}
