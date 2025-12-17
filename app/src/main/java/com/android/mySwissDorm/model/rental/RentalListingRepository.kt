package com.android.mySwissDorm.model.rental

import com.android.mySwissDorm.model.map.Location

interface RentalListingRepository {

  /** Generates and returns a new unique identifier for a Rental post. */
  fun getNewUid(): String

  /**
   * Retrieves all Rental posts from the repository.
   *
   * @return A list of all Rental posts.
   */
  suspend fun getAllRentalListings(): List<RentalListing>

  /**
   * Retrieves all Rental listings by specific location from the repository.
   *
   * @param location used as centered location.
   * @param radius used for the whole circle of locations
   * @return A list of rental listings.
   */
  suspend fun getAllRentalListingsByLocation(
      location: Location,
      radius: Double
  ): List<RentalListing>
  /**
   * Retrieves all Rental posts created by a specific user from the repository.
   *
   * @param userId The unique identifier of the user whose rental posts to retrieve.
   * @return A list of all Rental posts created by the specified user.
   */
  suspend fun getAllRentalListingsByUser(userId: String): List<RentalListing>

  /**
   * Retrieves a specific rental post by its unique identifier.
   *
   * @param rentalPostId The unique identifier of the rental post item to retrieve.
   * @return The rental post item with the specified identifier.
   * @throws Exception if the rental post item is not found.
   */
  suspend fun getRentalListing(rentalPostId: String): RentalListing

  /**
   * Retrieves all Rental posts from the repository, filtered based on bidirectional blocking. Only
   * returns listings where neither the current user nor the listing owner has blocked the other.
   *
   * @param userId The unique identifier of the current user. If null, returns all listings.
   * @return A list of Rental posts visible to the user.
   */
  suspend fun getAllRentalListingsForUser(userId: String?): List<RentalListing>

  /**
   * Retrieves a specific rental post by its unique identifier, if visible to the user. Returns the
   * listing only if neither the current user nor the listing owner has blocked the other.
   *
   * @param rentalPostId The unique identifier of the rental post item to retrieve.
   * @param userId The unique identifier of the current user. If null, returns the listing if it
   *   exists.
   * @return The rental post item with the specified identifier.
   * @throws Exception if the rental post item is not found or if blocked.
   */
  suspend fun getRentalListingForUser(rentalPostId: String, userId: String?): RentalListing

  /**
   * Adds a new rental post item to the repository.
   *
   * @param rentalPost The rental post item to add.
   */
  suspend fun addRentalListing(rentalPost: RentalListing)

  /**
   * Edits an existing rental post item in the repository.
   *
   * @param rentalPostId The unique identifier of the rental post item to edit.
   * @param newValue The new value for the rental post item.
   * @throws Exception if the rental post item is not found.
   */
  suspend fun editRentalListing(rentalPostId: String, newValue: RentalListing)

  /**
   * Deletes a rental post item from the repository.
   *
   * @param rentalPostId The unique identifier of the rental post item to delete.
   * @throws Exception if the rental post item is not found.
   */
  suspend fun deleteRentalListing(rentalPostId: String)
}
