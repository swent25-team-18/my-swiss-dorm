package com.android.mySwissDorm.ui.utils

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.resources.C.FilterTestTags.MAX_PRICE
import com.android.mySwissDorm.resources.C.FilterTestTags.MAX_SIZE
import com.android.mySwissDorm.resources.C.FilterTestTags.MIN_PRICE
import com.android.mySwissDorm.resources.C.FilterTestTags.MIN_SIZE
import com.android.mySwissDorm.resources.C.FilterTestTags.SLIDER_PRICE
import com.android.mySwissDorm.resources.C.FilterTestTags.SLIDER_SIZE
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilterUITest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun priceFilter_InitialStateNull_UsesDefaults() {
    composeTestRule.setContent {
      PriceFilterContent(priceRange = Pair(null, null), onRangeChange = { _, _ -> })
    }
    composeTestRule
        .onNodeWithTag("price_min_text")
        .assertIsDisplayed()
        .assertTextContains("Min: 0 CHF")

    composeTestRule
        .onNodeWithTag("price_max_text")
        .assertIsDisplayed()
        .assertTextContains("Max: 2000 CHF")
  }

  @Test
  fun priceFilter_InitialStateProvided_UsesProvidedValues() {
    composeTestRule.setContent {
      PriceFilterContent(priceRange = Pair(500.0, 1500.0), onRangeChange = { _, _ -> })
    }

    composeTestRule.onNodeWithTag(MIN_PRICE).assertTextContains("Min: 500 CHF")
    composeTestRule.onNodeWithTag(MAX_PRICE).assertTextContains("Max: 1500 CHF")
  }

  @Test
  fun priceFilter_SliderInteraction_UpdatesValuesAndCallback() {
    var capturedMin: Double? = null
    var capturedMax: Double? = null

    composeTestRule.setContent {
      PriceFilterContent(
          priceRange = Pair(0.0, 5000.0),
          onRangeChange = { min, max ->
            capturedMin = min
            capturedMax = max
          })
    }
    composeTestRule.onNodeWithTag(SLIDER_PRICE).performTouchInput { swipeRight() }
    composeTestRule.waitForIdle()
    assertTrue("Callback should be triggered on slider interaction", capturedMin != null)
    assertTrue("Callback should be triggered on slider interaction", capturedMax != null)
  }

  @Test
  fun sizeFilter_InitialStateNull_UsesDefaults() {
    composeTestRule.setContent {
      SizeFilterContent(sizeRange = Pair(null, null), onRangeChange = { _, _ -> })
    }
    composeTestRule.onNodeWithTag(MIN_SIZE).assertIsDisplayed().assertTextContains("Min: 0 m²")

    composeTestRule.onNodeWithTag(MAX_SIZE).assertIsDisplayed().assertTextContains("Max: 100 m²")
  }

  @Test
  fun sizeFilter_InitialStateProvided_UsesProvidedValues() {
    composeTestRule.setContent {
      SizeFilterContent(sizeRange = Pair(20, 80), onRangeChange = { _, _ -> })
    }
    composeTestRule.onNodeWithTag("size_min_text").assertTextContains("Min: 20 m²")
    composeTestRule.onNodeWithTag("size_max_text").assertTextContains("Max: 80 m²")
  }

  @Test
  fun sizeFilter_SliderInteraction_UpdatesValuesAndCallback() {
    var capturedMin: Int? = null
    var capturedMax: Int? = null

    composeTestRule.setContent {
      SizeFilterContent(
          sizeRange = Pair(0, 200),
          onRangeChange = { min, max ->
            capturedMin = min
            capturedMax = max
          })
    }
    composeTestRule.onNodeWithTag(SLIDER_SIZE).performTouchInput { swipeRight() }

    composeTestRule.waitForIdle()

    assertTrue("Callback should be triggered", capturedMin != null)
    assertTrue("Callback should be triggered", capturedMax != null)
  }
}
