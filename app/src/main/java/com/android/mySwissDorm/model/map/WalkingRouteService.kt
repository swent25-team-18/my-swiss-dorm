package com.android.mySwissDorm.model.map

import android.util.Log
import com.android.mySwissDorm.model.supermarket.Supermarket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Service for calculating walking routes (OpenRouteService) and searching for POIs (Photon API).
 */
class WalkingRouteService(
    private val client: OkHttpClient,
    private val apiKey: String =
        com.android.mySwissDorm.BuildConfig.OPENROUTESERVICE_API_KEY.takeIf { it.isNotBlank() }
            ?: ""
) {
  private val cache = mutableMapOf<String, Pair<Double, Long>>()
  private val CACHE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
  private val gate = Mutex()
  private var lastCall = 0L
  private val MIN_INTERVAL_MS = 200L

  companion object {
    private const val TAG = "WalkingRouteService"
    private const val WALKING_SPEED_KMH = 5.0
    private const val WALKING_SPEED_MS = WALKING_SPEED_KMH / 3.6
  }

  /**
   * Asynchronously searches for supermarkets and major grocery chains near a given location using
   * the Photon API.
   *
   * To ensure comprehensive coverage, this function performs **parallel searches** for specific
   * high-priority Swiss brands ("Migros", "Denner", "Coop") alongside a generic "supermarket"
   * query. This strategy helps find locations that may be indexed solely by brand name or solely by
   * category.
   *
   * **Key Implementation Details:**
   * - **Concurrency:** Utilizes [async] to launch simultaneous network requests for all search
   *   terms, reducing total wait time.
   * - **Deduplication:** Results are aggregated into a map keyed by the shop's unique ID (`uid`) to
   *   ensure the final list contains no duplicates.
   * - **Context:** Execution is confined to [Dispatchers.IO], making it safe to call from the main
   *   thread.
   *
   * @param location The [Location] object representing the center point of the search area.
   * @return A [List] of unique [Supermarket] objects. Returns an empty list if no shops are found
   *   or if the API calls fail.
   */
  suspend fun searchNearbyShops(location: Location): List<Supermarket> =
      withContext(Dispatchers.IO) {
        val uniqueShops = mutableMapOf<String, Supermarket>()
        val searchTerms = listOf("Migros", "Denner", "Coop", "supermarket")
        Log.d(TAG, "Starting parallel search for: $searchTerms")
        try {
          coroutineScope {
            val deferredResults =
                searchTerms.map { term -> async { fetchFromPhoton(term, location) } }
            val allResults = deferredResults.awaitAll()
            allResults.flatten().forEach { shop -> uniqueShops[shop.uid] = shop }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error during parallel search", e)
        }

        Log.d(TAG, "Combined search found ${uniqueShops.size} unique shops.")
        return@withContext uniqueShops.values.toList()
      }

  /** Helper function to fetch a single search term from Photon */
  private suspend fun fetchFromPhoton(query: String, location: Location): List<Supermarket> {
    val found = mutableListOf<Supermarket>()
    try {
      gate.withLock {
        val now = System.currentTimeMillis()
        val wait = (lastCall + 50L) - now
        if (wait > 0) kotlinx.coroutines.delay(wait)
        lastCall = System.currentTimeMillis()
      }
      val url =
          "https://photon.komoot.io/api/?q=$query&lat=${location.latitude}&lon=${location.longitude}&limit=50"
      val request = Request.Builder().url(url).build()
      val response = client.newCall(request).execute()
      response.use {
        if (!response.isSuccessful) return emptyList()
        val body = response.body?.string() ?: return emptyList()
        val json = JSONObject(body)
        val features = json.optJSONArray("features") ?: return emptyList()

        for (i in 0 until features.length()) {
          val feature = features.getJSONObject(i)
          val props = feature.optJSONObject("properties") ?: continue
          val geometry = feature.optJSONObject("geometry") ?: continue
          val coords = geometry.optJSONArray("coordinates") ?: continue

          val lon = coords.optDouble(0, 0.0)
          val lat = coords.optDouble(1, 0.0)
          val name = props.optString("name")
          if (name.isBlank()) continue
          if (lat != 0.0 && lon != 0.0) {
            val osmId = props.optLong("osm_id", 0).toString()
            val shopLocation = Location(name, lat, lon)
            if (location.distanceTo(shopLocation) <= 5.0) {
              found.add(Supermarket(osmId, name, shopLocation))
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to fetch '$query'", e)
    }
    return found
  }

  /** Calculates walking time using OpenRouteService Routing API. */
  suspend fun calculateWalkingTimeMinutes(from: Location, to: Location): Int? =
      withContext(Dispatchers.IO) {
        try {
          val cacheKey = "${from.latitude},${from.longitude}_${to.latitude},${to.longitude}"
          cache[cacheKey]?.let { (cachedDistance, timestamp) ->
            if (System.currentTimeMillis() - timestamp < CACHE_DURATION_MS) {
              return@withContext (cachedDistance / WALKING_SPEED_MS / 60).toInt()
            }
          }

          gate.withLock {
            val now = System.currentTimeMillis()
            val wait = (lastCall + MIN_INTERVAL_MS) - now
            if (wait > 0) kotlinx.coroutines.delay(wait)
            lastCall = System.currentTimeMillis()
          }

          if (apiKey.isBlank()) return@withContext calculateFallbackTime(from, to)

          val url =
              "https://api.openrouteservice.org/v2/directions/foot-walking?api_key=$apiKey&start=${from.longitude},${from.latitude}&end=${to.longitude},${to.latitude}"
          val request = Request.Builder().url(url).build()
          val response = client.newCall(request).execute()

          response.use {
            if (!response.isSuccessful) return@withContext calculateFallbackTime(from, to)
            val body = response.body?.string() ?: return@withContext calculateFallbackTime(from, to)
            val json = JSONObject(body)

            val features = json.optJSONArray("features")
            if (features == null || features.length() == 0)
                return@withContext calculateFallbackTime(from, to)

            val distanceMeters =
                features
                    .getJSONObject(0)
                    .getJSONObject("properties")
                    .getJSONObject("summary")
                    .getDouble("distance")

            cache[cacheKey] = Pair(distanceMeters, System.currentTimeMillis())
            return@withContext (distanceMeters / WALKING_SPEED_MS / 60).toInt()
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error calculating route", e)
          return@withContext calculateFallbackTime(from, to)
        }
      }

  private fun calculateFallbackTime(from: Location, to: Location): Int? {
    try {
      val distKm = from.distanceTo(to) * 1.3
      return maxOf(1, (distKm / WALKING_SPEED_KMH * 60).toInt())
    } catch (e: Exception) {
      return null
    }
  }
}
