package com.android.mySwissDorm.ui.utils

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val SWITZERLAND_TIMEZONE = TimeZone.getTimeZone("Europe/Zurich")
private val SWITZERLAND_LOCALE = Locale("fr", "CH")

/** A reusable date picker dialog component. */
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
  val initialMillis =
      remember(initialDate) {
        if (initialDate != null) {
          // Convert UTC timestamp to Switzerland timezone and get date components
          val swissCal = Calendar.getInstance(SWITZERLAND_TIMEZONE)
          swissCal.timeInMillis = initialDate.toDate().time
          val year = swissCal.get(Calendar.YEAR)
          val month = swissCal.get(Calendar.MONTH)
          val day = swissCal.get(Calendar.DAY_OF_MONTH)

          // Create date at noon in system timezone (to avoid DST issues)
          val systemCal = Calendar.getInstance()
          systemCal.set(year, month, day, 12, 0, 0)
          systemCal.set(Calendar.MILLISECOND, 0)
          systemCal.timeInMillis
        } else {
          System.currentTimeMillis()
        }
      }

  // Create DatePickerState - only recreate when initialMillis changes
  // This ensures the state is stable when the dialog is open
  val datePickerState =
      remember(initialMillis) {
        DatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = yearRange,
            locale = SWITZERLAND_LOCALE)
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

                  // Convert to UTC
                  val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                  utcCal.timeInMillis = swissCal.timeInMillis

                  onDateSelected(Timestamp(utcCal.time))
                }
                onDismiss()
              },
              colors = ButtonDefaults.textButtonColors(contentColor = MainColor)) {
                Text("OK")
              }
        },
        dismissButton = {
          TextButton(
              onClick = onDismiss,
              colors = ButtonDefaults.textButtonColors(contentColor = TextColor)) {
                Text("Cancel")
              }
        }) {
          DatePicker(
              state = datePickerState,
              colors =
                  DatePickerDefaults.colors(
                      containerColor = BackGroundColor,
                      selectedDayContainerColor = MainColor,
                      selectedDayContentColor = Color.White,
                      todayDateBorderColor = MainColor,
                      todayContentColor = MainColor))
        }
  }
}
