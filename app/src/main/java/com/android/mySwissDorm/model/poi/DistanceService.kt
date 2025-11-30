package com.android.mySwissDorm.model.poi

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.WalkingRouteService
import com.android.mySwissDorm.model.map.distanceTo
import com.android.mySwissDorm.model.market.MarketsRepository
import com.android.mySwissDorm.model.supermarket.SupermarketsRepository
import com.android.mySwissDorm.model.university.UniversitiesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Represents a calculated walking time to a point of interest.
 *
 * @param poiName The name of the point of interest
 * @param walkingTimeMinutes The walking time in minutes
 * @param poiType The type of POI
 */
data class POIDistance(val poiName: String, val walkingTimeMinutes: Int, val poiType: POIType)

/**
 * Service for calculating walking times to points of interest. Uses static data from Firestore
 * collections (universities, supermarkets, markets). Calculates actual walking route times using
 * OpenRouteService API.
 */
class DistanceService(
    private val universitiesRepository: UniversitiesRepository,
    private val supermarketsRepository: SupermarketsRepository,
    private val marketsRepository: MarketsRepository,
    private val walkingRouteService: WalkingRouteService
) {

  /**
   * Calculates walking times from a given location to nearby points of interest. If
   * userUniversityName is provided, shows only that university. Otherwise, shows the 2 nearest
   * universities. Always shows the nearest Migros and nearest Denner. Queries Firestore collections
   * and calculates walking times using OpenRouteService API.
   *
   * @param location The location to calculate walking times from
   * @param userUniversityName Optional university name from user's profile. If provided, only this
   *   university is shown.
   * @return A list of POIDistance objects, sorted by walking time (nearest first)
   */
  suspend fun calculateDistancesToPOIs(
      location: Location,
      userUniversityName: String? = null
  ): List<POIDistance> {
    // Handle invalid coordinates
    if (location.latitude == 0.0 && location.longitude == 0.0) {
      Log.d("DistanceService", "Invalid coordinates (0.0, 0.0), returning empty list")
      return emptyList()
    }

    val allDistances = mutableListOf<POIDistance>()

    try {
      coroutineScope {
        // Get all universities and calculate walking times
        val universities = universitiesRepository.getAllUniversities()

        val universityPOIs =
            if (userUniversityName != null && userUniversityName.isNotBlank()) {
              // User has a university in their profile - show only that university
              val userUniversity =
                  universities.firstOrNull { it.name.equals(userUniversityName, ignoreCase = true) }
              if (userUniversity != null) {
                listOf(userUniversity)
              } else {
                Log.w(
                    "DistanceService",
                    "User's university '$userUniversityName' not found in database, showing 2 nearest instead")
                // Fallback: show 2 nearest if user's university not found
                universities.sortedBy { location.distanceTo(it.location) }.take(2)
              }
            } else {
              // User has no university in profile - show 2 nearest universities
              universities.sortedBy { location.distanceTo(it.location) }.take(2)
            }

        // Calculate walking times for universities in parallel
        val universityDistances =
            universityPOIs
                .map { university ->
                  async {
                    val timeMinutes =
                        walkingRouteService.calculateWalkingTimeMinutes(
                            location, university.location)
                    if (timeMinutes != null) {
                      POIDistance(
                          poiName = university.name,
                          walkingTimeMinutes = timeMinutes,
                          poiType = POIType.UNIVERSITY)
                    } else null
                  }
                }
                .awaitAll()
                .filterNotNull()

        allDistances.addAll(universityDistances)
        Log.d("DistanceService", "Calculated ${universityDistances.size} university walking times")

        // Get all supermarkets and find the nearest Migros and nearest Denner separately
        val supermarkets = supermarketsRepository.getAllSupermarkets()
        Log.d("DistanceService", "Found ${supermarkets.size} supermarkets in database")

        // Separate Migros and Denner
        val migrosSupermarkets =
            supermarkets.filter {
              it.name.contains("Migros", ignoreCase = true) &&
                  !it.name.contains("Denner", ignoreCase = true)
            }
        val dennerSupermarkets =
            supermarkets.filter {
              it.name.contains("Denner", ignoreCase = true) &&
                  !it.name.contains("Migros", ignoreCase = true)
            }

        Log.d(
            "DistanceService",
            "Found ${migrosSupermarkets.size} Migros and ${dennerSupermarkets.size} Denner supermarkets")

        // Find nearest Migros by calculating walking times
        val nearestMigros =
            if (migrosSupermarkets.isNotEmpty()) {
              val migrosTimes =
                  migrosSupermarkets
                      .map { supermarket ->
                        async {
                          val timeMinutes =
                              walkingRouteService.calculateWalkingTimeMinutes(
                                  location, supermarket.location)
                          if (timeMinutes != null) {
                            Pair(supermarket, timeMinutes)
                          } else null
                        }
                      }
                      .awaitAll()
                      .filterNotNull()

              migrosTimes
                  .minByOrNull { it.second }
                  ?.let { (supermarket, timeMinutes) ->
                    POIDistance(
                        poiName = supermarket.name,
                        walkingTimeMinutes = timeMinutes,
                        poiType = POIType.SUPERMARKET)
                  }
            } else null

        // Find nearest Denner by calculating walking times
        val nearestDenner =
            if (dennerSupermarkets.isNotEmpty()) {
              val dennerTimes =
                  dennerSupermarkets
                      .map { supermarket ->
                        async {
                          val timeMinutes =
                              walkingRouteService.calculateWalkingTimeMinutes(
                                  location, supermarket.location)
                          if (timeMinutes != null) {
                            Pair(supermarket, timeMinutes)
                          } else null
                        }
                      }
                      .awaitAll()
                      .filterNotNull()

              dennerTimes
                  .minByOrNull { it.second }
                  ?.let { (supermarket, timeMinutes) ->
                    POIDistance(
                        poiName = supermarket.name,
                        walkingTimeMinutes = timeMinutes,
                        poiType = POIType.SUPERMARKET)
                  }
            } else null

        if (nearestMigros != null) {
          allDistances.add(nearestMigros)
          Log.d(
              "DistanceService",
              "✓ Selected nearest Migros: '${nearestMigros.poiName}' - ${nearestMigros.walkingTimeMinutes} min")
        }

        if (nearestDenner != null) {
          allDistances.add(nearestDenner)
          Log.d(
              "DistanceService",
              "✓ Selected nearest Denner: '${nearestDenner.poiName}' - ${nearestDenner.walkingTimeMinutes} min")
        }

        // Get all markets and calculate walking times
        val markets = marketsRepository.getAllMarkets()
        val marketDistances =
            markets
                .map { market ->
                  async {
                    val timeMinutes =
                        walkingRouteService.calculateWalkingTimeMinutes(location, market.location)
                    if (timeMinutes != null) {
                      POIDistance(
                          poiName = market.name,
                          walkingTimeMinutes = timeMinutes,
                          poiType = POIType.MARKET)
                    } else null
                  }
                }
                .awaitAll()
                .filterNotNull()

        allDistances.addAll(marketDistances)

        Log.d(
            "DistanceService",
            "Calculated ${allDistances.size} total POI walking times for location (${location.latitude}, ${location.longitude})")
        Log.d(
            "DistanceService",
            "Breakdown: ${allDistances.count { it.poiType == POIType.UNIVERSITY }} universities, ${allDistances.count { it.poiType == POIType.SUPERMARKET }} supermarkets, ${allDistances.count { it.poiType == POIType.MARKET }} markets")
      }
    } catch (e: Exception) {
      Log.e("DistanceService", "Error calculating POI walking times", e)
    }

    // Sort by walking time (nearest first)
    val sortedDistances = allDistances.sortedBy { it.walkingTimeMinutes }

    // Always include nearest Migros and nearest Denner if they exist
    val nearestMigros =
        sortedDistances.firstOrNull {
          it.poiType == POIType.SUPERMARKET && it.poiName.contains("Migros", ignoreCase = true)
        }
    val nearestDenner =
        sortedDistances.firstOrNull {
          it.poiType == POIType.SUPERMARKET && it.poiName.contains("Denner", ignoreCase = true)
        }

    val result = mutableListOf<POIDistance>()

    // Add nearest Migros if it exists
    if (nearestMigros != null) {
      result.add(nearestMigros)
    }

    // Add nearest Denner if it exists
    if (nearestDenner != null) {
      result.add(nearestDenner)
    }

    // Add other POIs (universities, markets) that aren't already included
    val otherPOIs = sortedDistances.filter { it != nearestMigros && it != nearestDenner }
    val maxOtherPOIs = if (nearestMigros != null && nearestDenner != null) 3 else 2
    result.addAll(otherPOIs.take(maxOtherPOIs))

    // Sort final result by walking time
    val finalResult = result.sortedBy { it.walkingTimeMinutes }
    Log.d(
        "DistanceService",
        "Final POIs (${finalResult.size}): ${finalResult.map { "${it.poiName} (${it.poiType.name}) - ${it.walkingTimeMinutes} min" }.joinToString(", ")}")
    return finalResult
  }

  /**
   * Finds the nearest point of interest of a specific type.
   *
   * @param location The location to calculate walking time from
   * @param type The type of POI to find
   * @param userUniversityName Optional university name from user's profile
   * @return The nearest POIDistance of the specified type, or null if none found
   */
  suspend fun findNearestPOIByType(
      location: Location,
      type: POIType,
      userUniversityName: String? = null
  ): POIDistance? {
    // Handle invalid coordinates
    if (location.latitude == 0.0 && location.longitude == 0.0) {
      return null
    }

    val allDistances = calculateDistancesToPOIs(location, userUniversityName)
    return allDistances.filter { it.poiType == type }.minByOrNull { it.walkingTimeMinutes }
  }
}
