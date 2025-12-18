package com.android.mySwissDorm.model.poi

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.WalkingRouteService
import com.android.mySwissDorm.model.map.distanceTo
import com.android.mySwissDorm.model.supermarket.Supermarket
import com.android.mySwissDorm.model.university.UniversitiesRepository
import com.android.mySwissDorm.model.university.University
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Represents a simplified point of interest with its walking distance.
 *
 * @property poiName The display name of the POI (e.g., "EPFL", "Migros").
 * @property walkingTimeMinutes The estimated walking time in minutes.
 * @property poiType The category of the POI (e.g., [TYPE_UNIVERSITY], [TYPE_SUPERMARKET]).
 */
data class POIDistance(val poiName: String, val walkingTimeMinutes: Int, val poiType: String) {
  companion object {
    const val TYPE_UNIVERSITY = "university"
    const val TYPE_SUPERMARKET = "supermarket"
  }
}
/**
 * Service responsible for calculating walking distances to key Points of Interest (POIs) relevant
 * to student life, specifically Universities and Supermarkets.
 *
 * This service orchestrates data fetching from [UniversitiesRepository] and [WalkingRouteService],
 * filters results based on proximity and relevance, and prioritizes specific Swiss supermarket
 * brands.
 */
class DistanceService(
    private val universitiesRepository: UniversitiesRepository,
    private val walkingRouteService: WalkingRouteService
) {

  companion object {
    private const val MAX_WALKING_TIME_MINUTES = 30
    private const val SEARCH_RADIUS_KM = 5.0
    private const val TAG = "DistanceService"
  }
  /**
   * Calculates walking times to nearby universities and supermarkets.
   *
   * **Logic Flow:**
   * 1. **Universities:** Finds the nearest universities. If [userUniversityName] is provided, it
   *    prioritizes that university.
   * 2. **Supermarkets:** Fetches all nearby shops and attempts to find the nearest **Migros** and
   *    **Denner** explicitly.
   * 3. **Fallback:** If neither Migros nor Denner is found, it searches for other major brands
   *    (Coop, Aldi, etc.) or falls back to the best generic supermarket.
   * 4. **Aggregation:** Combines results, limits the list size, and sorts by walking time.
   *
   * @param location The starting location for distance calculations.
   * @param userUniversityName Optional name of the user's university to prioritize in the results.
   * @return A list of [POIDistance] objects sorted by walking time. Returns an empty list if the
   *   location is invalid.
   */
  suspend fun calculateDistancesToPOIs(
      location: Location,
      userUniversityName: String? = null
  ): List<POIDistance> {
    if (!isValidLocation(location)) return emptyList()

    val allDistances = mutableListOf<POIDistance>()

    try {
      coroutineScope {
        val universityDistances = calculateUniversityDistances(location, userUniversityName)
        allDistances.addAll(universityDistances)
        val allNearbyShops = walkingRouteService.searchNearbyShops(location)
        val nearestMigros = findNearestSupermarketFromList(location, allNearbyShops, "Migros")
        val nearestDenner = findNearestSupermarketFromList(location, allNearbyShops, "Denner")
        if (nearestMigros == null && nearestDenner == null) {
          val otherBrands = listOf("Coop", "Aldi", "Lidl", "Volg", "Spar")
          var fallbackFound: POIDistance? = null

          for (brand in otherBrands) {
            fallbackFound = findNearestSupermarketFromList(location, allNearbyShops, brand)
            if (fallbackFound != null) break
          }
          if (fallbackFound == null) {
            val bestGeneric = findBestGenericSupermarket(location, allNearbyShops)
            if (bestGeneric != null) fallbackFound = bestGeneric
          }

          if (fallbackFound != null) {
            allDistances.add(fallbackFound)
          }
        } else {
          addSupermarketIfNotNull(allDistances, nearestMigros)
          addSupermarketIfNotNull(allDistances, nearestDenner)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error calculating POI walking times", e)
    }

    return buildFinalResult(allDistances)
  }
  /** Checks if the location coordinates are non-zero. */
  private fun isValidLocation(location: Location): Boolean {
    return !(location.latitude == 0.0 && location.longitude == 0.0)
  }
  /**
   * Selects relevant universities and calculates walking times to them in parallel.
   *
   * @return A list of [POIDistance] for universities within [MAX_WALKING_TIME_MINUTES].
   */
  private suspend fun calculateUniversityDistances(
      location: Location,
      userUniversityName: String?
  ): List<POIDistance> = coroutineScope {
    val universities = universitiesRepository.getAllUniversities()
    val universityPOIs = selectUniversitiesToShow(universities, location, userUniversityName)

    universityPOIs
        .map { university ->
          async {
            val time =
                walkingRouteService.calculateWalkingTimeMinutes(location, university.location)
            if (time != null && time <= MAX_WALKING_TIME_MINUTES) {
              POIDistance(university.name, time, POIDistance.TYPE_UNIVERSITY)
            } else null
          }
        }
        .awaitAll()
        .filterNotNull()
  }
  /**
   * Filters the list of universities to determine which ones to display.
   *
   * If [userUniversityName] is provided and matches a nearby university, only that one is returned.
   * Otherwise, returns the 2 closest universities within [SEARCH_RADIUS_KM].
   */
  private fun selectUniversitiesToShow(
      universities: List<University>,
      location: Location,
      userUniversityName: String?
  ): List<University> {
    val nearbyUniversities =
        universities.filter { location.distanceTo(it.location) <= SEARCH_RADIUS_KM }
    if (userUniversityName != null && userUniversityName.isNotBlank()) {
      val userUniversity =
          nearbyUniversities.firstOrNull { it.name.equals(userUniversityName, ignoreCase = true) }
      if (userUniversity != null) return listOf(userUniversity)
    }
    return nearbyUniversities.sortedBy { location.distanceTo(it.location) }.take(2)
  }
  /**
   * Finds the nearest supermarket matching a specific brand name from a pre-fetched list.
   *
   * This method:
   * 1. Filters the list by brand name (handling the Migros/Denner distinction).
   * 2. Takes the top 3 closest candidates by linear distance.
   * 3. Calculates actual walking times for these candidates in parallel.
   * 4. Returns the one with the shortest walking time.
   *
   * @param allShops The full list of fetched supermarkets.
   * @param brandName The brand to search for (e.g., "Migros"). If empty, searches all.
   */
  private suspend fun findNearestSupermarketFromList(
      location: Location,
      allShops: List<Supermarket>,
      brandName: String
  ): POIDistance? = coroutineScope {
    val candidates =
        if (brandName.isNotBlank()) {
          allShops.filter {
            it.name.contains(brandName, ignoreCase = true) &&
                !it.name.contains(
                    if (brandName == "Migros") "Denner" else "Migros", ignoreCase = true)
          }
        } else {
          allShops
        }

    if (candidates.isEmpty()) return@coroutineScope null

    val closestCandidates = candidates.sortedBy { location.distanceTo(it.location) }.take(3)

    val supermarketTimes =
        closestCandidates
            .map { supermarket ->
              async {
                val timeMinutes =
                    walkingRouteService.calculateWalkingTimeMinutes(location, supermarket.location)
                if (timeMinutes != null && timeMinutes <= MAX_WALKING_TIME_MINUTES) {
                  Pair(supermarket, timeMinutes)
                } else null
              }
            }
            .awaitAll()
            .filterNotNull()

    supermarketTimes
        .minByOrNull { it.second }
        ?.let { (supermarket, timeMinutes) ->
          POIDistance(supermarket.name, timeMinutes, POIDistance.TYPE_SUPERMARKET)
        }
  }

  /**
   * Finds the best non-branded supermarket. Penalizes names like "Supermarket" or "Shop" to avoid
   * generic labels if a better name exists.
   */
  private suspend fun findBestGenericSupermarket(
      location: Location,
      allShops: List<Supermarket>
  ): POIDistance? = coroutineScope {
    if (allShops.isEmpty()) return@coroutineScope null
    val genericNames = setOf("Supermarket", "Supermarkt", "Shop", "Convenience Shop", "Kiosk")
    val (generic, good) = allShops.partition { genericNames.contains(it.name) || it.name.isBlank() }
    val candidatePool = good.ifEmpty { generic }
    val closestCandidates = candidatePool.sortedBy { location.distanceTo(it.location) }.take(3)

    val supermarketTimes =
        closestCandidates
            .map { supermarket ->
              async {
                val timeMinutes =
                    walkingRouteService.calculateWalkingTimeMinutes(location, supermarket.location)
                if (timeMinutes != null && timeMinutes <= MAX_WALKING_TIME_MINUTES) {
                  Pair(supermarket, timeMinutes)
                } else null
              }
            }
            .awaitAll()
            .filterNotNull()

    supermarketTimes
        .minByOrNull { it.second }
        ?.let { (supermarket, timeMinutes) ->
          POIDistance(supermarket.name, timeMinutes, POIDistance.TYPE_SUPERMARKET)
        }
  }

  private fun addSupermarketIfNotNull(list: MutableList<POIDistance>, item: POIDistance?) {
    if (item != null) list.add(item)
  }
  /**
   * Constructs the final list of POIs to be displayed.
   *
   * Ensures that:
   * 1. The nearest Migros and Denner are included if available.
   * 2. The total list size does not exceed the limit (default 3 or 4 depending on mix).
   * 3. The list is sorted by walking time.
   */
  private fun buildFinalResult(allDistances: List<POIDistance>): List<POIDistance> {
    val sortedDistances = allDistances.sortedBy { it.walkingTimeMinutes }

    val nearestMigros =
        sortedDistances.firstOrNull {
          it.poiType == POIDistance.TYPE_SUPERMARKET &&
              it.poiName.contains("Migros", ignoreCase = true)
        }
    val nearestDenner =
        sortedDistances.firstOrNull {
          it.poiType == POIDistance.TYPE_SUPERMARKET &&
              it.poiName.contains("Denner", ignoreCase = true)
        }

    val result = mutableListOf<POIDistance>()
    if (nearestMigros != null) result.add(nearestMigros)
    if (nearestDenner != null) result.add(nearestDenner)

    val otherPOIs = sortedDistances.filter { it != nearestMigros && it != nearestDenner }
    val maxOtherPOIs = if (nearestMigros != null && nearestDenner != null) 3 else 2
    result.addAll(otherPOIs.take(maxOtherPOIs))

    return result.sortedBy { it.walkingTimeMinutes }
  }
  /**
   * Helper method to find the single nearest POI of a specific type.
   *
   * @param type The POI type to filter by (e.g., [POIDistance.TYPE_UNIVERSITY]).
   * @return The nearest [POIDistance] of that type, or null if none found.
   */
  suspend fun findNearestPOIByType(
      location: Location,
      type: String,
      userUniversityName: String? = null
  ): POIDistance? {
    val allDistances = calculateDistancesToPOIs(location, userUniversityName)
    return allDistances.filter { it.poiType == type }.minByOrNull { it.walkingTimeMinutes }
  }
}
