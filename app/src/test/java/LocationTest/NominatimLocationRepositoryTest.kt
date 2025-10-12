package com.github.se.bootcamp.model.map

import com.android.mySwissDorm.model.map.Location
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
import java.io.IOException

/**
 * Tests for NominatimLocationRepository without modifying production code.
 * We rewrite the target host/scheme to MockWebServer via a test-only interceptor.
 */
class NominatimLocationRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var repo: NominatimLocationRepository

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }

        client = OkHttpClient.Builder()
            .addInterceptor(TestUrlRewriteInterceptor(server.url("/")))
            .build()
        repo = NominatimLocationRepository(client)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun search_ok_parsesResults() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                [
                  {"display_name":"Lausanne, Vaud","lat":"46.5197","lon":"6.6323"},
                  {"display_name":"Genève, Canton de Genève","lat":"46.2044","lon":"6.1432"}
                ]
                """.trimIndent()
            )
        )

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

    /**
     * Test-only interceptor that preserves path & query but swaps scheme/host/port to MockWebServer.
     * This avoids adding a baseUrlHost seam in production code.
     */
    private class TestUrlRewriteInterceptor(
        private val serverUrl: HttpUrl
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val newUrl = original.url.newBuilder()
                .scheme(serverUrl.scheme)
                .host(serverUrl.host)
                .port(serverUrl.port)
                .build()
            val newReq = original.newBuilder().url(newUrl).build()
            return chain.proceed(newReq)
        }
    }
}
