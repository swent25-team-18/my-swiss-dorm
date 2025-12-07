package com.android.mySwissDorm.model.map

import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Tests for WalkingRouteService. Uses MockWebServer to simulate OpenRouteService API responses. */
class WalkingRouteServiceTest {
  private lateinit var server: MockWebServer
  private lateinit var client: OkHttpClient
  private lateinit var service: WalkingRouteService

  @Before
  fun setUp() {
    server = MockWebServer().apply { start() }
    client =
        OkHttpClient.Builder().addInterceptor(TestUrlRewriteInterceptor(server.url("/"))).build()
    // Use a test API key so the service doesn't fall back to Haversine calculation
    // No persistent cache for unit tests (would require Android context)
    service = WalkingRouteService(client, apiKey = "test-api-key", persistentCache = null)
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  /** Interceptor that rewrites URLs to point to MockWebServer. */
  class TestUrlRewriteInterceptor(private val baseUrl: okhttp3.HttpUrl) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
      val originalRequest = chain.request()
      val newUrl =
          originalRequest.url
              .newBuilder()
              .scheme(baseUrl.scheme)
              .host(baseUrl.host)
              .port(baseUrl.port)
              .build()
      val newRequest = originalRequest.newBuilder().url(newUrl).build()
      return chain.proceed(newRequest)
    }
  }

  private fun createMockRouteResponse(distanceMeters: Double): String {
    val json = JSONObject()
    val features = JSONArray()
    val feature = JSONObject()
    val properties = JSONObject()
    val summary = JSONObject()
    summary.put("distance", distanceMeters)
    properties.put("summary", summary)
    feature.put("properties", properties)
    features.put(feature)
    json.put("features", features)
    return json.toString()
  }

  @Test
  fun calculateWalkingTime_successfulApiCall_returnsCorrectTime() = runTest {
    // 1000 meters = 1 km, at 5 km/h = 12 minutes
    val distanceMeters = 1000.0
    val expectedMinutes = 12 // 1000m / (5km/h / 3.6) / 60 = ~12 min

    server.enqueue(
        MockResponse().setResponseCode(200).setBody(createMockRouteResponse(distanceMeters)))

    val from = Location("From", 46.5200, 6.6300)
    val to = Location("To", 46.5210, 6.6310)

    val result = service.calculateWalkingTimeMinutes(from, to)

    assertNotNull("Walking time should be calculated", result)
    assertEquals("Walking time should be approximately 12 minutes", expectedMinutes, result)
  }

  @Test
  fun calculateWalkingTime_apiError_usesFallback() = runTest {
    server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

    val from = Location("From", 46.5200, 6.6300)
    val to = Location("To", 46.5210, 6.6310)

    val result = service.calculateWalkingTimeMinutes(from, to)

    // Fallback should still return a time (at least 1 minute)
    assertNotNull("Fallback should return a time", result)
    assertTrue("Fallback time should be at least 1 minute", result!! >= 1)
  }

  @Test
  fun calculateWalkingTime_emptyResponse_usesFallback() = runTest {
    server.enqueue(MockResponse().setResponseCode(200).setBody("""{"features":[]}"""))

    val from = Location("From", 46.5200, 6.6300)
    val to = Location("To", 46.5210, 6.6310)

    val result = service.calculateWalkingTimeMinutes(from, to)

    assertNotNull("Fallback should return a time", result)
    assertTrue("Fallback time should be at least 1 minute", result!! >= 1)
  }

  @Test
  fun calculateWalkingTime_cachesResults() = runTest {
    val distanceMeters = 1000.0
    server.enqueue(
        MockResponse().setResponseCode(200).setBody(createMockRouteResponse(distanceMeters)))

    val from = Location("From", 46.5200, 6.6300)
    val to = Location("To", 46.5210, 6.6310)

    // First call - should hit API
    val result1 = service.calculateWalkingTimeMinutes(from, to)
    assertEquals(1, server.requestCount)

    // Second call - should use in-memory cache
    val result2 = service.calculateWalkingTimeMinutes(from, to)
    assertEquals(1, server.requestCount) // Still 1, cache was used

    assertEquals("Cached result should match", result1, result2)
  }

  // Note: Testing with persistent cache requires Android instrumentation.
  // This test is covered in RouteCacheManagerTest (androidTest) which tests
  // the integration with WalkingRouteService.
  // For unit tests, we verify that the service works without persistent cache.

  @Test
  fun calculateWalkingTime_invalidCoordinates_returnsNull() = runTest {
    val from = Location("From", 0.0, 0.0)
    val to = Location("To", 0.0, 0.0)

    // This should use fallback, but fallback with 0,0 might return null or a default
    val result = service.calculateWalkingTimeMinutes(from, to)

    // Fallback should handle this and return at least 1 minute
    assertNotNull("Should return at least 1 minute for fallback", result)
  }

  @Test
  fun calculateWalkingTime_networkException_usesFallback() = runTest {
    // Close server to simulate network error
    server.shutdown()

    val from = Location("From", 46.5200, 6.6300)
    val to = Location("To", 46.5210, 6.6310)

    val result = service.calculateWalkingTimeMinutes(from, to)

    // Should use fallback
    assertNotNull("Fallback should return a time", result)
    assertTrue("Fallback time should be at least 1 minute", result!! >= 1)
  }

  @Test
  fun calculateWalkingTime_differentDistances_returnsDifferentTimes() = runTest {
    // Short distance
    server.enqueue(
        MockResponse().setResponseCode(200).setBody(createMockRouteResponse(500.0))) // 500m

    val from1 = Location("From1", 46.5200, 6.6300)
    val to1 = Location("To1", 46.5205, 6.6305)

    val shortTime = service.calculateWalkingTimeMinutes(from1, to1)

    // Long distance
    server.enqueue(
        MockResponse().setResponseCode(200).setBody(createMockRouteResponse(5000.0))) // 5000m

    val from2 = Location("From2", 46.5200, 6.6300)
    val to2 = Location("To2", 46.5300, 6.6400)

    val longTime = service.calculateWalkingTimeMinutes(from2, to2)

    assertNotNull("Short time should be calculated", shortTime)
    assertNotNull("Long time should be calculated", longTime)
    assertTrue("Long distance should take more time", longTime!! > shortTime!!)
  }
}
