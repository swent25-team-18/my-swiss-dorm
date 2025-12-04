package com.android.mySwissDorm.model.supermarket

import com.android.mySwissDorm.model.map.Location

/** Represents a repository that manages Supermarket items. */
interface SupermarketsRepository {
  /**
   * Retrieves all Supermarket items from the repository.
   *
   * @return A list of all Supermarket items.
   */
  suspend fun getAllSupermarkets(): List<Supermarket>

  /**
   * Adds a new Supermarket item to the repository.
   *
   * @param supermarket The Supermarket item to add.
   */
  suspend fun addSupermarket(supermarket: Supermarket)

  /**
   * Retrieves all Supermarket items located within a given radius from a specified location.
   *
   * @param location The reference location from which distances are calculated.
   * @param radius The maximum distance (in kilometers) within which supermarkets should be
   *   included.
   * @return A list of all Supermarket items located within the specified radius.
   */
  suspend fun getAllSupermarketsByLocation(location: Location, radius: Double): List<Supermarket>
}
