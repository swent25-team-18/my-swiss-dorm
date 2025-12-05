package com.android.mySwissDorm.model.map

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Service for calculating walking routes and times between two locations. Uses OpenRouteService
 * API. Requires an API key configured in BuildConfig.OPENROUTESERVICE_API_KEY or falls back to
 * Haversine calculation.
 */
class WalkingRouteService(
    private val client: OkHttpClient,
    private val apiKey: String =
        com.android.mySwissDorm.BuildConfig.OPENROUTESERVICE_API_KEY?.takeIf { it.isNotBlank() }
            ?: ""
) {
  private val cache =
      mutableMapOf<
          String, Pair<Double, Long>>() // Cache: routeKey -> (distance in meters, timestamp)
  private val CACHE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
  private val gate = Mutex()
  private var lastCall = 0L
  private val MIN_INTERVAL_MS = 1000L // Rate limiting: 1 request per second

  companion object {
    private const val TAG = "WalkingRouteService"
    private const val WALKING_SPEED_KMH = 5.0 // Average walking speed: 5 km/h
    private const val WALKING_SPEED_MS = WALKING_SPEED_KMH / 3.6 // Convert to m/s
  }

  /**
   * Calculates walking time in minutes between two locations. Uses OpenRouteService to get the
   * actual walking route distance.
   *
   * @param from Starting location
   * @param to Destination location
   * @return Walking time in minutes, or null if calculation fails
   */
  suspend fun calculateWalkingTimeMinutes(from: Location, to: Location): Int? =
      withContext(Dispatchers.IO) {
        try {
          // Check cache first
          val cacheKey = "${from.latitude},${from.longitude}_${to.latitude},${to.longitude}"
          cache[cacheKey]?.let { (cachedDistance, timestamp) ->
            val age = System.currentTimeMillis() - timestamp
            if (age < CACHE_DURATION_MS) {
              val timeMinutes = (cachedDistance / WALKING_SPEED_MS / 60).toInt()
              Log.d(TAG, "Using cached route: ${cachedDistance.toInt()}m = $timeMinutes min")
              return@withContext timeMinutes
            }
          }

          // Rate limiting
          gate.withLock {
            val now = System.currentTimeMillis()
            val wait = (lastCall + MIN_INTERVAL_MS) - now
            if (wait > 0) {
              kotlinx.coroutines.delay(wait)
            }
            lastCall = System.currentTimeMillis()
          }

          // Call OpenRouteService API
          // Note: OpenRouteService requires an API key. If not provided, fallback to Haversine
          // calculation.
          if (apiKey.isBlank()) {
            Log.w(TAG, "OpenRouteService API key not configured, using fallback calculation")
            return@withContext calculateFallbackTime(from, to)
          }

          val url =
              HttpUrl.Builder()
                  .scheme("https")
                  .host("api.openrouteservice.org")
                  .addPathSegment("v2")
                  .addPathSegment("directions")
                  .addPathSegment("foot-walking")
                  .addQueryParameter("api_key", apiKey)
                  .addQueryParameter(
                      "start", "${from.longitude},${from.latitude}") // Note: lon,lat order for ORS
                  .addQueryParameter("end", "${to.longitude},${to.latitude}")
                  .build()

          val request = Request.Builder().url(url).header("User-Agent", "MySwissDorm/1.0").build()

          val response = client.newCall(request).execute()

          response.use {
            if (!response.isSuccessful) {
              Log.w(TAG, "OpenRouteService API error: ${response.code}")
              // Fallback to Haversine distance with 1.3x multiplier (typical route factor)
              return@withContext calculateFallbackTime(from, to)
            }

            val body = response.body?.string() ?: return@withContext calculateFallbackTime(from, to)
            val json = JSONObject(body)
            val features = json.getJSONArray("features")
            if (features.length() == 0) {
              return@withContext calculateFallbackTime(from, to)
            }

            val feature = features.getJSONObject(0)
            val properties = feature.getJSONObject("properties")
            val summary = properties.getJSONObject("summary")
            val distanceMeters = summary.getDouble("distance")

            // Cache the result
            cache[cacheKey] = Pair(distanceMeters, System.currentTimeMillis())

            // Calculate walking time: distance (meters) / speed (m/s) / 60 (seconds to minutes)
            val timeMinutes = (distanceMeters / WALKING_SPEED_MS / 60).toInt()
            Log.d(
                TAG,
                "Route calculated: ${distanceMeters.toInt()}m = $timeMinutes min (${from.name} -> ${to.name})")
            return@withContext timeMinutes
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error calculating walking route", e)
          // Fallback to Haversine distance
          return@withContext calculateFallbackTime(from, to)
        }
      }

  /**
   * Fallback calculation using Haversine distance with a route factor. Real walking routes are
   * typically 1.2-1.5x longer than straight-line distance.
   */
  private fun calculateFallbackTime(from: Location, to: Location): Int? {
    try {
      val straightDistanceKm = from.distanceTo(to)
      val routeDistanceKm = straightDistanceKm * 1.3 // Apply route factor
      val timeMinutes = (routeDistanceKm / WALKING_SPEED_KMH * 60).toInt()
      Log.d(
          TAG,
          "Using fallback calculation: ${straightDistanceKm}km straight -> ${routeDistanceKm}km route = $timeMinutes min")
      return maxOf(1, timeMinutes) // At least 1 minute
    } catch (e: Exception) {
      Log.e(TAG, "Error in fallback calculation", e)
      return null
    }
  }
}
