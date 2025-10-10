package com.android.mySwissDorm.model.rental

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
   * Retrieves a specific rental post by its unique identifier.
   *
   * @param rentalPostId The unique identifier of the rental post item to retrieve.
   * @return The rental post item with the specified identifier.
   * @throws Exception if the rental post item is not found.
   */
  suspend fun getRentalListing(rentalPostId: String): RentalListing

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
