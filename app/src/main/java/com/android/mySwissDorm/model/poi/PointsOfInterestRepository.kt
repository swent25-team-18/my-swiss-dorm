package com.android.mySwissDorm.model.poi

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
}
