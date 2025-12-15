package com.android.mySwissDorm.ui.utils

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.WalkingRouteServiceProvider
import com.android.mySwissDorm.model.poi.DistanceService
import com.android.mySwissDorm.model.poi.POIDistance
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider

/**
 * Utility function to calculate POI distances for a given location. This function is shared between
 * ViewListingViewModel and ViewResidencyViewModel to avoid code duplication.
 *
 * @param location The location to calculate distances from.
 * @param userUniversityName The user's university name, or null if not available or guest user.
 * @param locationName A name identifier for logging purposes (e.g., listing ID or residency name).
 * @return List of POI distances, or empty list if calculation fails.
 */
suspend fun calculatePOIDistances(
    location: Location,
    userUniversityName: String?,
    locationName: String
): List<POIDistance> {
  return try {
    val distanceService =
        DistanceService(
            universitiesRepository = UniversitiesRepositoryProvider.repository,
            walkingRouteService = WalkingRouteServiceProvider.service)
    val distances = distanceService.calculateDistancesToPOIs(location, userUniversityName)
    Log.d(
        "POIDistanceCalculator",
        "Calculated ${distances.size} POI distances for $locationName at lat=${location.latitude}, lng=${location.longitude}${if (userUniversityName != null) " (user university: $userUniversityName)" else " (showing 2 nearest universities)"}")
    distances
  } catch (e: Exception) {
    Log.e("POIDistanceCalculator", "Error calculating POI distances for $locationName", e)
    emptyList()
  }
}
