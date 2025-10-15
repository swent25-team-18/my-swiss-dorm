package com.android.mySwissDorm.ui.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

/** UI-friendly date/time helpers for listing screens. */
object DateTimeUi {

  /** Format a Timestamp as dd/MM/yyyy, or "—" if null. */
  fun formatDate(ts: Timestamp?): String {
    return ts?.toDate()?.let { date ->
      SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    } ?: "—"
  }

  /**
   * Return a compact relative string like "45s ago", "12 min ago", "3h ago", "2d ago", or "on
   * 15/09/2025" for 7+ days. Null -> "—".
   *
   * @param nowMillis allows deterministic tests.
   */
  fun formatRelative(ts: Timestamp?, nowMillis: Long = System.currentTimeMillis()): String {
    if (ts == null) return "—"

    val then = ts.toDate().time
    var diff = nowMillis - then
    if (diff < 0) diff = 0L // future-safe

    val seconds = diff / 1000
    return when {
      seconds < 60 -> "${seconds}s ago"
      seconds < 60 * 60 -> "${seconds / 60} min ago" // < 1 hour → minutes
      seconds < 24 * 60 * 60 -> "${seconds / 3600}h ago" // < 24h → hours
      seconds < 7 * 24 * 60 * 60 -> "${seconds / 86400}d ago" // < 7d → days
      else -> "on ${formatDate(ts)}" // ≥ 7d → date
    }
  }
}
