package com.android.mySwissDorm.model.rental

import android.content.Context
import android.util.Log
import com.android.mySwissDorm.model.HybridRepositoryBase
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.utils.LastSyncTracker

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
   * Fetches owner names for listings that don't have them, then uses the existing
   * [RentalListingRepositoryLocal.addRentalListing] method which handles syncing. Also records the
   * sync timestamp for the offline banner.
   *
   * @param listings The rental listings to sync to local storage.
   */
  private suspend fun syncListingsToLocal(listings: List<RentalListing>) {
    if (listings.isEmpty()) return

    try {
      listings.forEach { listing ->
        try {
          // Fetch owner name if missing
          val listingWithOwnerName =
              if (listing.ownerName == null) {
                try {
                  val profile = ProfileRepositoryProvider.repository.getProfile(listing.ownerId)
                  val ownerName = "${profile.userInfo.name} ${profile.userInfo.lastName}".trim()
                  listing.copy(ownerName = ownerName.takeIf { it.isNotEmpty() })
                } catch (e: Exception) {
                  Log.w(TAG, "Error fetching owner name for listing ${listing.uid}", e)
                  listing // Store null if profile fetch fails - ViewModels will handle fallback
                }
              } else {
                listing
              }

          localRepository.addRentalListing(listingWithOwnerName)
        } catch (e: Exception) {
          Log.w(TAG, "Error syncing listing ${listing.uid} to local", e)
          // Continue with other listings even if one fails
        }
      }
      // Record successful sync timestamp
      LastSyncTracker.recordSync(context)
    } catch (e: Exception) {
      Log.w(TAG, "Error syncing listings to local", e)
      // Don't throw - syncing is best effort
    }
  }
}
