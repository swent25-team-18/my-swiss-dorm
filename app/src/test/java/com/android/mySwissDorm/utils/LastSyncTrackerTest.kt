package com.android.mySwissDorm.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LastSyncTrackerTest {
  private lateinit var context: Context
  private lateinit var sharedPreferences: SharedPreferences

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    sharedPreferences = context.getSharedPreferences("last_sync_prefs", Context.MODE_PRIVATE)
    // Clear any existing data
    sharedPreferences.edit { clear() }
  }

  @Test
  fun recordSync_storesCurrentTimestamp() {
    val beforeTime = System.currentTimeMillis()
    LastSyncTracker.recordSync(context)
    val afterTime = System.currentTimeMillis()

    val storedTimestamp = LastSyncTracker.getLastSyncTimestamp(context)
    assertNotNull("Timestamp should be stored", storedTimestamp)

    val storedMillis = storedTimestamp!!.seconds * 1000L + storedTimestamp.nanoseconds / 1_000_000
    assertTrue("Timestamp should be after beforeTime", storedMillis >= beforeTime)
    assertTrue("Timestamp should be before afterTime", storedMillis <= afterTime)
  }

  @Test
  fun getLastSyncTimestamp_returnsNullWhenNoSyncRecorded() {
    val timestamp = LastSyncTracker.getLastSyncTimestamp(context)
    assertNull("Should return null when no sync recorded", timestamp)
  }

  @Test
  fun getLastSyncTimestamp_returnsStoredTimestamp() {
    val expectedMillis = System.currentTimeMillis()
    sharedPreferences.edit { putLong("last_sync_timestamp", expectedMillis) }

    val timestamp = LastSyncTracker.getLastSyncTimestamp(context)
    assertNotNull("Should return stored timestamp", timestamp)

    val actualMillis = timestamp!!.seconds * 1000L + timestamp.nanoseconds / 1_000_000
    // Allow small difference due to conversion
    assertTrue(
        "Timestamp should match stored value",
        kotlin.math.abs(actualMillis - expectedMillis) < 1000)
  }

  @Test
  fun clearLastSync_removesStoredTimestamp() {
    // First record a sync
    LastSyncTracker.recordSync(context)
    assertNotNull(
        "Should have timestamp after recording", LastSyncTracker.getLastSyncTimestamp(context))

    // Then clear it
    LastSyncTracker.clearLastSync(context)

    val timestamp = LastSyncTracker.getLastSyncTimestamp(context)
    assertNull("Should return null after clearing", timestamp)
  }

  @Test
  fun clearLastSync_whenNoTimestampExists_doesNotThrow() {
    // Should not throw when clearing non-existent timestamp
    LastSyncTracker.clearLastSync(context)
    val timestamp = LastSyncTracker.getLastSyncTimestamp(context)
    assertNull("Should still return null", timestamp)
  }

  @Test
  fun recordSync_overwritesPreviousTimestamp() {
    val firstTime = System.currentTimeMillis()
    LastSyncTracker.recordSync(context)
    Thread.sleep(100) // Ensure different timestamp
    val secondTime = System.currentTimeMillis()
    LastSyncTracker.recordSync(context)

    val timestamp = LastSyncTracker.getLastSyncTimestamp(context)
    assertNotNull("Should have timestamp", timestamp)

    val storedMillis = timestamp!!.seconds * 1000L + timestamp.nanoseconds / 1_000_000
    assertTrue("Should have second timestamp", storedMillis >= secondTime)
    assertTrue("Should not have first timestamp", storedMillis > firstTime + 50)
  }

  @Test
  fun getLastSyncTimestamp_handlesInvalidStoredValue() {
    // Store an invalid value (negative)
    sharedPreferences.edit { putLong("last_sync_timestamp", -1L) }

    val timestamp = LastSyncTracker.getLastSyncTimestamp(context)
    assertNull("Should return null for invalid value", timestamp)
  }

  @Test
  fun getLastSyncTimestamp_handlesZeroValue() {
    // Store zero (also invalid)
    sharedPreferences.edit { putLong("last_sync_timestamp", 0L) }

    val timestamp = LastSyncTracker.getLastSyncTimestamp(context)
    assertNull("Should return null for zero value", timestamp)
  }
}
