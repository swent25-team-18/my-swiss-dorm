package com.android.mySwissDorm.model.map

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Manages persistent caching of route distances using SharedPreferences. Cache entries are stored
 * as JSON with distance and timestamp.
 */
class RouteCacheManager(private val context: Context) {
  private val prefs: SharedPreferences by lazy {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  companion object {
    private const val TAG = "RouteCacheManager"
    private const val PREFS_NAME = "route_cache_prefs"
    private const val CACHE_DURATION_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
  }

  /**
   * Gets a cached route distance if it exists and is not expired.
   *
   * @param cacheKey The cache key (format: "lat1,lng1_lat2,lng2")
   * @return The cached distance in meters, or null if not found or expired
   */
  suspend fun get(cacheKey: String): Double? =
      withContext(Dispatchers.IO) {
        try {
          val cachedJson = prefs.getString(cacheKey, null) ?: return@withContext null
          val json = JSONObject(cachedJson)
          val distance = json.getDouble("distance")
          val timestamp = json.getLong("timestamp")

          val age = System.currentTimeMillis() - timestamp
          if (age >= CACHE_DURATION_MS) {
            // Cache expired, remove it
            prefs.edit { remove(cacheKey) }
            return@withContext null
          }

          return@withContext distance
        } catch (e: Exception) {
          Log.e(TAG, "Error reading cache for key: $cacheKey", e)
          return@withContext null
        }
      }

  /**
   * Stores a route distance in the cache.
   *
   * @param cacheKey The cache key (format: "lat1,lng1_lat2,lng2")
   * @param distance The distance in meters
   */
  suspend fun put(cacheKey: String, distance: Double) =
      withContext(Dispatchers.IO) {
        try {
          val json =
              JSONObject().apply {
                put("distance", distance)
                put("timestamp", System.currentTimeMillis())
              }
          prefs.edit { putString(cacheKey, json.toString()) }
        } catch (e: Exception) {
          Log.e(TAG, "Error writing cache for key: $cacheKey", e)
        }
      }

  /** Clears all cached routes. Useful for testing or when cache needs to be reset. */
  suspend fun clear() =
      withContext(Dispatchers.IO) {
        prefs.edit { clear() }
        Log.d(TAG, "Route cache cleared")
      }

  /** Cleans up expired cache entries. Should be called periodically. */
  suspend fun cleanupExpired() =
      withContext(Dispatchers.IO) {
        try {
          val allEntries = prefs.all
          val now = System.currentTimeMillis()
          var removedCount = 0

          prefs.edit {
            allEntries.forEach { (key, value) ->
              if (value is String) {
                try {
                  val json = JSONObject(value)
                  val timestamp = json.getLong("timestamp")
                  val age = now - timestamp
                  if (age >= CACHE_DURATION_MS) {
                    remove(key)
                    removedCount++
                  }
                } catch (e: Exception) {
                  // Invalid entry, remove it
                  remove(key)
                  removedCount++
                }
              }
            }
          }

          if (removedCount > 0) {
            Log.d(TAG, "Cleaned up $removedCount expired cache entries")
          } else {
            // No expired entries to clean up
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error cleaning up cache", e)
        }
      }
}
