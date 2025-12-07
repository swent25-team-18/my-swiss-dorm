package com.android.mySwissDorm.model.rental

import android.util.Log
import com.android.mySwissDorm.model.database.RentalListingDao
import com.android.mySwissDorm.model.database.RentalListingEntity
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.distanceTo

/**
 * Room-backed local implementation of [RentalListingRepository].
 *
 * This repository provides offline access to rental listings by storing them in a local Room
 * database. It implements the same interface as [RentalListingRepositoryFirestore], allowing
 * seamless switching between local and remote data sources.
 */
class RentalListingRepositoryLocal(private val rentalListingDao: RentalListingDao) :
    RentalListingRepository {

  /**
   * Generates a new unique identifier for a rental listing.
   *
   * This method is not supported when using the local repository, as new content cannot be created
   * offline.
   *
   * @throws UnsupportedOperationException Always, as new content cannot be created offline.
   */
  override fun getNewUid(): String {
    throw UnsupportedOperationException(
        "RentalListingRepositoryLocal: Cannot create new listings offline. Please connect to the internet.")
  }

  /**
   * Retrieves all rental listings from the local database.
   *
   * @return A list of all rental listings stored locally.
   */
  override suspend fun getAllRentalListings(): List<RentalListing> {
    return rentalListingDao.getAllRentalListings().map { it.toRentalListing() }
  }

  /**
   * Retrieves all rental listings within a specified radius of a location.
   *
   * This method fetches all listings from the database and filters them by distance.
   *
   * @param location The center location for the search.
   * @param radius The maximum distance in kilometers from the center location.
   * @return A list of rental listings within the specified radius.
   */
  override suspend fun getAllRentalListingsByLocation(
      location: Location,
      radius: Double
  ): List<RentalListing> {
    val allListings = getAllRentalListings()
    return allListings.filter { listing ->
      try {
        val distance = location.distanceTo(listing.location)
        distance <= radius
      } catch (e: Exception) {
        Log.e(
            "RentalListingRepositoryLocal",
            "Error calculating distance for listing ${listing.uid}",
            e)
        false
      }
    }
  }

  /**
   * Retrieves all rental listings created by a specific user.
   *
   * @param userId The unique identifier of the user.
   * @return A list of rental listings created by the specified user.
   */
  override suspend fun getAllRentalListingsByUser(userId: String): List<RentalListing> {
    return rentalListingDao.getAllRentalListingsByUser(userId).map { it.toRentalListing() }
  }

  override suspend fun getAllRentalListingsByResidency(residencyName: String): List<RentalListing> {
    val allListings = getAllRentalListings()
    return allListings.filter { it.residencyName == residencyName && it.status.name == "POSTED" }
  }

  /**
   * Retrieves a specific rental listing by its unique identifier.
   *
   * @param rentalPostId The unique identifier of the rental listing.
   * @return The rental listing with the specified identifier.
   * @throws NoSuchElementException if the rental listing is not found.
   */
  override suspend fun getRentalListing(rentalPostId: String): RentalListing {
    return rentalListingDao.getRentalListing(rentalPostId)?.toRentalListing()
        ?: throw NoSuchElementException(
            "RentalListingRepositoryLocal: Rental listing $rentalPostId not found")
  }

  /**
   * Adds a rental listing to the local database.
   *
   * This method can be used to store listings that already have a UID (e.g., when syncing from
   * remote). It cannot be used to create new listings offline, as [getNewUid] will throw an
   * exception.
   *
   * @param rentalPost The rental listing to add. Must have a valid UID.
   */
  override suspend fun addRentalListing(rentalPost: RentalListing) {
    rentalListingDao.insertRentalListing(RentalListingEntity.fromRentalListing(rentalPost))
  }

  /**
   * Edits an existing rental listing in the local database.
   *
   * @param rentalPostId The unique identifier of the rental listing to edit.
   * @param newValue The new value for the rental listing.
   * @throws IllegalArgumentException if the rentalPostId doesn't match newValue.uid.
   * @throws NoSuchElementException if the rental listing is not found.
   */
  override suspend fun editRentalListing(rentalPostId: String, newValue: RentalListing) {
    if (newValue.uid != rentalPostId) {
      throw IllegalArgumentException(
          "RentalListingRepositoryLocal: Provided rentalPostId does not match newValue.uid")
    }
    // Verify the listing exists before updating
    if (rentalListingDao.getRentalListing(rentalPostId) == null) {
      throw NoSuchElementException(
          "RentalListingRepositoryLocal: Rental listing $rentalPostId not found")
    }
    rentalListingDao.updateRentalListing(RentalListingEntity.fromRentalListing(newValue))
  }

  /**
   * Deletes a rental listing from the local database.
   *
   * @param rentalPostId The unique identifier of the rental listing to delete.
   */
  override suspend fun deleteRentalListing(rentalPostId: String) {
    rentalListingDao.deleteRentalListing(rentalPostId)
  }
}
