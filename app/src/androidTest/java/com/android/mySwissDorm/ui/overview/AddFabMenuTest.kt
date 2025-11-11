package com.android.mySwissDorm.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddFabMenuTest {

  @get:Rule val composeRule = createComposeRule()

  private fun setUnderTest(onAddListing: () -> Unit = {}, onAddReview: () -> Unit = {}) {
    composeRule.setContent {
      MySwissDormAppTheme { AddFabMenu(onAddListing = onAddListing, onAddReview = onAddReview) }
    }
  }

  @Test
  fun fab_isVisible_initially_menu_isClosed() {
    setUnderTest()

    composeRule.onNodeWithTag("fab_main").assertExists().assertIsDisplayed()
    composeRule.onNodeWithTag("fab_menu_listing").assertDoesNotExist()
    composeRule.onNodeWithTag("fab_menu_review").assertDoesNotExist()
  }

  @Test
  fun tapping_fab_opens_menu_and_shows_actions() {
    setUnderTest()

    composeRule.onNodeWithTag("fab_main").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("fab_menu_listing").assertExists().assertIsDisplayed()
    composeRule.onNodeWithTag("fab_menu_review").assertExists().assertIsDisplayed()
    composeRule.onNodeWithTag("fab_scrim").assertExists().assertIsDisplayed()
  }

  @Test
  fun tapping_fab_again_closes_menu() {
    setUnderTest()

    composeRule.onNodeWithTag("fab_main").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("fab_main").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("fab_menu_listing").assertDoesNotExist()
    composeRule.onNodeWithTag("fab_menu_review").assertDoesNotExist()
  }

  @Test
  fun tapping_scrim_closes_menu() {
    setUnderTest()

    // Open
    composeRule.onNodeWithTag("fab_main").performClick()
    composeRule.waitForIdle()
    // Close via scrim
    composeRule.onNodeWithTag("fab_scrim").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("fab_menu_listing").assertDoesNotExist()
    composeRule.onNodeWithTag("fab_menu_review").assertDoesNotExist()
  }

  @Test
  fun clicking_add_listing_invokes_callback_and_closes_menu() {
    var listingClicks = 0
    setUnderTest(onAddListing = { listingClicks++ })

    composeRule.onNodeWithTag("fab_main").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("fab_menu_listing").performClick()
    composeRule.waitForIdle()

    assertEquals(
        "onAddListing should be called exactly once",
        1,
        listingClicks,
    )
    composeRule.onNodeWithTag("fab_menu_listing").assertDoesNotExist()
    composeRule.onNodeWithTag("fab_menu_review").assertDoesNotExist()
  }

  @Test
  fun clicking_add_review_invokes_callback_and_closes_menu() {
    var reviewClicks = 0
    setUnderTest(onAddReview = { reviewClicks++ })

    composeRule.onNodeWithTag("fab_main").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("fab_menu_review").performClick()
    composeRule.waitForIdle()

    assertEquals(
        "onAddReview should be called exactly once",
        1,
        reviewClicks,
    )
    composeRule.onNodeWithTag("fab_menu_listing").assertDoesNotExist()
    composeRule.onNodeWithTag("fab_menu_review").assertDoesNotExist()
  }
}
