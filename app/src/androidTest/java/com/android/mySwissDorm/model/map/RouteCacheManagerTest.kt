package com.android.mySwissDorm.model.map

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Tests for RouteCacheManager. Tests persistent caching using SharedPreferences. */
class RouteCacheManagerTest {
  private lateinit var context: Context
  private lateinit var cacheManager: RouteCacheManager
  private lateinit var prefs: SharedPreferences

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    cacheManager = RouteCacheManager(context)
    prefs = context.getSharedPreferences("route_cache_prefs", Context.MODE_PRIVATE)
    // Clear cache before each test
    prefs.edit { clear() }
  }

  @After
  fun tearDown() {
    // Clean up after each test
    prefs.edit { clear() }
  }

  @Test
  fun get_cacheMiss_returnsNull() = runTest {
    val result = cacheManager.get("nonexistent_key")
    assertNull("Should return null for non-existent key", result)
  }

  @Test
  fun putAndGet_cacheHit_returnsCachedDistance() = runTest {
    val cacheKey = "46.5200,6.6300_46.5210,6.6310"
    val distance = 1000.0

    cacheManager.put(cacheKey, distance)
    val result = cacheManager.get(cacheKey)

    assertNotNull("Should return cached distance", result)
    assertEquals("Should return correct distance", distance, result!!, 0.01)
  }

  @Test
  fun get_expiredCache_returnsNull() = runTest {
    val cacheKey = "46.5200,6.6300_46.5210,6.6310"
    val distance = 1000.0

    // Put with old timestamp (31 days ago)
    val oldTimestamp = System.currentTimeMillis() - (31L * 24 * 60 * 60 * 1000)
    val json =
        org.json.JSONObject().apply {
          put("distance", distance)
          put("timestamp", oldTimestamp)
        }
    prefs.edit { putString(cacheKey, json.toString()) }

    val result = cacheManager.get(cacheKey)

    assertNull("Should return null for expired cache", result)
    // Verify expired entry was removed
    assertFalse("Expired entry should be removed", prefs.contains(cacheKey))
  }

  @Test
  fun get_validCache_returnsDistance() = runTest {
    val cacheKey = "46.5200,6.6300_46.5210,6.6310"
    val distance = 1000.0

    // Put with recent timestamp (1 day ago)
    val recentTimestamp = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000)
    val json =
        org.json.JSONObject().apply {
          put("distance", distance)
          put("timestamp", recentTimestamp)
        }
    prefs.edit { putString(cacheKey, json.toString()) }

    val result = cacheManager.get(cacheKey)

    assertNotNull("Should return cached distance", result)
    assertEquals("Should return correct distance", distance, result!!, 0.01)
  }

  @Test
  fun put_multipleEntries_storesAll() = runTest {
    val key1 = "46.5200,6.6300_46.5210,6.6310"
    val key2 = "46.5300,6.6400_46.5310,6.6410"
    val distance1 = 1000.0
    val distance2 = 2000.0

    cacheManager.put(key1, distance1)
    cacheManager.put(key2, distance2)

    val result1 = cacheManager.get(key1)
    val result2 = cacheManager.get(key2)

    assertNotNull("Should return first cached distance", result1)
    assertEquals("Should return correct first distance", distance1, result1!!, 0.01)
    assertNotNull("Should return second cached distance", result2)
    assertEquals("Should return correct second distance", distance2, result2!!, 0.01)
  }

  @Test
  fun clear_removesAllEntries() = runTest {
    val key1 = "46.5200,6.6300_46.5210,6.6310"
    val key2 = "46.5300,6.6400_46.5310,6.6410"

    cacheManager.put(key1, 1000.0)
    cacheManager.put(key2, 2000.0)

    cacheManager.clear()

    assertNull("First entry should be removed", cacheManager.get(key1))
    assertNull("Second entry should be removed", cacheManager.get(key2))
  }

  @Test
  fun cleanupExpired_removesOnlyExpiredEntries() = runTest {
    val expiredKey = "expired_key"
    val validKey = "valid_key"

    // Add expired entry (31 days ago)
    val expiredTimestamp = System.currentTimeMillis() - (31L * 24 * 60 * 60 * 1000)
    val expiredJson =
        org.json.JSONObject().apply {
          put("distance", 1000.0)
          put("timestamp", expiredTimestamp)
        }
    prefs.edit { putString(expiredKey, expiredJson.toString()) }

    // Add valid entry (1 day ago)
    val validTimestamp = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000)
    val validJson =
        org.json.JSONObject().apply {
          put("distance", 2000.0)
          put("timestamp", validTimestamp)
        }
    prefs.edit { putString(validKey, validJson.toString()) }

    cacheManager.cleanupExpired()

    assertFalse("Expired entry should be removed", prefs.contains(expiredKey))
    assertTrue("Valid entry should remain", prefs.contains(validKey))
    assertNotNull("Valid entry should still be retrievable", cacheManager.get(validKey))
  }

  @Test
  fun get_invalidJson_returnsNull() = runTest {
    val cacheKey = "invalid_key"
    // Put invalid JSON
    prefs.edit { putString(cacheKey, "not valid json") }

    val result = cacheManager.get(cacheKey)

    assertNull("Should return null for invalid JSON", result)
  }

  @Test
  fun get_missingTimestamp_returnsNull() = runTest {
    val cacheKey = "missing_timestamp_key"
    // Put JSON without timestamp
    val json = org.json.JSONObject().apply { put("distance", 1000.0) }
    prefs.edit { putString(cacheKey, json.toString()) }

    val result = cacheManager.get(cacheKey)

    assertNull("Should return null for missing timestamp", result)
  }
}
