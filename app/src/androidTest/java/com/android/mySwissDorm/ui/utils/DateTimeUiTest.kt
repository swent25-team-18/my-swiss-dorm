package com.android.mySwissDorm.ui.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.Timestamp
import org.junit.Assert
import org.junit.Test

class DateTimeUiTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun formatDate_null_returnsDash() {
    Assert.assertEquals("—", DateTimeUi.formatDate(null))
  }

  @Test
  fun formatDate_has_ddMMyyyy_shape() {
    // 2025-09-15 00:00:00 UTC
    val ts = Timestamp(1_756_208_000, 0)
    val s = DateTimeUi.formatDate(ts)
    Assert.assertTrue(Regex("""\d{2}/\d{2}/\d{4}""").matches(s))
  }

  @Test
  fun formatRelative_seconds() {
    val now = System.currentTimeMillis()
    val ts = Timestamp((now - 12_000) / 1000, 0) // 12s ago
    Assert.assertTrue(DateTimeUi.formatRelative(ts, now, context).endsWith("s ago"))
  }

  @Test
  fun formatRelative_minutes() {
    val now = System.currentTimeMillis()
    val ts = Timestamp((now - 5 * 60_000) / 1000, 0) // 5 min ago
    Assert.assertTrue(DateTimeUi.formatRelative(ts, now, context).contains("min ago"))
  }

  @Test
  fun formatRelative_hours() {
    val now = System.currentTimeMillis()
    val ts = Timestamp((now - 2 * 3_600_000L) / 1000, 0) // 2h ago
    Assert.assertTrue(DateTimeUi.formatRelative(ts, now, context).endsWith("h ago"))
  }

  @Test
  fun formatRelative_days() {
    val now = System.currentTimeMillis()
    val ts = Timestamp((now - 3 * 24 * 3_600_000L) / 1000, 0) // 3d ago
    Assert.assertTrue(DateTimeUi.formatRelative(ts, now, context).endsWith("d ago"))
  }

  @Test
  fun formatRelative_onDate_when_7days_or_more() {
    val now = System.currentTimeMillis()
    val ts = Timestamp((now - 8 * 24 * 3_600_000L) / 1000, 0) // 8d ago
    Assert.assertTrue(DateTimeUi.formatRelative(ts, now, context).startsWith("on "))
  }

  @Test
  fun formatRelative_futureIsClampedToZeroSeconds() {
    val now = System.currentTimeMillis()
    val ts = Timestamp((now + 60_000) / 1000, 0) // in the future
    Assert.assertEquals("0s ago", DateTimeUi.formatRelative(ts, now, context))
  }
}
