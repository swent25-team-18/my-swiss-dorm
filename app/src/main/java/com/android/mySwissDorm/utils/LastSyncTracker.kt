package com.android.mySwissDorm.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.Timestamp

/**
 * Tracks the last successful data synchronization timestamp.
 *
 * This utility stores the timestamp of the last successful sync operation, which is used to display
 * "last updated X hours ago" in the offline banner.
 */
object LastSyncTracker {
  private const val PREFS_NAME = "last_sync_prefs"
  private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"

  /** Gets the SharedPreferences instance for storing sync timestamps. */
  private fun getSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  /**
   * Records the current timestamp as the last successful sync time.
   *
   * This should be called whenever data is successfully synced from the remote repository.
   *
   * @param context The application context.
   */
  fun recordSync(context: Context) {
    val prefs = getSharedPreferences(context)
    val currentTime = System.currentTimeMillis()
    prefs.edit { putLong(KEY_LAST_SYNC_TIMESTAMP, currentTime) }
  }

  /**
   * Gets the timestamp of the last successful sync, or null if no sync has been recorded.
   *
   * @param context The application context.
   * @return The last sync timestamp, or null if no sync has been recorded yet.
   */
  fun getLastSyncTimestamp(context: Context): Timestamp? {
    val prefs = getSharedPreferences(context)
    val timestampMillis = prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, -1L)
    return if (timestampMillis > 0) {
      Timestamp(timestampMillis / 1000, ((timestampMillis % 1000) * 1_000_000).toInt())
    } else {
      null
    }
  }

  /**
   * Clears the stored last sync timestamp.
   *
   * This can be useful for testing or when resetting the app state.
   *
   * @param context The application context.
   */
  fun clearLastSync(context: Context) {
    val prefs = getSharedPreferences(context)
    prefs.edit { remove(KEY_LAST_SYNC_TIMESTAMP) }
  }
}
