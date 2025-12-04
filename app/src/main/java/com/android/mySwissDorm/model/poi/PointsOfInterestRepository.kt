package com.android.mySwissDorm.model.poi

import com.android.mySwissDorm.model.map.Location

/** Represents a repository that manages PointOfInterest items. */
interface PointsOfInterestRepository {
  /**
   * Retrieves all PointOfInterest items from the repository.
   *
   * @return A list of all PointOfInterest items.
   */
  suspend fun getAllPointsOfInterest(): List<PointOfInterest>

  /**
   * Retrieves PointOfInterest items by type.
   *
   * @param type The type of POI to retrieve.
   * @return A list of PointOfInterest items of the specified type.
   */
  suspend fun getPointsOfInterestByType(type: POIType): List<PointOfInterest>

  /**
   * Retrieves all PointOfInterest items located within a given radius from a specified location.
   *
   * @param location The reference location from which distances are calculated.
   * @param radius The maximum distance (in kilometers) within which POIs should be included.
   * @return A list of all PointOfInterest items located within the specified radius.
   */
  suspend fun getAllPointsOfInterestByLocation(
      location: Location,
      radius: Double
  ): List<PointOfInterest>
}
