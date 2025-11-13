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

                  onDateSelected(Timestamp(swissCal.time))
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
