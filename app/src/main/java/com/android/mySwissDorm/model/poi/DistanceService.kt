package com.android.mySwissDorm.model.poi

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.WalkingRouteService
import com.android.mySwissDorm.model.map.distanceTo
import com.android.mySwissDorm.model.supermarket.SupermarketsRepository
import com.android.mySwissDorm.model.university.UniversitiesRepository
import com.android.mySwissDorm.model.university.University
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
 * Service for calculating walking times to points of interest. Uses data from Firestore collections
 * (universities, supermarkets). Calculates actual walking route times using OpenRouteService API.
 */
class DistanceService(
    private val universitiesRepository: UniversitiesRepository,
    private val supermarketsRepository: SupermarketsRepository,
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
    if (!isValidLocation(location)) {
      return emptyList()
    }

    val allDistances = mutableListOf<POIDistance>()

    try {
      coroutineScope {
        val universityDistances = calculateUniversityDistances(location, userUniversityName)
        allDistances.addAll(universityDistances)

        val nearestMigros = findNearestSupermarket(location, "Migros")
        val nearestDenner = findNearestSupermarket(location, "Denner")

        addSupermarketIfNotNull(allDistances, nearestMigros, "Migros")
        addSupermarketIfNotNull(allDistances, nearestDenner, "Denner")

        logCalculationSummary(allDistances, location)
      }
    } catch (e: Exception) {
      Log.e("DistanceService", "Error calculating POI walking times", e)
    }

    return buildFinalResult(allDistances)
  }

  private fun isValidLocation(location: Location): Boolean {
    if (location.latitude == 0.0 && location.longitude == 0.0) {
      Log.d("DistanceService", "Invalid coordinates (0.0, 0.0), returning empty list")
      return false
    }
    return true
  }

  private suspend fun calculateUniversityDistances(
      location: Location,
      userUniversityName: String?
  ): List<POIDistance> = coroutineScope {
    val universities = universitiesRepository.getAllUniversities()
    val universityPOIs = selectUniversitiesToShow(universities, location, userUniversityName)

    val universityDistances =
        universityPOIs
            .map { university ->
              async {
                val timeMinutes =
                    walkingRouteService.calculateWalkingTimeMinutes(location, university.location)
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

    Log.d("DistanceService", "Calculated ${universityDistances.size} university walking times")
    universityDistances
  }

  private fun selectUniversitiesToShow(
      universities: List<University>,
      location: Location,
      userUniversityName: String?
  ): List<University> {
    if (userUniversityName != null && userUniversityName.isNotBlank()) {
      val userUniversity =
          universities.firstOrNull { it.name.equals(userUniversityName, ignoreCase = true) }
      if (userUniversity != null) {
        return listOf(userUniversity)
      } else {
        Log.w(
            "DistanceService",
            "User's university '$userUniversityName' not found in database, showing 2 nearest instead")
        return universities.sortedBy { location.distanceTo(it.location) }.take(2)
      }
    }
    return universities.sortedBy { location.distanceTo(it.location) }.take(2)
  }

  private suspend fun findNearestSupermarket(location: Location, brandName: String): POIDistance? =
      coroutineScope {
        val supermarkets = supermarketsRepository.getAllSupermarkets()
        val brandSupermarkets =
            supermarkets.filter {
              it.name.contains(brandName, ignoreCase = true) &&
                  !it.name.contains(
                      if (brandName == "Migros") "Denner" else "Migros", ignoreCase = true)
            }

        if (brandSupermarkets.isEmpty()) {
          return@coroutineScope null
        }

        val supermarketTimes =
            brandSupermarkets
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

        supermarketTimes
            .minByOrNull { it.second }
            ?.let { (supermarket, timeMinutes) ->
              POIDistance(
                  poiName = supermarket.name,
                  walkingTimeMinutes = timeMinutes,
                  poiType = POIType.SUPERMARKET)
            }
      }

  private fun addSupermarketIfNotNull(
      allDistances: MutableList<POIDistance>,
      supermarket: POIDistance?,
      brandName: String
  ) {
    if (supermarket != null) {
      allDistances.add(supermarket)
      Log.d(
          "DistanceService",
          "✓ Selected nearest $brandName: '${supermarket.poiName}' - ${supermarket.walkingTimeMinutes} min")
    }
  }

  private fun logCalculationSummary(allDistances: List<POIDistance>, location: Location) {
    Log.d(
        "DistanceService",
        "Calculated ${allDistances.size} total POI walking times for location (${location.latitude}, ${location.longitude})")
    Log.d(
        "DistanceService",
        "Breakdown: ${allDistances.count { it.poiType == POIType.UNIVERSITY }} universities, ${allDistances.count { it.poiType == POIType.SUPERMARKET }} supermarkets")
  }

  private fun buildFinalResult(allDistances: List<POIDistance>): List<POIDistance> {
    val sortedDistances = allDistances.sortedBy { it.walkingTimeMinutes }

    val nearestMigros =
        sortedDistances.firstOrNull {
          it.poiType == POIType.SUPERMARKET && it.poiName.contains("Migros", ignoreCase = true)
        }
    val nearestDenner =
        sortedDistances.firstOrNull {
          it.poiType == POIType.SUPERMARKET && it.poiName.contains("Denner", ignoreCase = true)
        }

    val result = mutableListOf<POIDistance>()

    if (nearestMigros != null) {
      result.add(nearestMigros)
    }

    if (nearestDenner != null) {
      result.add(nearestDenner)
    }

    val otherPOIs = sortedDistances.filter { it != nearestMigros && it != nearestDenner }
    val maxOtherPOIs = if (nearestMigros != null && nearestDenner != null) 3 else 2
    result.addAll(otherPOIs.take(maxOtherPOIs))

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
