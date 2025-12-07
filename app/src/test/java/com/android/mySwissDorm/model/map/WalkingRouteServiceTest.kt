package com.android.mySwissDorm.model.map

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WalkingRouteServiceTest {
  private lateinit var server: MockWebServer
  private lateinit var client: OkHttpClient
  private lateinit var service: WalkingRouteService

  private val startLoc = Location("Home", 46.5200, 6.6300)

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()

    client =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
              val originalRequest = chain.request()
              val newUrl =
                  originalRequest.url
                      .newBuilder()
                      .scheme(server.url("/").scheme)
                      .host(server.url("/").host)
                      .port(server.url("/").port)
                      .build()
              chain.proceed(originalRequest.newBuilder().url(newUrl).build())
            }
            .build()

    service = WalkingRouteService(client, apiKey = "test-api-key")
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  private fun createPhotonResponse(name: String, osmId: Long, lat: Double, lon: Double): String {
    return """
        {
            "features": [
                {
                    "properties": { "name": "$name", "osm_id": $osmId },
                    "geometry": { "coordinates": [$lon, $lat] }
                }
            ]
        }
        """
        .trimIndent()
  }

  private fun createRouteResponse(distanceMeters: Double): String {
    return """
        {
            "features": [
                {
                    "properties": {
                        "summary": { "distance": $distanceMeters }
                    }
                }
            ]
        }
        """
        .trimIndent()
  }

  @Test
  fun searchNearbyShops_mergesResultsFromParallelSearches() = runTest {
    server.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path ?: ""
            return when {
              path.contains("q=Migros") ->
                  MockResponse().setBody(createPhotonResponse("Migros EPFL", 101, 46.5201, 6.6301))
              path.contains("q=Denner") ->
                  MockResponse().setBody(createPhotonResponse("Denner Sat", 102, 46.5202, 6.6302))
              path.contains("q=Coop") ->
                  MockResponse().setBody(createPhotonResponse("Coop Pronto", 103, 46.5203, 6.6303))
              path.contains("q=supermarket") ->
                  MockResponse().setBody(createPhotonResponse("Generic Shop", 104, 46.5204, 6.6304))
              else -> MockResponse().setResponseCode(404)
            }
          }
        }

    val results = service.searchNearbyShops(startLoc)
    assertEquals("Should find 4 unique shops", 4, results.size)
    assertTrue(results.any { it.name == "Migros EPFL" })
    assertTrue(results.any { it.name == "Denner Sat" })
    assertTrue(results.any { it.name == "Coop Pronto" })
  }

  @Test
  fun searchNearbyShops_deduplicatesByOsmId() = runTest {
    server.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            return MockResponse().setBody(createPhotonResponse("Same Shop", 100, 46.5201, 6.6301))
          }
        }
    val results = service.searchNearbyShops(startLoc)
    assertEquals("Should deduplicate to 1 shop", 1, results.size)
    assertEquals("Same Shop", results.first().name)
  }

  @Test
  fun searchNearbyShops_filtersOutShopsTooFarAway() = runTest {
    server.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            return MockResponse()
                .setBody(createPhotonResponse("Far Away Shop", 999, 46.6000, 6.6300))
          }
        }
    val results = service.searchNearbyShops(startLoc)
    assertTrue("Should filter out distant shops", results.isEmpty())
  }

  @Test
  fun searchNearbyShops_handlesApiErrorsGracefully() = runTest {
    server.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            return MockResponse().setResponseCode(500)
          }
        }
    val results = service.searchNearbyShops(startLoc)
    assertTrue("Should return empty list on error", results.isEmpty())
  }

  @Test
  fun calculateWalkingTime_successfulApiCall() = runTest {
    server.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            if (request.path?.contains("foot-walking") == true) {
              return MockResponse().setBody(createRouteResponse(1000.0))
            }
            return MockResponse().setResponseCode(404)
          }
        }
    val to = Location("To", 46.5210, 6.6310)
    val time = service.calculateWalkingTimeMinutes(startLoc, to)
    assertEquals(12, time)
  }

  @Test
  fun calculateWalkingTime_usesFallbackOnApiError() = runTest {
    server.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            return MockResponse().setResponseCode(500)
          }
        }
    val to = Location("To", 46.5210, 6.6310)
    val time = service.calculateWalkingTimeMinutes(startLoc, to)
    assertNotNull("Should return fallback time", time)
    assertTrue(time!! > 0)
  }

  @Test
  fun calculateWalkingTime_usesCache() = runTest {
    server.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            return MockResponse().setBody(createRouteResponse(1000.0))
          }
        }
    val to = Location("To", 46.5210, 6.6310)
    service.calculateWalkingTimeMinutes(startLoc, to)
    service.calculateWalkingTimeMinutes(startLoc, to)
    assertEquals("Should only hit API once due to caching", 1, server.requestCount)
  }
}
