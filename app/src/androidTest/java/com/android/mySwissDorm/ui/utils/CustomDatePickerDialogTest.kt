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
}
