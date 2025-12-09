package com.android.mySwissDorm.ui.map

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.resources.C.SharedMapTags.IMAGE_COUNTER
import com.android.mySwissDorm.resources.C.SharedMapTags.NEXT_IMAGE
import com.android.mySwissDorm.resources.C.SharedMapTags.PREVIOUS_IMAGE
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
          image = listOf(Uri.parse("http://fake.uri/image.jpg")),
          location = location)
  private val listingWithMultipleImages =
      ListingCardUI(
          title = "Listing Multiple",
          leftBullets = listOf("Studio", "1200.-"),
          rightBullets = listOf("Available"),
          listingUid = "uid2",
          image =
              listOf(
                  Uri.parse("http://fake.uri/image1.jpg"),
                  Uri.parse("http://fake.uri/image2.jpg"),
                  Uri.parse("http://fake.uri/image3.jpg")),
          location = location)

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

  @Test
  fun smallListingCard_singleImage_hidesNavigationArrows() {
    composeTestRule.setContent {
      SmallListingPreviewCard(listing = listingWithImage, onClick = {}, onClose = {})
    }
    composeTestRule.onNodeWithTag(NEXT_IMAGE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(PREVIOUS_IMAGE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(IMAGE_COUNTER).assertDoesNotExist()
  }

  @Test
  fun smallListingCard_multipleImages_navigatesCorrectly() {
    composeTestRule.setContent {
      SmallListingPreviewCard(listing = listingWithMultipleImages, onClick = {}, onClose = {})
    }
    composeTestRule.onNodeWithText("1/3").assertIsDisplayed()
    composeTestRule.onNodeWithTag(PREVIOUS_IMAGE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(NEXT_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NEXT_IMAGE).performClick()
    composeTestRule.onNodeWithText("2/3").assertIsDisplayed()
    composeTestRule.onNodeWithTag(PREVIOUS_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NEXT_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NEXT_IMAGE).performClick()
    composeTestRule.onNodeWithText("3/3").assertIsDisplayed()
    composeTestRule.onNodeWithTag(NEXT_IMAGE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(PREVIOUS_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PREVIOUS_IMAGE).performClick()
    composeTestRule.onNodeWithText("2/3").assertIsDisplayed()
  }
}
