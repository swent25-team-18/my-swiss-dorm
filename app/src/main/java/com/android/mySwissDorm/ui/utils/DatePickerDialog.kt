package com.android.mySwissDorm.ui.utils

import android.content.res.Configuration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val SWITZERLAND_TIMEZONE = TimeZone.getTimeZone("Europe/Zurich")
private val SWITZERLAND_LOCALE = Locale("fr", "CH")

/** Gets today's date at midnight in Switzerland timezone as UTC milliseconds. */
private fun getTodayMidnightUtc(): Long {
  val todaySwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
  todaySwiss.set(Calendar.HOUR_OF_DAY, 0)
  todaySwiss.set(Calendar.MINUTE, 0)
  todaySwiss.set(Calendar.SECOND, 0)
  todaySwiss.set(Calendar.MILLISECOND, 0)
  return todaySwiss.timeInMillis
}

/** Converts a Timestamp to midnight UTC milliseconds in Switzerland timezone. */
private fun timestampToMidnightUtc(timestamp: Timestamp): Long {
  val swissCal = Calendar.getInstance(SWITZERLAND_TIMEZONE)
  swissCal.timeInMillis = timestamp.toDate().time
  val year = swissCal.get(Calendar.YEAR)
  val month = swissCal.get(Calendar.MONTH)
  val day = swissCal.get(Calendar.DAY_OF_MONTH)

  val midnightSwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
  midnightSwiss.set(year, month, day, 0, 0, 0)
  midnightSwiss.set(Calendar.MILLISECOND, 0)
  return midnightSwiss.timeInMillis
}

/**
 * Converts UTC milliseconds to date components and creates a system timezone calendar at noon (to
 * avoid DST issues) for DatePickerState.
 */
private fun utcMillisToSystemNoon(utcMillis: Long): Long {
  val swissCal = Calendar.getInstance(SWITZERLAND_TIMEZONE)
  swissCal.timeInMillis = utcMillis
  val year = swissCal.get(Calendar.YEAR)
  val month = swissCal.get(Calendar.MONTH)
  val day = swissCal.get(Calendar.DAY_OF_MONTH)

  val systemCal = Calendar.getInstance()
  systemCal.set(year, month, day, 12, 0, 0)
  systemCal.set(Calendar.MILLISECOND, 0)
  return systemCal.timeInMillis
}

/** Converts system timezone milliseconds to a Timestamp at midnight in Switzerland timezone. */
private fun systemMillisToTimestamp(millis: Long): Timestamp {
  val systemCal = Calendar.getInstance()
  systemCal.timeInMillis = millis
  val year = systemCal.get(Calendar.YEAR)
  val month = systemCal.get(Calendar.MONTH)
  val day = systemCal.get(Calendar.DAY_OF_MONTH)

  val swissCal = Calendar.getInstance(SWITZERLAND_TIMEZONE)
  swissCal.set(year, month, day, 0, 0, 0)
  swissCal.set(Calendar.MILLISECOND, 0)
  return Timestamp(swissCal.time)
}

/** Normalizes UTC milliseconds to midnight UTC in Switzerland timezone. */
private fun normalizeToMidnightUtc(utcTimeMillis: Long): Long {
  val swissCal = Calendar.getInstance(SWITZERLAND_TIMEZONE)
  swissCal.timeInMillis = utcTimeMillis
  val year = swissCal.get(Calendar.YEAR)
  val month = swissCal.get(Calendar.MONTH)
  val day = swissCal.get(Calendar.DAY_OF_MONTH)

  val midnightSwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
  midnightSwiss.set(year, month, day, 0, 0, 0)
  midnightSwiss.set(Calendar.MILLISECOND, 0)
  return midnightSwiss.timeInMillis
}

/**
 * [SelectableDates] implementation that prevents selecting past dates. Only dates from today
 * onwards (in Switzerland timezone) are selectable.
 */
@OptIn(ExperimentalMaterial3Api::class)
private class FutureSelectableDates : SelectableDates {
  override fun isSelectableDate(utcTimeMillis: Long): Boolean {
    val todayMidnightUtc = getTodayMidnightUtc()
    val selectedMidnightUtc = normalizeToMidnightUtc(utcTimeMillis)
    return selectedMidnightUtc >= todayMidnightUtc
  }

  override fun isSelectableYear(year: Int): Boolean {
    val currentYear = Calendar.getInstance(SWITZERLAND_TIMEZONE).get(Calendar.YEAR)
    return year >= currentYear
  }
}

/**
 * [SelectableDates] implementation that respects min and max date constraints. Dates must be from
 * today onwards and within the specified min/max range.
 */
@OptIn(ExperimentalMaterial3Api::class)
private class ConstrainedSelectableDates(
    private val minDate: Timestamp?,
    private val maxDate: Timestamp?
) : SelectableDates {
  override fun isSelectableDate(utcTimeMillis: Long): Boolean {
    val todayMidnightUtc = getTodayMidnightUtc()
    val selectedMidnightUtc = normalizeToMidnightUtc(utcTimeMillis)

    // Must be from today onwards
    if (selectedMidnightUtc < todayMidnightUtc) return false

    // Check min date constraint
    minDate?.let { if (selectedMidnightUtc < timestampToMidnightUtc(it)) return false }

    // Check max date constraint
    maxDate?.let { if (selectedMidnightUtc > timestampToMidnightUtc(it)) return false }

    return true
  }

  override fun isSelectableYear(year: Int): Boolean {
    val currentYear = Calendar.getInstance(SWITZERLAND_TIMEZONE).get(Calendar.YEAR)
    if (year < currentYear) return false

    minDate?.let {
      val minYear =
          Calendar.getInstance(SWITZERLAND_TIMEZONE)
              .apply { timeInMillis = it.toDate().time }
              .get(Calendar.YEAR)
      if (year < minYear) return false
    }

    maxDate?.let {
      val maxYear =
          Calendar.getInstance(SWITZERLAND_TIMEZONE)
              .apply { timeInMillis = it.toDate().time }
              .get(Calendar.YEAR)
      if (year > maxYear) return false
    }

    return true
  }
}

/**
 * A reusable date picker dialog component with Switzerland timezone support and past date
 * prevention.
 *
 * Features:
 * - All dates are interpreted in Switzerland timezone (Europe/Zurich)
 * - Past dates cannot be selected (automatically disabled in UI)
 * - Selected dates are normalized to midnight in Switzerland timezone
 * - If [initialDate] is in the past, today's date is used instead
 *
 * @param showDialog Controls dialog visibility
 * @param initialDate Initial date to display (null = current date). Past dates are adjusted to
 *   today
 * @param onDismiss Callback when dialog is dismissed
 * @param onDateSelected Callback with selected date as [Timestamp] at midnight in Switzerland
 *   timezone
 * @param yearRange Available year range (default: 2025-3000)
 * @param minDate Optional minimum selectable date (null = no minimum constraint beyond today)
 * @param maxDate Optional maximum selectable date (null = no maximum constraint)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    showDialog: Boolean,
    initialDate: Timestamp? = null,
    onDismiss: () -> Unit,
    onDateSelected: (Timestamp) -> Unit,
    yearRange: IntRange = IntRange(2025, 3000),
    minDate: Timestamp? = null,
    maxDate: Timestamp? = null
) {
  // Calculate initial milliseconds only when initialDate or constraints change
  // If initialDate is in the past or outside constraints, adjust it
  val initialMillis =
      remember(initialDate, minDate, maxDate) {
        val todayMidnightUtc = getTodayMidnightUtc()

        // Calculate effective min/max constraints
        val effectiveMinUtc = minDate?.let { timestampToMidnightUtc(it) } ?: todayMidnightUtc
        val effectiveMaxUtc = maxDate?.let { timestampToMidnightUtc(it) } ?: Long.MAX_VALUE

        // Determine the initial date UTC
        val initialMidnightUtc =
            if (initialDate != null) {
              var dateUtc = timestampToMidnightUtc(initialDate)
              // Adjust if outside constraints
              dateUtc = dateUtc.coerceIn(effectiveMinUtc, effectiveMaxUtc)
              // Ensure not in the past
              dateUtc.coerceAtLeast(todayMidnightUtc)
            } else {
              // Use today, adjusted to constraints
              todayMidnightUtc.coerceIn(effectiveMinUtc, effectiveMaxUtc)
            }

        // Convert to system timezone at noon for DatePickerState
        utcMillisToSystemNoon(initialMidnightUtc)
      }

  // Create SelectableDates instance with constraints
  val selectableDates =
      remember(minDate, maxDate) {
        if (minDate != null || maxDate != null) {
          ConstrainedSelectableDates(minDate, maxDate)
        } else {
          FutureSelectableDates()
        }
      }

  // DatePickerState must be recreated when selectableDates changes to apply constraints
  val datePickerState =
      remember(initialMillis, selectableDates) {
        DatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = yearRange,
            locale = SWITZERLAND_LOCALE,
            selectableDates = selectableDates)
      }

  if (showDialog) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
          TextButton(
              onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                  val selectedTimestamp = systemMillisToTimestamp(millis)
                  val selectedMidnightUtc = timestampToMidnightUtc(selectedTimestamp)
                  val todayMidnightUtc = getTodayMidnightUtc()

                  // Ensure selected date is not in the past (safety check)
                  val finalTimestamp =
                      if (selectedMidnightUtc < todayMidnightUtc) {
                        // Use today's date if somehow a past date was selected
                        val todaySwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
                        todaySwiss.timeInMillis = todayMidnightUtc
                        Timestamp(todaySwiss.time)
                      } else {
                        selectedTimestamp
                      }

                  onDateSelected(finalTimestamp)
                }
                onDismiss()
              },
              modifier = Modifier.testTag(C.CustomDatePickerDialogTags.OK_BUTTON),
              colors = ButtonDefaults.textButtonColors(contentColor = MainColor)) {
                Text("OK")
              }
        },
        dismissButton = {
          TextButton(
              onClick = onDismiss,
              modifier = Modifier.testTag(C.CustomDatePickerDialogTags.CANCEL_BUTTON),
              colors = ButtonDefaults.textButtonColors(contentColor = TextColor)) {
                Text(stringResource(R.string.cancel))
              }
        }) {
          val currentConfig = LocalConfiguration.current
          val swissConfig =
              remember(currentConfig) {
                Configuration(currentConfig).apply { setLocale(SWITZERLAND_LOCALE) }
              }

          CompositionLocalProvider(LocalConfiguration provides swissConfig) {
            DatePicker(
                state = datePickerState,
                colors =
                    DatePickerDefaults.colors(
                        containerColor = BackGroundColor,
                        selectedDayContainerColor = MainColor,
                        selectedDayContentColor = Color.White,
                        todayDateBorderColor = MainColor,
                        todayContentColor = MainColor,
                        dateTextFieldColors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = BackGroundColor,
                                unfocusedContainerColor = BackGroundColor,
                                focusedIndicatorColor = MainColor,
                                unfocusedIndicatorColor = TextColor,
                                errorIndicatorColor = Color.Red,
                                focusedTextColor = TextColor,
                                unfocusedTextColor = TextColor,
                                focusedLabelColor = TextColor,
                                unfocusedLabelColor = TextColor,
                                cursorColor = MainColor)))
          }
        }
  }
}
