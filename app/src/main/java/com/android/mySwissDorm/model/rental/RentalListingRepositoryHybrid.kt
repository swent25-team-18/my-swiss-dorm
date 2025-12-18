package com.android.mySwissDorm.model.rental

import android.content.Context
import android.util.Log
import com.android.mySwissDorm.model.HybridRepositoryBase
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.utils.LastSyncTracker
import com.android.mySwissDorm.utils.NetworkUtils

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
    context: Context,
    private val remoteRepository: RentalListingRepositoryFirestore,
    private val localRepository: RentalListingRepositoryLocal
) :
    HybridRepositoryBase<RentalListing>(context, "RentalListingRepository"),
    RentalListingRepository {

  override fun getNewUid(): String {
    return getNewUidWithNetworkCheck { remoteRepository.getNewUid() }
  }

  override suspend fun getAllRentalListings(): List<RentalListing> =
      performRead(
          operationName = "getAllRentalListings",
          remoteCall = { remoteRepository.getAllRentalListings() },
          localFallback = { localRepository.getAllRentalListings() },
          syncToLocal = { listings -> syncListingsToLocal(listings, isFullSync = true) })

  override suspend fun getAllRentalListingsByLocation(
      location: Location,
      radius: Double
  ): List<RentalListing> =
      performRead(
          operationName = "getAllRentalListingsByLocation",
          remoteCall = { remoteRepository.getAllRentalListingsByLocation(location, radius) },
          localFallback = { localRepository.getAllRentalListingsByLocation(location, radius) },
          syncToLocal = { listings -> syncListingsToLocal(listings, isFullSync = false) })

  override suspend fun getAllRentalListingsByUser(userId: String): List<RentalListing> =
      performRead(
          operationName = "getAllRentalListingsByUser",
          remoteCall = { remoteRepository.getAllRentalListingsByUser(userId) },
          localFallback = { localRepository.getAllRentalListingsByUser(userId) },
          syncToLocal = { listings -> syncListingsToLocal(listings, isFullSync = false) })

  override suspend fun getRentalListing(rentalPostId: String): RentalListing =
      performRead(
          operationName = "getRentalListing",
          remoteCall = { remoteRepository.getRentalListing(rentalPostId) },
          localFallback = { localRepository.getRentalListing(rentalPostId) },
          syncToLocal = { listing -> syncListingsToLocal(listOf(listing), isFullSync = false) })

  override suspend fun addRentalListing(rentalPost: RentalListing) =
      performWrite(
          operationName = "addRentalListing",
          remoteCall = { remoteRepository.addRentalListing(rentalPost) },
          localSync = { syncListingsToLocal(listOf(rentalPost), isFullSync = false) })

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

  override suspend fun getAllRentalListingsForUser(userId: String?): List<RentalListing> =
      performRead(
          operationName = "getAllRentalListingsForUser",
          remoteCall = {
            val allListings = remoteRepository.getAllRentalListings()
            if (userId == null) allListings else filterListingsByBlocking(allListings, userId)
          },
          localFallback = {
            val allListings = localRepository.getAllRentalListings()
            if (userId == null) allListings else filterListingsByBlocking(allListings, userId)
          },
          syncToLocal = { filteredListings ->
            // Only sync the filtered listings that the user can see
            syncListingsToLocal(filteredListings, isFullSync = false)
          })

  override suspend fun getRentalListingForUser(
      rentalPostId: String,
      userId: String?
  ): RentalListing {
    val listing = getRentalListing(rentalPostId)
    if (userId == null || userId == listing.ownerId) return listing

    // Check bidirectional blocking
    val isBlocked = isBlockedBidirectionally(listing.ownerId, userId)
    if (isBlocked) {
      throw NoSuchElementException(
          "RentalListingRepositoryHybrid: Listing $rentalPostId is not available due to blocking restrictions")
    }
    return listing
  }

  /**
   * Filters listings based on bidirectional blocking.
   *
   * @param listings The listings to filter.
   * @param userId The current user's ID.
   * @return Filtered list of listings visible to the user.
   */
  private suspend fun filterListingsByBlocking(
      listings: List<RentalListing>,
      userId: String
  ): List<RentalListing> {
    // Load current user's blocked list once for efficiency
    val currentUserBlockedList =
        runCatching { ProfileRepositoryProvider.repository.getBlockedUserIds(userId) }
            .onFailure { Log.w(tag, "Failed to fetch current user's blocked list", it) }
            .getOrDefault(emptyList())

    val blockedCache = mutableMapOf<String, Boolean>()
    return listings.filter { listing ->
      if (listing.ownerId == userId) return@filter true
      !isBlockedBidirectionally(listing.ownerId, userId, currentUserBlockedList, blockedCache)
    }
  }

  /**
   * Syncs rental listings to the local database for offline access.
   *
   * This method is called after successful remote operations to ensure data is available offline.
   * Fetches owner names for listings that don't have them, then uses the existing
   * [RentalListingRepositoryLocal.addRentalListing] method which handles syncing. Also records the
   * sync timestamp for the offline banner.
   *
   * When [isFullSync] is true (e.g., from [getAllRentalListings]), this method will also delete any
   * local listings that are not in the remote list, ensuring the local database stays in sync with
   * Firestore deletions.
   *
   * @param listings The rental listings to sync to local storage.
   * @param isFullSync Whether this represents a complete sync of all listings. If true, local
   *   listings not in this list will be deleted.
   */
  private suspend fun syncListingsToLocal(listings: List<RentalListing>, isFullSync: Boolean) {
    try {
      // If this is a full sync, delete local listings that are no longer in Firestore
      if (isFullSync) {
        try {
          val remoteIds = listings.map { it.uid }.toSet()
          val localListingsBefore = localRepository.getAllRentalListings()
          val localIdsBefore = localListingsBefore.map { it.uid }.toSet()
          localRepository.deleteRentalListingsNotIn(remoteIds.toList())
          val deletedIds = localIdsBefore - remoteIds
          if (deletedIds.isNotEmpty()) {
            Log.d(
                tag,
                "[syncListingsToLocal] Deleted ${deletedIds.size} stale listings during full sync")
          }
        } catch (e: Exception) {
          Log.w(tag, "[syncListingsToLocal] Error deleting stale listings during full sync", e)
          // Continue with sync even if deletion fails
        }
      }

      if (listings.isEmpty()) {
        // Record sync even if no listings to sync (might have just deleted stale data)
        LastSyncTracker.recordSync(context)
        return
      }

      listings.forEach { listing ->
        try {
          // Fetch owner name if missing (only when online)
          val listingWithOwnerName =
              if (listing.ownerName == null && NetworkUtils.isNetworkAvailable(context)) {
                try {
                  val profile = ProfileRepositoryProvider.repository.getProfile(listing.ownerId)
                  val ownerName = "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
                  listing.copy(ownerName = ownerName.takeIf { it.isNotEmpty() })
                } catch (e: Exception) {
                  Log.w(tag, "Error fetching owner name for listing ${listing.uid}", e)
                  listing // Store null if profile fetch fails - ViewModels will handle fallback
                }
              } else {
                listing
              }

          localRepository.addRentalListing(listingWithOwnerName)
        } catch (e: Exception) {
          Log.w(tag, "Error syncing listing ${listing.uid} to local", e)
          // Continue with other listings even if one fails
        }
      }
      // Record successful sync timestamp
      LastSyncTracker.recordSync(context)
    } catch (e: Exception) {
      Log.w(tag, "Error syncing listings to local", e)
      // Don't throw - syncing is best effort
    }
  }
}
