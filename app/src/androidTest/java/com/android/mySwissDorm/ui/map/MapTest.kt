package com.android.mySwissDorm.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.map.Location
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumentation tests for MapScreen and MapPreview composables. */
@RunWith(AndroidJUnit4::class)
class MapTest {

  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule val intentsRule = IntentsRule()

  @Test
  fun mapPreview_whenClicked_triggersOnMapClickCallback() {
    // Arrange
    var isClicked = false
    val testLocation = Location(name = "Lausanne", latitude = 46.5196, longitude = 6.6322)

    composeTestRule.setContent {
      MapPreview(
          location = testLocation,
          title = "Test Location",
          modifier = Modifier.testTag("mapPreview"),
          onMapClick = { isClicked = true })
    }
    composeTestRule.onNodeWithTag("mapPreview").performClick()
    assert(isClicked) { "onMapClick callback was not triggered." }
  }

  @Test
  fun mapScreen_displaysCorrectDynamicTitle() {
    composeTestRule.setContent {
      MapScreen(
          latitude = 47.3769,
          longitude = 8.5417,
          title = "Zurich",
          onGoBack = {},
          nameId = R.string.view_listing_listing_location)
    }
    composeTestRule.onNodeWithText("Listing Location").assertIsDisplayed()
  }

  @Test
  fun mapScreen_backButton_triggersOnGoBackCallback() {
    var isBackPressed = false
    composeTestRule.setContent {
      MapScreen(
          latitude = 47.3769,
          longitude = 8.5417,
          title = "Zurich",
          onGoBack = { isBackPressed = true },
          nameId = R.string.view_listing_listing_location)
    }
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    assert(isBackPressed) { "onGoBack callback was not triggered." }
  }

  @Test
  fun mapScreen_navigationFab_firesCorrectGoogleMapsIntent() {
    val testLat = 47.3769
    val testLon = 8.5417
    composeTestRule.setContent {
      MapScreen(
          latitude = testLat,
          longitude = testLon,
          title = "Zurich",
          onGoBack = {},
          nameId = R.string.view_listing_listing_location)
    }
    composeTestRule.onNodeWithContentDescription("Open in Maps").performClick()
    Intents.intended(
        allOf(
            hasAction(Intent.ACTION_VIEW),
            hasData(Uri.parse("google.navigation:q=$testLat,$testLon")),
            hasPackage("com.google.android.apps.maps")))
  }
}
