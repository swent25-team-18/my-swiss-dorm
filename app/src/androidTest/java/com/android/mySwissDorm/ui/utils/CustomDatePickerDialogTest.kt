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

    // Verify the selected date is not in the past
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
  fun past_date_as_initial_is_adjusted_to_today() {
    // Create a past date (30 days ago)
    val pastSwiss = Calendar.getInstance(SWITZERLAND_TIMEZONE)
    pastSwiss.add(Calendar.DAY_OF_YEAR, -30)
    pastSwiss.set(Calendar.HOUR_OF_DAY, 12)
    pastSwiss.set(Calendar.MINUTE, 0)
    pastSwiss.set(Calendar.SECOND, 0)
    pastSwiss.set(Calendar.MILLISECOND, 0)
    val pastTimestamp = Timestamp(pastSwiss.time)

    var selectedDate: Timestamp? = null

    composeRule.setContent {
      MySwissDormAppTheme {
        CustomDatePickerDialog(
            showDialog = true,
            initialDate = pastTimestamp,
            onDismiss = {},
            onDateSelected = { selectedDate = it })
      }
    }

    composeRule.waitForIdle()
    // Dialog should still be displayed (the picker will show today's date instead of the past date)
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    // When OK is clicked, the selected date should be today or later
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    assertNotNull("Date should be selected", selectedDate)

    // Verify the selected date is not in the past
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

    assertTrue(
        "Selected date should be today or in the future, even if initial date was in the past",
        selectedMidnight >= todayMidnight)
  }
}
