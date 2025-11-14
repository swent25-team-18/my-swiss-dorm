package com.android.mySwissDorm.model.map

import CachedLocations
import GeoCache
import InMemoryGeoCache
import android.util.Log
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class NominatimLocationRepository(
    private val client: OkHttpClient,
    private val cache: GeoCache = InMemoryGeoCache()
) : LocationRepository {
  private var lastCall = 0L
  private val MIN_INTERVAL_MS = 1100L
  private val gate = Mutex()

  private suspend fun respectRateLimit() =
      gate.withLock {
        val now = System.currentTimeMillis()
        val wait = (lastCall + MIN_INTERVAL_MS) - now
        if (wait > 0) delay(wait)
        lastCall = System.currentTimeMillis()
      }

  private fun normalizeKey(query: String, countryCodes: String, locale: Locale): String {
    val normQ = query.trim().lowercase(locale).replace(Regex("\\s+"), " ")
    return "q=$normQ|cc=$countryCodes|lang=${locale.toLanguageTag()}"
  }

  private fun parseBody(body: String): List<Location> {
    val jsonArray = JSONArray(body)

    return List(jsonArray.length()) { i ->
      val jsonObject = jsonArray.getJSONObject(i)
      val lat = jsonObject.getDouble("lat")
      val lon = jsonObject.getDouble("lon")
      val name = jsonObject.getString("display_name")
      Location(latitude = lat, name = name, longitude = lon)
    }
  }

  override suspend fun search(query: String): List<Location> =
      withContext(Dispatchers.IO) {
        val locale = Locale.getDefault()
        val countryCodes = "ch"
        val cacheKey = normalizeKey(query, countryCodes, locale)
        cache.get(cacheKey)?.let { cached ->
          val age = System.currentTimeMillis() - cached.fetchedTime
          if (age <= cache.timeToLive) return@withContext cached.locations
        }

        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .addPathSegment("search")
                .addQueryParameter("q", query)
                .addQueryParameter("format", "json")
                .addQueryParameter("countrycodes", countryCodes)
                .build()
        val request =
            Request.Builder()
                .url(url)
                .header("User-Agent", "MySwissDorm/1.0 (contact@myswissdorm.ch)")
                .header("Referer", "https://myswissdorm.com")
                .header("Accept-Language", locale.toLanguageTag())
                .build()

        try {
          respectRateLimit()
          val response = client.newCall(request).execute()

          response.use {
            if (response.code == 429) {
              val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 2L
              delay(retryAfter * 1000)
              throw IOException("Rate limited by Nominatim (429). Try later.")
            }
            if (!response.isSuccessful) {
              Log.d("NominatimLocationRepository", "Unexpected code $response")
              throw Exception("Unexpected code $response")
            }

            val body = response.body?.string() ?: return@withContext emptyList<Location>()
            val results = parseBody(body)
            cache.put(cacheKey, CachedLocations(results, System.currentTimeMillis()))
            return@withContext results
          }
        } catch (e: IOException) {
          Log.e("NominatimLocationRepository", "Failed to execute request", e)
          throw e
        }
      }

  override suspend fun reverseSearch(latitude: Double, longitude: Double): Location? =
      withContext(Dispatchers.IO) {
        val locale = Locale.getDefault()
        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .addPathSegment("reverse")
                .addQueryParameter("lat", latitude.toString())
                .addQueryParameter("lon", longitude.toString())
                .addQueryParameter("format", "json")
                .build()

        val request =
            Request.Builder()
                .url(url)
                .header("User-Agent", "MySwissDorm/1.0 (contact@myswissdorm.ch)")
                .header("Referer", "https://myswissdorm.com")
                .header("Accept-Language", locale.toLanguageTag())
                .build()

        try {
          respectRateLimit()
          val response = client.newCall(request).execute()

          response.use {
            if (response.code == 429) {
              val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 2L
              delay(retryAfter * 1000)
              throw IOException("Rate limited by Nominatim (429). Try later.")
            }
            if (!response.isSuccessful) {
              Log.d("NominatimLocationRepository", "Unexpected code $response")
              throw Exception("Unexpected code $response")
            }

            val body = response.body?.string() ?: return@withContext null
            val jsonObject = JSONObject(body)

            val lat = jsonObject.optDouble("lat", latitude)
            val lon = jsonObject.optDouble("lon", longitude)
            val name = jsonObject.optString("display_name", "Current Location")

            if (name.isEmpty()) return@withContext null

            return@withContext Location(latitude = lat, name = name, longitude = lon)
          }
        } catch (e: Exception) {
          Log.e("NominatimLocationRepository", "Failed to execute reverse request", e)
          throw e
        }
      }
}
