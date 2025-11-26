package com.android.mySwissDorm.ui.photo

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullScreenImageViewerTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val uri1 = "android.resource://com.android.mySwissDorm/${R.drawable.geneve}".toUri()
  private val uri2 = "android.resource://com.android.mySwissDorm/${R.drawable.zurich}".toUri()
  private val uri3 = "android.resource://com.android.mySwissDorm/${R.drawable.fribourg}".toUri()

  val uriList = listOf(uri1, uri2, uri3)

  @Test
  fun everythingIsDisplayed() {
    composeTestRule.setContent {
      FullScreenImageViewer(
          imageUris = listOf(uri1, uri2),
          onDismiss = {},
          initialIndex = 0,
      )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uri1)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.DELETE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.LEFT_ARROW_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.RIGHT_ARROW_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun navigationWorks() {
    composeTestRule.setContent {
      FullScreenImageViewer(
          imageUris = listOf(uri1, uri2),
          onDismiss = {},
          initialIndex = 0,
      )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uri1)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uri2)).assertIsNotDisplayed()
    // Go to the second image with the right arrow
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.RIGHT_ARROW_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uri1)).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uri2)).assertIsDisplayed()
    // Go to the previous image with the left arrow
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.RIGHT_ARROW_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uri1)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uri2)).assertIsNotDisplayed()
    // Go to the second image with the left arrow (it should loop)
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.RIGHT_ARROW_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uri1)).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uri2)).assertIsDisplayed()
  }

  @Test
  fun initialIndexWorks() {
    composeTestRule.setContent {
      FullScreenImageViewer(
          imageUris = listOf(uri1, uri2),
          onDismiss = {},
          initialIndex = 1,
      )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uri1)).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uri2)).assertIsDisplayed()
  }

  @Test(expected = IllegalArgumentException::class)
  fun negativeIndexThrows() {
    composeTestRule.setContent {
      FullScreenImageViewer(imageUris = listOf(uri1), onDismiss = {}, initialIndex = -1)
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun tooBigIndexThrows() {
    composeTestRule.setContent {
      FullScreenImageViewer(imageUris = listOf(uri1), onDismiss = {}, initialIndex = 4)
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun emptyListUrisThrows() {
    composeTestRule.setContent { FullScreenImageViewer(imageUris = emptyList(), onDismiss = {}) }
  }

  @Test
  fun onDismissIsCorrectlyExecuted() {
    val clicked = mutableStateOf(false)
    composeTestRule.setContent {
      FullScreenImageViewer(imageUris = uriList, onDismiss = { clicked.value = true })
    }
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.DELETE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    assertTrue(clicked.value)
  }

  @Test
  fun clickOnTheImageMakeTheControlsDisappear() {
    composeTestRule.setContent { FullScreenImageViewer(imageUris = uriList, onDismiss = {}) }
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.DELETE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.LEFT_ARROW_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.RIGHT_ARROW_BUTTON)
        .assertIsDisplayed()
    // Make them disappear
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uriList.first()))
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.DELETE_BUTTON).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.LEFT_ARROW_BUTTON)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.RIGHT_ARROW_BUTTON)
        .assertIsNotDisplayed()
  }

  @Test
  fun oneImageDoesNotShowArrows() {
    composeTestRule.setContent { FullScreenImageViewer(imageUris = listOf(uri1), onDismiss = {}) }
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.imageTag(uri1)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.FullScreenImageViewerTags.DELETE_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.LEFT_ARROW_BUTTON)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(C.FullScreenImageViewerTags.RIGHT_ARROW_BUTTON)
        .assertIsNotDisplayed()
  }
}
