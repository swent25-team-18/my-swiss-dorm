package com.android.mySwissDorm.model.residency

import com.android.mySwissDorm.model.map.Location

/** Represents a repository that manages Residency items. */
interface ResidenciesRepository {
  /**
   * Retrieves all Residency items from the repository.
   *
   * @return A list of all Residency items.
   */
  suspend fun getAllResidencies(): List<Residency>

  /**
   * Retrieves all Residency located within a given radius from a specified location.
   *
   * @param location The reference location from which distances are calculated.
   * @param radius The maximum distance (in kilometers) within which residencies should be included.
   * @return A list of all Residency items located within the specified radius.
   */
  suspend fun getAllResidenciesNearLocation(location: Location, radius: Double): List<Residency>

  /**
   * Retrieves a specific Residency item by its unique identifier.
   *
   * @param residencyName The unique identifier of the Residency item to retrieve.
   * @return The Residency item with the specified identifier.
   * @throws Exception if the Residency item is not found.
   */
  suspend fun getResidency(residencyName: String): Residency

  /**
   * Adds a new [Residency] item to the repository.
   *
   * @param residency The residency to persist.
   */
  suspend fun addResidency(residency: Residency)

  /**
   * Updates an existing [Residency] item in the repository.
   *
   * @param residency The residency to update with new data.
   */
  suspend fun updateResidency(residency: Residency)
}
