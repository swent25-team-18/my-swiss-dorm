package com.android.mySwissDorm.ui.utils

import android.content.Context
import com.android.mySwissDorm.R
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val SWITZERLAND_TIMEZONE = TimeZone.getTimeZone("Europe/Zurich")

/** UI-friendly date/time helpers for listing screens. */
object DateTimeUi {

  /** Format a Timestamp as DD/MM/yyyy in Switzerland timezone, or "—" if null. */
  fun formatDate(ts: Timestamp?): String {
    return ts?.toDate()?.let { date ->
      val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
      formatter.timeZone = SWITZERLAND_TIMEZONE
      formatter.format(date)
    } ?: "—"
  }

  /**
   * Return a compact relative string like "45s ago", "12 min ago", "3h ago", "2d ago", or "on
   * 15/09/2025" for 7+ days. Null -> "—".
   *
   * @param nowMillis allows deterministic tests.
   */
  fun formatRelative(
      ts: Timestamp?,
      nowMillis: Long = System.currentTimeMillis(),
      context: Context
  ): String {
    if (ts == null) return "—"

    val then = ts.toDate().time
    var diff = nowMillis - then
    if (diff < 0) diff = 0L // future-safe

    val seconds = diff / 1000
    return when {
      seconds < 60 -> context.getString(R.string.date_time_ui_x_seconds_ago, seconds)
      seconds < 60 * 60 ->
          context.getString(R.string.date_time_ui_x_minutes_ago, seconds / 60) // < 1 hour → minutes
      seconds < 24 * 60 * 60 ->
          context.getString(R.string.date_time_ui_x_hours_ago, seconds / 3600) // < 24h → hours
      seconds < 7 * 24 * 60 * 60 ->
          context.getString(R.string.date_time_ui_x_days_ago, seconds / 86400) // < 7d → days
      else -> context.getString(R.string.date_time_ui_on_date, formatDate(ts)) // ≥ 7d → date
    }
  }
}
