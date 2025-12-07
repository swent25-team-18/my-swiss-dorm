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

data class POIDistance(val poiName: String, val walkingTimeMinutes: Int, val poiType: String) {
  companion object {
    const val TYPE_UNIVERSITY = "university"
    const val TYPE_SUPERMARKET = "supermarket"
  }
}

class DistanceService(
    private val universitiesRepository: UniversitiesRepository,
    private val walkingRouteService: WalkingRouteService
) {

  companion object {
    private const val MAX_WALKING_TIME_MINUTES = 30
    private const val SEARCH_RADIUS_KM = 5.0
    private const val SEARCH_RADIUS_METERS = 2000
    private const val TAG = "DistanceService"
  }

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
        val allNearbyShops = walkingRouteService.searchNearbyShops(location, SEARCH_RADIUS_METERS)
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
          addSupermarketIfNotNull(allDistances, nearestMigros, "Migros")
          addSupermarketIfNotNull(allDistances, nearestDenner, "Denner")
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error calculating POI walking times", e)
    }

    return buildFinalResult(allDistances)
  }

  private fun isValidLocation(location: Location): Boolean {
    return !(location.latitude == 0.0 && location.longitude == 0.0)
  }

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
    val candidatePool = if (good.isNotEmpty()) good else generic
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

  private fun addSupermarketIfNotNull(
      list: MutableList<POIDistance>,
      item: POIDistance?,
      label: String
  ) {
    if (item != null) list.add(item)
  }

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

  suspend fun findNearestPOIByType(
      location: Location,
      type: String,
      userUniversityName: String? = null
  ): POIDistance? {
    val allDistances = calculateDistancesToPOIs(location, userUniversityName)
    return allDistances.filter { it.poiType == type }.minByOrNull { it.walkingTimeMinutes }
  }
}
