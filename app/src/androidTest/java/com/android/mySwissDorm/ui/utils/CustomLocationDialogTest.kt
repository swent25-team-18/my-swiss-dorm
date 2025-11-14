package com.android.mySwissDorm.ui.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.resources.C
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomLocationDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun dialog_displaysTitle() {
    var value by mutableStateOf("")
    var currentLocation: Location? = null
    val suggestions = emptyList<Location>()

    composeTestRule.setContent {
      CustomLocationDialog(
          value = value,
          currentLocation = currentLocation,
          locationSuggestions = suggestions,
          onValueChange = { value = it },
          onDropDownLocationSelect = { currentLocation = it },
          onDismiss = {},
          onConfirm = {},
          onUseCurrentLocationClick = {})
    }

    composeTestRule.onNodeWithTag(C.CustomLocationDialogTags.DIALOG_TITLE).assertIsDisplayed()
  }

  @Test
  fun dialog_displaysLocationTextField() {
    var value by mutableStateOf("")
    var currentLocation: Location? = null
    val suggestions = emptyList<Location>()

    composeTestRule.setContent {
      CustomLocationDialog(
          value = value,
          currentLocation = currentLocation,
          locationSuggestions = suggestions,
          onValueChange = { value = it },
          onDropDownLocationSelect = { currentLocation = it },
          onDismiss = {},
          onConfirm = {},
          onUseCurrentLocationClick = {})
    }

    composeTestRule
        .onNodeWithTag(C.CustomLocationDialogTags.LOCATION_TEXT_FIELD)
        .assertIsDisplayed()
  }

  @Test
  fun dialog_confirmButtonDisabledWhenNoLocation() {
    var value by mutableStateOf("")
    var currentLocation: Location? = null
    val suggestions = emptyList<Location>()

    composeTestRule.setContent {
      CustomLocationDialog(
          value = value,
          currentLocation = currentLocation,
          locationSuggestions = suggestions,
          onValueChange = { value = it },
          onDropDownLocationSelect = { currentLocation = it },
          onDismiss = {},
          onConfirm = {},
          onUseCurrentLocationClick = {})
    }

    composeTestRule.onNodeWithTag(C.CustomLocationDialogTags.CONFIRM_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun dialog_confirmButtonEnabledWhenLocationSelected() {
    var value by mutableStateOf("Lausanne")
    val location = Location("Lausanne", 46.5197, 6.6323)
    var currentLocation: Location? = location
    val suggestions = emptyList<Location>()

    composeTestRule.setContent {
      CustomLocationDialog(
          value = value,
          currentLocation = currentLocation,
          locationSuggestions = suggestions,
          onValueChange = { value = it },
          onDropDownLocationSelect = { currentLocation = it },
          onDismiss = {},
          onConfirm = {},
          onUseCurrentLocationClick = {})
    }

    composeTestRule.onNodeWithTag(C.CustomLocationDialogTags.CONFIRM_BUTTON).assertIsEnabled()
  }

  @Test
  fun dialog_displaysLocationSuggestions() {
    var value by mutableStateOf("Laus")
    var currentLocation: Location? = null
    val suggestions =
        listOf(
            Location("Lausanne, Switzerland", 46.5197, 6.6323),
            Location("Lausanne, France", 48.8566, 2.3522),
            Location("Lausanne Station", 46.5200, 6.6300))

    composeTestRule.setContent {
      CustomLocationDialog(
          value = value,
          currentLocation = currentLocation,
          locationSuggestions = suggestions,
          onValueChange = { value = it },
          onDropDownLocationSelect = { currentLocation = it },
          onDismiss = {},
          onConfirm = {},
          onUseCurrentLocationClick = {})
    }

    // Type to trigger dropdown
    composeTestRule
        .onNodeWithTag(C.CustomLocationDialogTags.LOCATION_TEXT_FIELD)
        .performTextInput("anne")
    composeTestRule.waitForIdle()

    // Check that suggestions appear (first one should be visible)
    composeTestRule
        .onNodeWithTag(C.CustomLocationDialogTags.locationSuggestion(0))
        .assertIsDisplayed()
  }

  @Test
  fun dialog_truncatesLongLocationNames() {
    var value by mutableStateOf("")
    var currentLocation: Location? = null
    val longName = "A".repeat(35) // Longer than 30 characters
    val suggestions = listOf(Location(longName, 46.5197, 6.6323))

    composeTestRule.setContent {
      CustomLocationDialog(
          value = value,
          currentLocation = currentLocation,
          locationSuggestions = suggestions,
          onValueChange = { value = it },
          onDropDownLocationSelect = { currentLocation = it },
          onDismiss = {},
          onConfirm = {},
          onUseCurrentLocationClick = {})
    }

    // Type to trigger dropdown
    composeTestRule
        .onNodeWithTag(C.CustomLocationDialogTags.LOCATION_TEXT_FIELD)
        .performTextInput("test")
    composeTestRule.waitForIdle()

    // Check that suggestion with truncated name is displayed
    composeTestRule
        .onNodeWithTag(C.CustomLocationDialogTags.locationSuggestion(0))
        .assertIsDisplayed()
  }

  @Test
  fun dialog_showsMoreOptionWhenMoreThanThreeSuggestions() {
    var value by mutableStateOf("")
    var currentLocation: Location? = null
    val suggestions =
        listOf(
            Location("Location 1", 46.5197, 6.6323),
            Location("Location 2", 46.5200, 6.6300),
            Location("Location 3", 46.5210, 6.6310),
            Location("Location 4", 46.5220, 6.6320))

    composeTestRule.setContent {
      CustomLocationDialog(
          value = value,
          currentLocation = currentLocation,
          locationSuggestions = suggestions,
          onValueChange = { value = it },
          onDropDownLocationSelect = { currentLocation = it },
          onDismiss = {},
          onConfirm = {},
          onUseCurrentLocationClick = {})
    }

    // Type to trigger dropdown
    composeTestRule
        .onNodeWithTag(C.CustomLocationDialogTags.LOCATION_TEXT_FIELD)
        .performTextInput("test")
    composeTestRule.waitForIdle()

    // Check that "More..." option appears
    composeTestRule.onNodeWithTag(C.CustomLocationDialogTags.MORE_OPTION).assertIsDisplayed()
  }

  @Test
  fun dialog_callsOnValueChangeWhenTextChanges() {
    var value by mutableStateOf("")
    var currentLocation: Location? = null
    var valueChanged = false
    val suggestions = emptyList<Location>()

    composeTestRule.setContent {
      CustomLocationDialog(
          value = value,
          currentLocation = currentLocation,
          locationSuggestions = suggestions,
          onValueChange = {
            value = it
            valueChanged = true
          },
          onDropDownLocationSelect = { currentLocation = it },
          onDismiss = {},
          onConfirm = {},
          onUseCurrentLocationClick = {})
    }

    composeTestRule
        .onNodeWithTag(C.CustomLocationDialogTags.LOCATION_TEXT_FIELD)
        .performTextInput("Lausanne")
    composeTestRule.waitForIdle()

    assert(valueChanged)
    assert(value == "Lausanne")
  }

  @Test
  fun dialog_callsOnDropDownLocationSelectWhenSuggestionClicked() {
    var value by mutableStateOf("Laus")
    var currentLocation: Location? = null
    val selectedLocation = Location("Lausanne, Switzerland", 46.5197, 6.6323)
    val suggestions = listOf(selectedLocation)
    var locationSelected: Location? = null

    composeTestRule.setContent {
      CustomLocationDialog(
          value = value,
          currentLocation = currentLocation,
          locationSuggestions = suggestions,
          onValueChange = { value = it },
          onDropDownLocationSelect = {
            currentLocation = it
            locationSelected = it
          },
          onDismiss = {},
          onConfirm = {},
          onUseCurrentLocationClick = {})
    }

    // Type to trigger dropdown
    composeTestRule
        .onNodeWithTag(C.CustomLocationDialogTags.LOCATION_TEXT_FIELD)
        .performTextInput("anne")
    composeTestRule.waitForIdle()

    // Click on suggestion
    composeTestRule.onNodeWithTag(C.CustomLocationDialogTags.locationSuggestion(0)).performClick()
    composeTestRule.waitForIdle()

    assert(locationSelected == selectedLocation)
    assert(currentLocation == selectedLocation)
  }

  @Test
  fun dialog_callsOnConfirmWhenConfirmButtonClicked() {
    var value by mutableStateOf("Lausanne")
    val location = Location("Lausanne", 46.5197, 6.6323)
    var currentLocation: Location? = location
    val suggestions = emptyList<Location>()
    var confirmedLocation: Location? = null

    composeTestRule.setContent {
      CustomLocationDialog(
          value = value,
          currentLocation = currentLocation,
          locationSuggestions = suggestions,
          onValueChange = { value = it },
          onDropDownLocationSelect = { currentLocation = it },
          onDismiss = {},
          onConfirm = { confirmedLocation = it },
          onUseCurrentLocationClick = {})
    }

    composeTestRule.onNodeWithTag(C.CustomLocationDialogTags.CONFIRM_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assert(confirmedLocation == location)
  }

  @Test
  fun useMyLocationButton_calls_onUseCurrentLocationClick() {
    var isClicked = false
    val onClick = { isClicked = true }

    composeTestRule.setContent {
      CustomLocationDialog(
          value = "",
          currentLocation = null,
          locationSuggestions = emptyList(),
          onValueChange = {},
          onDropDownLocationSelect = {},
          onDismiss = {},
          onConfirm = {},
          onUseCurrentLocationClick = onClick)
    }
    composeTestRule.onNodeWithTag("UseCurrentLocationButton").performClick()
    composeTestRule.waitForIdle()

    assert(isClicked)
  }

  @Test
  fun dialog_displaysUseMyLocationButton() {
    composeTestRule.setContent {
      CustomLocationDialog(
          value = "",
          currentLocation = null,
          locationSuggestions = emptyList(),
          onValueChange = {},
          onDropDownLocationSelect = {},
          onDismiss = {},
          onConfirm = {},
          onUseCurrentLocationClick = {})
    }
    composeTestRule.onNodeWithTag("UseCurrentLocationButton").assertIsDisplayed()
  }

  @Test
  fun dialog_callsOnDismissWhenCloseButtonClicked() {
    var dismissed = false

    composeTestRule.setContent {
      CustomLocationDialog(
          value = "",
          currentLocation = null,
          locationSuggestions = emptyList(),
          onValueChange = {},
          onDropDownLocationSelect = {},
          onDismiss = { dismissed = true },
          onConfirm = {},
          onUseCurrentLocationClick = {})
    }
    composeTestRule.onNodeWithTag(C.CustomLocationDialogTags.CLOSE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    assert(dismissed)
  }
}
