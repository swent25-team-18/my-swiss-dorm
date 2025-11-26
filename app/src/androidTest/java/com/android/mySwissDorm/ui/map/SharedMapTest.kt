package com.android.mySwissDorm.ui.map

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.ui.overview.ListingCardUI
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedMapTest {

  @get:Rule val composeTestRule = createComposeRule()
  private val location = Location("Test Location", 0.0, 0.0)

  private val listingWithImage =
      ListingCardUI(
          title = "Listing With Image",
          leftBullets = listOf("Studio", "1000.-"),
          rightBullets = listOf("Available"),
          listingUid = "uid1",
          image = Uri.parse("http://fake.uri/image.jpg"),
          location = location)

  private val listingNoImage =
      ListingCardUI(
          title = "Listing No Image",
          leftBullets = listOf("Apartment", "2000.-"),
          rightBullets = listOf("Taken"),
          listingUid = "uid2",
          image = null,
          location = location)

  @Test
  fun smallListingCard_displaysPlaceholder_whenNoImage() {
    composeTestRule.setContent {
      SmallListingPreviewCard(listing = listingNoImage, onClick = {}, onClose = {})
    }
    composeTestRule.onNodeWithText("No Images", ignoreCase = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Listing No Image").assertIsDisplayed()
  }

  @Test
  fun smallListingCard_displaysContent_and_HandlesClicks() {
    var clicked = false
    var closed = false

    composeTestRule.setContent {
      SmallListingPreviewCard(
          listing = listingWithImage, onClick = { clicked = true }, onClose = { closed = true })
    }
    composeTestRule.onNodeWithText("Listing With Image").assertIsDisplayed()
    composeTestRule.onNodeWithText("1000.-").assertIsDisplayed()

    composeTestRule.onNodeWithText("Listing With Image").performClick()
    assert(clicked) { "Card click should have set clicked to true" }
    composeTestRule.onNodeWithContentDescription("Close").performClick()
    assert(closed) { "Close click should have set closed to true" }
  }

  @Test
  fun carousel_cycles_next_correctly() {
    val listings =
        listOf(
            listingWithImage.copy(title = "Item 1"),
            listingWithImage.copy(title = "Item 2"),
            listingWithImage.copy(title = "Item 3"))

    composeTestRule.setContent {
      MultiListingCarouselCard(listings = listings, onListingClick = {}, onClose = {})
    }
    composeTestRule.onNodeWithText("Item 1").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Next").performClick()
    composeTestRule.onNodeWithText("Item 2").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Next").performClick()
    composeTestRule.onNodeWithText("Item 3").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Next").performClick()
    composeTestRule.onNodeWithText("Item 1").assertIsDisplayed()
  }

  @Test
  fun carousel_cycles_previous_correctly() {
    val listings =
        listOf(
            listingWithImage.copy(title = "Item 1"),
            listingWithImage.copy(title = "Item 2"),
            listingWithImage.copy(title = "Item 3"))

    composeTestRule.setContent {
      MultiListingCarouselCard(listings = listings, onListingClick = {}, onClose = {})
    }
    composeTestRule.onNodeWithText("Item 1").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Previous").performClick()
    composeTestRule.onNodeWithText("Item 3").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Previous").performClick()
    composeTestRule.onNodeWithText("Item 2").assertIsDisplayed()
  }

  @Test
  fun carousel_passes_click_events() {
    val listings = listOf(listingWithImage)
    var clickedId = ""

    composeTestRule.setContent {
      MultiListingCarouselCard(
          listings = listings, onListingClick = { id -> clickedId = id }, onClose = {})
    }

    composeTestRule.onNodeWithText("Listing With Image").performClick()
    assert(clickedId == "uid1")
  }
}
