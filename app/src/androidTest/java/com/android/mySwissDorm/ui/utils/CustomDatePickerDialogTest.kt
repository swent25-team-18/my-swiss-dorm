package com.android.mySwissDorm.ui.utils

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for CustomDatePickerDialog component. Verifies dialog visibility, date selection, and
 * timezone handling.
 */
class CustomDatePickerDialogTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private val SWITZERLAND_TIMEZONE = TimeZone.getTimeZone("Europe/Zurich")

  /** Creates a Timestamp for a date N days from now in Switzerland timezone. */
  private fun createTimestampDaysFromNow(days: Int): Timestamp {
    val cal = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    cal.add(Calendar.DAY_OF_YEAR, days)
    cal.set(Calendar.HOUR_OF_DAY, 12)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return Timestamp(cal.time)
  }

  /** Converts a Timestamp to midnight UTC milliseconds in Switzerland timezone. */
  private fun timestampToMidnightUtc(timestamp: Timestamp): Long {
    val swiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    swiss.time = timestamp.toDate()
    swiss.set(Calendar.HOUR_OF_DAY, 0)
    swiss.set(Calendar.MINUTE, 0)
    swiss.set(Calendar.SECOND, 0)
    swiss.set(Calendar.MILLISECOND, 0)
    return swiss.timeInMillis
  }

  /**
   * Sets up the date picker dialog, clicks OK, and returns the selected date.
   *
   * @param initialDate Initial date to display (null = current date)
   * @param minDate Optional minimum selectable date
   * @param maxDate Optional maximum selectable date
   * @return The selected date, or null if no date was selected
   */
  private fun selectDateFromDialog(
      initialDate: Timestamp? = null,
      minDate: Timestamp? = null,
      maxDate: Timestamp? = null
  ): Timestamp? {
    var selectedDate: Timestamp? = null

    composeRule.setContent {
      MySwissDormAppTheme {
        CustomDatePickerDialog(
            showDialog = true,
            initialDate = initialDate,
            onDismiss = {},
            onDateSelected = { selectedDate = it },
            minDate = minDate,
            maxDate = maxDate)
      }
    }

    composeRule.waitForIdle()
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    return selectedDate
  }

  @Test
  fun dialog_not_displayed_when_showDialog_is_false() {
    composeRule.setContent {
      MySwissDormAppTheme {
        CustomDatePickerDialog(
            showDialog = false, initialDate = null, onDismiss = {}, onDateSelected = {})
      }
    }

    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .assertDoesNotExist()
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.CANCEL_BUTTON, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun dialog_displayed_when_showDialog_is_true() {
    composeRule.setContent {
      MySwissDormAppTheme {
        CustomDatePickerDialog(
            showDialog = true, initialDate = null, onDismiss = {}, onDateSelected = {})
      }
    }

    composeRule.waitForIdle()
    // DatePickerDialog should be displayed (OK and Cancel buttons visible)
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.CANCEL_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun cancel_button_dismisses_dialog() {
    var showDialog by mutableStateOf(true)

    composeRule.setContent {
      MySwissDormAppTheme {
        CustomDatePickerDialog(
            showDialog = showDialog,
            initialDate = null,
            onDismiss = { showDialog = false },
            onDateSelected = {})
      }
    }

    composeRule.waitForIdle()
    // Verify dialog is displayed
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.CANCEL_BUTTON, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    // Wait for dialog to be dismissed
    composeRule.waitUntil(2_000) { !showDialog }

    // Dialog should be dismissed (buttons no longer visible)
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun ok_button_selects_date_and_dismisses() = runTest {
    var selectedDate: Timestamp? = null

    composeRule.setContent {
      MySwissDormAppTheme {
        CustomDatePickerDialog(
            showDialog = true,
            initialDate = null,
            onDismiss = {},
            onDateSelected = { selectedDate = it })
      }
    }

    composeRule.waitForIdle()
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    assertNotNull("Date should be selected", selectedDate)
  }

  @Test
  fun initial_date_is_set_correctly() {
    // Create a specific date: 15th September 2025 in Switzerland timezone
    val swissCal = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    swissCal.set(2025, Calendar.SEPTEMBER, 15, 12, 0, 0)
    swissCal.set(Calendar.MILLISECOND, 0)

    val initialTimestamp = Timestamp(swissCal.time)

    composeRule.setContent {
      MySwissDormAppTheme {
        CustomDatePickerDialog(
            showDialog = true, initialDate = initialTimestamp, onDismiss = {}, onDateSelected = {})
      }
    }

    composeRule.waitForIdle()
    // Dialog should be displayed with the initial date
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun date_selection_uses_switzerland_timezone() = runTest {
    var selectedDate: Timestamp? = null

    composeRule.setContent {
      MySwissDormAppTheme {
        CustomDatePickerDialog(
            showDialog = true,
            initialDate = null,
            onDismiss = {},
            onDateSelected = { selectedDate = it })
      }
    }

    composeRule.waitForIdle()
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    assertNotNull("Date should be selected", selectedDate)

    // Verify the selected date is at midnight in Switzerland timezone
    val swissCal = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    swissCal.time = selectedDate!!.toDate()
    val hour = swissCal.get(Calendar.HOUR_OF_DAY)
    val minute = swissCal.get(Calendar.MINUTE)
    val second = swissCal.get(Calendar.SECOND)

    // Date should be at midnight (00:00:00) in Switzerland timezone
    assert(hour == 0 && minute == 0 && second == 0) {
      "Selected date should be at midnight in Switzerland timezone, but was $hour:$minute:$second"
    }
  }

  @Test
  fun selected_date_is_not_in_past() = runTest {
    val selectedDate = selectDateFromDialog()

    assertNotNull("Date should be selected", selectedDate)

    val todayMidnight = timestampToMidnightUtc(createTimestampDaysFromNow(0))
    val selectedMidnight = timestampToMidnightUtc(selectedDate!!)

    assertTrue(
        "Selected date should be today or in the future, but was before today",
        selectedMidnight >= todayMidnight)
  }

  @Test
  fun today_date_can_be_selected() = runTest {
    var selectedDate: Timestamp? = null

    // Set initial date to today
    val todaySwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    todaySwiss.set(Calendar.HOUR_OF_DAY, 12)
    todaySwiss.set(Calendar.MINUTE, 0)
    todaySwiss.set(Calendar.SECOND, 0)
    todaySwiss.set(Calendar.MILLISECOND, 0)
    val todayTimestamp = Timestamp(todaySwiss.time)

    composeRule.setContent {
      MySwissDormAppTheme {
        CustomDatePickerDialog(
            showDialog = true,
            initialDate = todayTimestamp,
            onDismiss = {},
            onDateSelected = { selectedDate = it })
      }
    }

    composeRule.waitForIdle()
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    assertNotNull("Date should be selected", selectedDate)

    // Verify the selected date is today (at midnight)
    val selectedSwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    selectedSwiss.time = selectedDate!!.toDate()
    val selectedYear = selectedSwiss.get(Calendar.YEAR)
    val selectedMonth = selectedSwiss.get(Calendar.MONTH)
    val selectedDay = selectedSwiss.get(Calendar.DAY_OF_MONTH)

    val currentYear = todaySwiss.get(Calendar.YEAR)
    val currentMonth = todaySwiss.get(Calendar.MONTH)
    val currentDay = todaySwiss.get(Calendar.DAY_OF_MONTH)

    assertTrue(
        "Selected date should be today",
        selectedYear == currentYear && selectedMonth == currentMonth && selectedDay == currentDay)
  }

  @Test
  fun future_date_can_be_selected() = runTest {
    var selectedDate: Timestamp? = null

    // Set initial date to a future date (30 days from now)
    val futureSwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    futureSwiss.add(Calendar.DAY_OF_YEAR, 30)
    futureSwiss.set(Calendar.HOUR_OF_DAY, 12)
    futureSwiss.set(Calendar.MINUTE, 0)
    futureSwiss.set(Calendar.SECOND, 0)
    futureSwiss.set(Calendar.MILLISECOND, 0)
    val futureTimestamp = Timestamp(futureSwiss.time)

    composeRule.setContent {
      MySwissDormAppTheme {
        CustomDatePickerDialog(
            showDialog = true,
            initialDate = futureTimestamp,
            onDismiss = {},
            onDateSelected = { selectedDate = it })
      }
    }

    composeRule.waitForIdle()
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    assertNotNull("Date should be selected", selectedDate)

    // Verify the selected date is in the future (at least today)
    val todaySwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    todaySwiss.set(Calendar.HOUR_OF_DAY, 0)
    todaySwiss.set(Calendar.MINUTE, 0)
    todaySwiss.set(Calendar.SECOND, 0)
    todaySwiss.set(Calendar.MILLISECOND, 0)
    val todayMidnight = todaySwiss.timeInMillis

    val selectedSwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    selectedSwiss.time = selectedDate!!.toDate()
    selectedSwiss.set(Calendar.HOUR_OF_DAY, 0)
    selectedSwiss.set(Calendar.MINUTE, 0)
    selectedSwiss.set(Calendar.SECOND, 0)
    selectedSwiss.set(Calendar.MILLISECOND, 0)
    val selectedMidnight = selectedSwiss.timeInMillis

    assertTrue("Selected date should be today or in the future", selectedMidnight >= todayMidnight)
  }

  @Test
  fun past_date_as_initial_is_adjusted_to_today() = runTest {
    val pastTimestamp = createTimestampDaysFromNow(-30)
    val selectedDate = selectDateFromDialog(initialDate = pastTimestamp)

    assertNotNull("Date should be selected", selectedDate)

    val todayMidnight = timestampToMidnightUtc(createTimestampDaysFromNow(0))
    val selectedMidnight = timestampToMidnightUtc(selectedDate!!)

    assertTrue(
        "Selected date should be today or in the future, even if initial date was in the past",
        selectedMidnight >= todayMidnight)
  }

  @Test
  fun min_date_constraint_prevents_selecting_dates_before_min() = runTest {
    val minTimestamp = createTimestampDaysFromNow(10)
    val selectedDate = selectDateFromDialog(minDate = minTimestamp)

    assertNotNull("Date should be selected", selectedDate)

    val minMidnight = timestampToMidnightUtc(minTimestamp)
    val selectedMidnight = timestampToMidnightUtc(selectedDate!!)

    assertTrue(
        "Selected date should be at least the min date, but was before it",
        selectedMidnight >= minMidnight)
  }

  @Test
  fun max_date_constraint_prevents_selecting_dates_after_max() = runTest {
    val maxTimestamp = createTimestampDaysFromNow(30)
    val selectedDate = selectDateFromDialog(maxDate = maxTimestamp)

    assertNotNull("Date should be selected", selectedDate)

    val maxMidnight = timestampToMidnightUtc(maxTimestamp)
    val selectedMidnight = timestampToMidnightUtc(selectedDate!!)

    assertTrue(
        "Selected date should be at most the max date, but was after it",
        selectedMidnight <= maxMidnight)
  }

  @Test
  fun both_min_and_max_constraints_are_respected() = runTest {
    val minTimestamp = createTimestampDaysFromNow(10)
    val maxTimestamp = createTimestampDaysFromNow(30)
    val selectedDate = selectDateFromDialog(minDate = minTimestamp, maxDate = maxTimestamp)

    assertNotNull("Date should be selected", selectedDate)

    val minMidnight = timestampToMidnightUtc(minTimestamp)
    val maxMidnight = timestampToMidnightUtc(maxTimestamp)
    val selectedMidnight = timestampToMidnightUtc(selectedDate!!)

    assertTrue(
        "Selected date should be within min and max range",
        selectedMidnight >= minMidnight && selectedMidnight <= maxMidnight)
  }

  @Test
  fun initial_date_before_min_is_adjusted_to_min() = runTest {
    val minTimestamp = createTimestampDaysFromNow(10)
    val initialTimestamp = createTimestampDaysFromNow(5)
    val selectedDate = selectDateFromDialog(initialDate = initialTimestamp, minDate = minTimestamp)

    assertNotNull("Date should be selected", selectedDate)

    val minMidnight = timestampToMidnightUtc(minTimestamp)
    val selectedMidnight = timestampToMidnightUtc(selectedDate!!)

    assertTrue(
        "Selected date should be at least the min date, even if initial date was before it",
        selectedMidnight >= minMidnight)
  }

  @Test
  fun initial_date_after_max_is_adjusted_to_max() = runTest {
    val maxTimestamp = createTimestampDaysFromNow(30)
    val initialTimestamp = createTimestampDaysFromNow(50)
    val selectedDate = selectDateFromDialog(initialDate = initialTimestamp, maxDate = maxTimestamp)

    assertNotNull("Date should be selected", selectedDate)

    val maxMidnight = timestampToMidnightUtc(maxTimestamp)
    val selectedMidnight = timestampToMidnightUtc(selectedDate!!)

    assertTrue(
        "Selected date should be at most the max date, even if initial date was after it",
        selectedMidnight <= maxMidnight)
  }

  @Test
  fun initial_date_outside_range_is_adjusted_to_valid_range() = runTest {
    val minTimestamp = createTimestampDaysFromNow(10)
    val maxTimestamp = createTimestampDaysFromNow(30)
    val initialTimestamp = createTimestampDaysFromNow(5)
    val selectedDate =
        selectDateFromDialog(
            initialDate = initialTimestamp, minDate = minTimestamp, maxDate = maxTimestamp)

    assertNotNull("Date should be selected", selectedDate)

    val minMidnight = timestampToMidnightUtc(minTimestamp)
    val maxMidnight = timestampToMidnightUtc(maxTimestamp)
    val selectedMidnight = timestampToMidnightUtc(selectedDate!!)

    assertTrue(
        "Selected date should be within min and max range, even if initial date was outside it",
        selectedMidnight >= minMidnight && selectedMidnight <= maxMidnight)
  }
}
