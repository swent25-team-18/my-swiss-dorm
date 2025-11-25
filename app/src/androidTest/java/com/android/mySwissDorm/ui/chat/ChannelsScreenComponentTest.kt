package com.android.mySwissDorm.ui.chat

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChannelsScreenComponentTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  /**
   * Test formatMessageTime function directly Tests all time format cases: "Just now", "Xm ago", "Xh
   * ago", "Yesterday", "Xd ago", "MMM dd"
   */
  @Test
  fun formatMessageTime_justNow() {
    val now = Date()
    val result = formatMessageTime(now, context)
    assertTrue(
        "Should return 'Just now' for current time", result == "Just now" || result.contains("ago"))
  }

  @Test
  fun formatMessageTime_minutesAgo_direct() {
    val minutesAgo = Date(System.currentTimeMillis() - 5 * 60 * 1000) // 5 minutes ago
    val result = formatMessageTime(minutesAgo, context)
    assertTrue("Should contain 'm ago' for minutes", result.contains("m ago"))
    assertTrue("Should show approximately 5 minutes", result.contains("5") || result.contains("4"))
  }

  @Test
  fun formatMessageTime_hoursAgo_direct() {
    val hoursAgo = Date(System.currentTimeMillis() - 3 * 60 * 60 * 1000) // 3 hours ago
    val result = formatMessageTime(hoursAgo, context)
    assertTrue("Should contain 'h ago' for hours", result.contains("h ago"))
    assertTrue("Should show approximately 3 hours", result.contains("3") || result.contains("2"))
  }

  @Test
  fun formatMessageTime_yesterday_direct() {
    val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000) // 1 day ago
    val result = formatMessageTime(yesterday, context)
    assertEquals("Should return 'Yesterday' for 1 day ago", "Yesterday", result)
  }

  @Test
  fun formatMessageTime_daysAgo_direct() {
    val daysAgo = Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000) // 3 days ago
    val result = formatMessageTime(daysAgo, context)
    assertTrue("Should contain 'd ago' for days", result.contains("d ago"))
    assertTrue("Should show approximately 3 days", result.contains("3") || result.contains("2"))
  }

  @Test
  fun formatMessageTime_oldDate_direct() {
    val oldDate = Date(System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000) // 10 days ago
    val result = formatMessageTime(oldDate, context)
    // Should be formatted as "MMM dd" (e.g., "Jan 15")
    assertTrue("Should be formatted date string", result.length >= 5)
    assertTrue("Should not contain 'ago' for old dates", !result.contains("ago"))
  }
}
