package com.github.se.bootcamp.model.map

import android.util.Log
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class NominatimLocationRepository(private val client: OkHttpClient) : LocationRepository {

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
            val url =
                HttpUrl.Builder()
                    .scheme("https")
                    .host("nominatim.openstreetmap.org")
                    .addPathSegment("search")
                    .addQueryParameter("q", query)
                    .addQueryParameter("format", "json")
                    .addQueryParameter("countrycodes", "ch")
                    .build()
            val request =
                Request.Builder()
                    .url(url)
                    .header(
                        "User-Agent",
                        "MySwissDorm/1.0 (contact@myswissdorm.ch)"
                    )
                    .header("Referer", "https://myswissdorm.com")
                    .build()

            try {
                val response = client.newCall(request).execute()
                response.use {
                    if (!response.isSuccessful) {
                        Log.d("NominatimLocationRepository", "Unexpected code $response")
                        throw Exception("Unexpected code $response")
                    }

                    val body = response.body?.string()
                    if (body != null) {
                        Log.d("NominatimLocationRepository", "Body: $body")
                        return@withContext parseBody(body)
                    } else {
                        Log.d("NominatimLocationRepository", "Empty body")
                        return@withContext emptyList()
                    }
                }
            } catch (e: IOException) {
                Log.e("NominatimLocationRepository", "Failed to execute request", e)
                throw e
            }
        }
}
