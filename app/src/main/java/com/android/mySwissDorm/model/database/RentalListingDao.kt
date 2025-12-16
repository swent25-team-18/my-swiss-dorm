package com.android.mySwissDorm.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object for [RentalListingEntity] operations.
 *
 * Provides methods to query, insert, update, and delete rental listings from the local database.
 */
@Dao
interface RentalListingDao {
  /**
   * Retrieves all rental listings from the database.
   *
   * @return A list of all [RentalListingEntity] objects in the database.
   */
  @Query("SELECT * FROM rental_listings")
  suspend fun getAllRentalListings(): List<RentalListingEntity>

  /**
   * Retrieves all rental listings created by a specific user.
   *
   * @param userId The unique identifier of the user.
   * @return A list of [RentalListingEntity] objects created by the specified user.
   */
  @Query("SELECT * FROM rental_listings WHERE ownerId = :userId")
  suspend fun getAllRentalListingsByUser(userId: String): List<RentalListingEntity>

  /**
   * Retrieves a single rental listing by its unique identifier.
   *
   * @param listingId The unique identifier of the listing.
   * @return The [RentalListingEntity] with the specified ID, or null if not found.
   */
  @Query("SELECT * FROM rental_listings WHERE uid = :listingId")
  suspend fun getRentalListing(listingId: String): RentalListingEntity?

  /**
   * Inserts a single rental listing into the database.
   *
   * If a listing with the same [RentalListingEntity.uid] already exists, it will be replaced.
   *
   * @param listing The [RentalListingEntity] to insert.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRentalListing(listing: RentalListingEntity)

  /**
   * Inserts multiple rental listings into the database.
   *
   * If any listing with the same [RentalListingEntity.uid] already exists, it will be replaced.
   *
   * @param listings The list of [RentalListingEntity] objects to insert.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRentalListings(listings: List<RentalListingEntity>)

  /**
   * Updates an existing rental listing in the database.
   *
   * @param listing The [RentalListingEntity] to update. Must have an existing
   *   [RentalListingEntity.uid].
   */
  @Update suspend fun updateRentalListing(listing: RentalListingEntity)

  /**
   * Deletes a specific rental listing from the database.
   *
   * @param listingId The unique identifier of the listing to delete.
   */
  @Query("DELETE FROM rental_listings WHERE uid = :listingId")
  suspend fun deleteRentalListing(listingId: String)

  /**
   * Deletes all rental listings whose UIDs are not in the provided list.
   *
   * This is used during full sync operations to remove items that have been deleted from Firestore.
   *
   * @param keepIds The set of UIDs to keep. All other listings will be deleted.
   */
  @Query("DELETE FROM rental_listings WHERE uid NOT IN (:keepIds)")
  suspend fun deleteRentalListingsNotIn(keepIds: List<String>)
}
