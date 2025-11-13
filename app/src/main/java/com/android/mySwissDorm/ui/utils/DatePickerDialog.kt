package com.android.mySwissDorm.ui.utils

import android.content.res.Configuration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
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

/**
 * [SelectableDates] implementation that prevents selecting past dates. Only dates from today
 * onwards (in Switzerland timezone) are selectable.
 */
@OptIn(ExperimentalMaterial3Api::class)
private class FutureSelectableDates : SelectableDates {
  override fun isSelectableDate(utcTimeMillis: Long): Boolean {
    // Get today's date at midnight in Switzerland timezone
    val todaySwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    todaySwiss.set(Calendar.HOUR_OF_DAY, 0)
    todaySwiss.set(Calendar.MINUTE, 0)
    todaySwiss.set(Calendar.SECOND, 0)
    todaySwiss.set(Calendar.MILLISECOND, 0)
    val todayMidnightUtc = todaySwiss.timeInMillis

    // Convert UTC milliseconds to date components in Switzerland timezone
    // utcTimeMillis is already in UTC, so we can directly compare
    // But we need to normalize to midnight in Switzerland timezone for fair comparison
    val selectedSwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    selectedSwiss.timeInMillis = utcTimeMillis
    val year = selectedSwiss.get(Calendar.YEAR)
    val month = selectedSwiss.get(Calendar.MONTH)
    val day = selectedSwiss.get(Calendar.DAY_OF_MONTH)

    // Create a calendar for the selected date at midnight in Switzerland timezone
    val selectedMidnightSwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    selectedMidnightSwiss.set(year, month, day, 0, 0, 0)
    selectedMidnightSwiss.set(Calendar.MILLISECOND, 0)
    val selectedMidnightUtc = selectedMidnightSwiss.timeInMillis

    // Only allow dates from today onwards
    return selectedMidnightUtc >= todayMidnightUtc
  }

  override fun isSelectableYear(year: Int): Boolean {
    // Get current year in Switzerland timezone
    val currentYear = Calendar.getInstance(SWITZERLAND_TIMEZONE).get(Calendar.YEAR)
    return year >= currentYear
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    showDialog: Boolean,
    initialDate: Timestamp? = null,
    onDismiss: () -> Unit,
    onDateSelected: (Timestamp) -> Unit,
    yearRange: IntRange = IntRange(2025, 3000)
) {
  // Calculate initial milliseconds only when initialDate changes
  // If initialDate is in the past, use today's date instead
  val initialMillis =
      remember(initialDate) {
        val todaySwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
        todaySwiss.set(Calendar.HOUR_OF_DAY, 0)
        todaySwiss.set(Calendar.MINUTE, 0)
        todaySwiss.set(Calendar.SECOND, 0)
        todaySwiss.set(Calendar.MILLISECOND, 0)
        val todayMidnightUtc = todaySwiss.timeInMillis

        if (initialDate != null) {
          // Convert UTC timestamp to Switzerland timezone and get date components
          val swissCal = Calendar.getInstance(SWITZERLAND_TIMEZONE)
          swissCal.timeInMillis = initialDate.toDate().time
          val year = swissCal.get(Calendar.YEAR)
          val month = swissCal.get(Calendar.MONTH)
          val day = swissCal.get(Calendar.DAY_OF_MONTH)

          // Create a calendar for the initial date at midnight in Switzerland timezone
          val initialMidnightSwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
          initialMidnightSwiss.set(year, month, day, 0, 0, 0)
          initialMidnightSwiss.set(Calendar.MILLISECOND, 0)
          val initialMidnightUtc = initialMidnightSwiss.timeInMillis

          // If initial date is in the past, use today's date instead
          if (initialMidnightUtc < todayMidnightUtc) {
            // Use today's date at noon in system timezone (to avoid DST issues)
            val systemCal = Calendar.getInstance()
            systemCal.timeInMillis = todayMidnightUtc
            systemCal.set(Calendar.HOUR_OF_DAY, 12)
            systemCal.set(Calendar.MINUTE, 0)
            systemCal.set(Calendar.SECOND, 0)
            systemCal.set(Calendar.MILLISECOND, 0)
            systemCal.timeInMillis
          } else {
            // Create date at noon in system timezone (to avoid DST issues)
            val systemCal = Calendar.getInstance()
            systemCal.set(year, month, day, 12, 0, 0)
            systemCal.set(Calendar.MILLISECOND, 0)
            systemCal.timeInMillis
          }
        } else {
          System.currentTimeMillis()
        }
      }

  // Create SelectableDates instance to prevent past dates
  val selectableDates = remember { FutureSelectableDates() }

  val datePickerState =
      remember(initialMillis) {
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
                  // Get date components from system timezone
                  val systemCal = Calendar.getInstance()
                  systemCal.timeInMillis = millis

                  val year = systemCal.get(Calendar.YEAR)
                  val month = systemCal.get(Calendar.MONTH)
                  val day = systemCal.get(Calendar.DAY_OF_MONTH)

                  // Create timestamp at midnight in Switzerland timezone
                  val swissCal = Calendar.getInstance(SWITZERLAND_TIMEZONE)
                  swissCal.set(year, month, day, 0, 0, 0)
                  swissCal.set(Calendar.MILLISECOND, 0)
                  val selectedMidnightUtc = swissCal.timeInMillis

                  // Get today's date at midnight in Switzerland timezone
                  val todaySwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
                  todaySwiss.set(Calendar.HOUR_OF_DAY, 0)
                  todaySwiss.set(Calendar.MINUTE, 0)
                  todaySwiss.set(Calendar.SECOND, 0)
                  todaySwiss.set(Calendar.MILLISECOND, 0)
                  val todayMidnightUtc = todaySwiss.timeInMillis

                  // Ensure selected date is not in the past (safety check)
                  val finalDate =
                      if (selectedMidnightUtc < todayMidnightUtc) {
                        // Use today's date if somehow a past date was selected
                        todaySwiss
                      } else {
                        swissCal
                      }

                  onDateSelected(Timestamp(finalDate.time))
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
                Text("Cancel")
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
