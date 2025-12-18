package com.android.mySwissDorm.locationTest

import com.android.mySwissDorm.model.map.CachedLocations
import com.android.mySwissDorm.model.map.GeoCache
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.NominatimLocationRepository
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

/**
 * Tests for NominatimLocationRepository without modifying production code. We rewrite the target
 * host/scheme to MockWebServer via a test-only interceptor.
 */
class NominatimLocationRepositoryTest {
  /** Test-only cache that lets us set TTL easily. */
  private lateinit var server: MockWebServer
  private lateinit var client: OkHttpClient
  private lateinit var repo: NominatimLocationRepository

  @Before
  fun setUp() {
    server = MockWebServer().apply { start() }

    client =
        OkHttpClient.Builder().addInterceptor(TestUrlRewriteInterceptor(server.url("/"))).build()
    repo = NominatimLocationRepository(client)
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  class TestGeoCache(override val timeToLive: Long) : GeoCache {
    private val map = mutableMapOf<String, CachedLocations>()

    override suspend fun get(key: String) = map[key]

    override suspend fun put(key: String, value: CachedLocations) {
      map[key] = value
    }
  }

  @Test
  fun cache_hit_returns_data_without_network() = runTest {
    val mockClient = mock(OkHttpClient::class.java)
    val cache = TestGeoCache(timeToLive = 86_400_000L)
    val repo = NominatimLocationRepository(mockClient, cache)
    val query = "Lausanne"
    val normQ = query.trim().lowercase(Locale.getDefault()).replace(Regex("\\s+"), " ")
    val key = "q=$normQ|cc=ch|lang=${Locale.getDefault().toLanguageTag()}"

    val cached =
        listOf(Location(latitude = 46.519653, name = "Lausanne, Switzerland", longitude = 6.632273))
    cache.put(key, CachedLocations(cached, System.currentTimeMillis()))
    val result = repo.search(query)
    assertEquals(cached, result)
    verify(mockClient, never()).newCall(any())
  }

  @Test(expected = IOException::class)
  fun http_429_retry_after_throws_ioexception() = runTest {
    server.enqueue(
        MockResponse()
            .setResponseCode(429)
            .setHeader("Retry-After", "0")
            .setBody("""{"error":"rate limited"}"""))
    try {
      repo.search("lausanne")
    } finally {
      assertEquals(1, server.requestCount)
    }
  }

  @Test
  fun search_ok_parsesResults() = runTest {
    server.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                [
                  {"display_name":"Lausanne, Vaud","lat":"46.5197","lon":"6.6323"},
                  {"display_name":"Genève, Canton de Genève","lat":"46.2044","lon":"6.1432"}
                ]
                """
                    .trimIndent()))

    val res: List<Location> = repo.search("lausanne")

    assertEquals(2, res.size)
    assertEquals("Lausanne, Vaud", res[0].name)
    assertEquals(46.5197, res[0].latitude, 1e-6)
    assertEquals(6.6323, res[0].longitude, 1e-6)

    val recorded = server.takeRequest()
    assertTrue(recorded.path!!.startsWith("/search"))
    val url = recorded.requestUrl!!
    assertEquals("json", url.queryParameter("format"))
    assertEquals("ch", url.queryParameter("countrycodes"))
    assertEquals("lausanne", url.queryParameter("q"))
    assertEquals("MySwissDorm/1.0 (contact@myswissdorm.ch)", recorded.getHeader("User-Agent"))
    assertEquals("https://myswissdorm.com", recorded.getHeader("Referer"))
  }

  @Test
  fun search_emptyArray_returnsEmptyList() = runTest {
    server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

    val res = repo.search("no-such-place")
    assertTrue(res.isEmpty())

    server.takeRequest()
  }

  @Test(expected = Exception::class)
  fun search_http503_throwsException() = runTest {
    server.enqueue(MockResponse().setResponseCode(503).setBody("""{"error":"unavailable"}"""))
    repo.search("lausanne")
  }

  @Test(expected = Exception::class)
  fun search_malformedJson_throwsException() = runTest {
    server.enqueue(MockResponse().setResponseCode(200).setBody("""{ not-an-array """))
    repo.search("lausanne")
  }

  @Test(expected = IOException::class)
  fun search_networkFailure_throwsIOException() = runTest {
    // Shut down server to force a connect failure
    server.shutdown()
    repo.search("lausanne")
  }

  // Added these tests for the reverse geocode(search) mechanism
  @Test
  fun reverseSearch_ok_parsesResult() = runTest {
    server.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                    "place_id": 12345,
                    "lat": "46.5197",
                    "lon": "6.6323",
                    "display_name": "Lausanne, District de Lausanne, Vaud, Switzerland"
                }
                """
                    .trimIndent()))

    val lat = 46.5197
    val lon = 6.6323
    val result = repo.reverseSearch(lat, lon)

    assertNotNull(result)
    assertEquals("Lausanne, District de Lausanne, Vaud, Switzerland", result!!.name)
    assertEquals(lat, result.latitude, 1e-6)
    assertEquals(lon, result.longitude, 1e-6)

    val recorded = server.takeRequest()
    assertTrue(recorded.path!!.startsWith("/reverse"))
    val url = recorded.requestUrl!!
    assertEquals(lat.toString(), url.queryParameter("lat"))
    assertEquals(lon.toString(), url.queryParameter("lon"))
    assertEquals("json", url.queryParameter("format"))
  }

  @Test(expected = IOException::class)
  fun reverseSearch_http429_throwsIOException() = runTest {
    server.enqueue(
        MockResponse()
            .setResponseCode(429)
            .setHeader("Retry-After", "0")
            .setBody("""{"error":"rate limited"}"""))
    try {
      repo.reverseSearch(46.5197, 6.6323)
    } finally {
      assertEquals(1, server.requestCount)
    }
  }

  @Test
  fun reverseSearch_emptyName_returnsNull() = runTest {
    server.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                    "place_id": 12345,
                    "lat": "46.5197",
                    "lon": "6.6323",
                    "display_name": ""
                }
                """
                    .trimIndent()))

    val result = repo.reverseSearch(46.5197, 6.6323)
    assertNull(result)
    server.takeRequest()
  }

  @Test(expected = Exception::class)
  fun reverseSearch_http503_throwsException() = runTest {
    server.enqueue(MockResponse().setResponseCode(503).setBody("""{"error":"unavailable"}"""))
    repo.reverseSearch(46.5197, 6.6323)
  }

  @Test(expected = Exception::class)
  fun reverseSearch_malformedJson_throwsException() = runTest {
    server.enqueue(MockResponse().setResponseCode(200).setBody("""{ not-a-valid-object """))
    repo.reverseSearch(46.5197, 6.6323)
  }
  /**
   * Test-only interceptor that preserves path & query but swaps scheme/host/port to MockWebServer.
   * This avoids adding a baseUrlHost seam in production code.
   */
  private class TestUrlRewriteInterceptor(private val serverUrl: HttpUrl) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val original = chain.request()
      val newUrl =
          original.url
              .newBuilder()
              .scheme(serverUrl.scheme)
              .host(serverUrl.host)
              .port(serverUrl.port)
              .build()
      val newReq = original.newBuilder().url(newUrl).build()
      return chain.proceed(newReq)
    }
  }
}
