package com.android.mySwissDorm.ui.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.resources.C.FilterTestTags.MAX_PRICE
import com.android.mySwissDorm.resources.C.FilterTestTags.MAX_SIZE
import com.android.mySwissDorm.resources.C.FilterTestTags.MIN_PRICE
import com.android.mySwissDorm.resources.C.FilterTestTags.MIN_SIZE
import com.android.mySwissDorm.resources.C.FilterTestTags.SLIDER_PRICE
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor

/** Shared Price filter content with range slider. */
@Composable
fun PriceFilterContent(
    priceRange: Pair<Double?, Double?>,
    onRangeChange: (Double?, Double?) -> Unit
) {
  val defaultMin = 0.0f
  val defaultMax = 0.0f

  var minPrice by
      remember(priceRange) { mutableFloatStateOf((priceRange.first?.toFloat() ?: defaultMin)) }
  var maxPrice by
      remember(priceRange) { mutableFloatStateOf((priceRange.second?.toFloat() ?: defaultMax)) }
    LaunchedEffect(Unit) {
        if (priceRange.first == null || priceRange.second == null) {
            onRangeChange(minPrice.toDouble(), maxPrice.toDouble())
        }}
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(
          "${stringResource(R.string.min)}: ${minPrice.toInt()} CHF",
          color = TextColor,
          modifier = Modifier.testTag(MIN_PRICE))
      Text(
          "${stringResource(R.string.max)}: ${maxPrice.toInt()} CHF",
          color = TextColor,
          modifier = Modifier.testTag(MAX_PRICE))
    }
    RangeSlider(
        value = minPrice..maxPrice,
        onValueChange = { range ->
          minPrice = range.start
          maxPrice = range.endInclusive
          onRangeChange(minPrice.toDouble(), maxPrice.toDouble())
        },
        valueRange = 0f..5000f,
        steps = 49,
        modifier = Modifier.testTag(SLIDER_PRICE),
        colors =
            SliderDefaults.colors(
                thumbColor = MainColor,
                activeTrackColor = MainColor,
                inactiveTrackColor = MainColor.copy(alpha = 0.3f)))
  }
}

/** Shared Size filter content with range slider. */
@Composable
fun SizeFilterContent(sizeRange: Pair<Int?, Int?>, onRangeChange: (Int?, Int?) -> Unit) {
  val defaultMin = 0.0f
  val defaultMax = 0.0f
  var minSize by
      remember(sizeRange) { mutableFloatStateOf((sizeRange.first?.toFloat() ?: defaultMin)) }
  var maxSize by
      remember(sizeRange) { mutableFloatStateOf((sizeRange.second?.toFloat() ?: defaultMax)) }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(
          "${stringResource(R.string.min)}: ${minSize.toInt()} m²",
          color = TextColor,
          modifier = Modifier.testTag(MIN_SIZE))
      Text(
          "${stringResource(R.string.max)}: ${maxSize.toInt()} m²",
          color = TextColor,
          modifier = Modifier.testTag(MAX_SIZE))
    }
    RangeSlider(
        value = minSize..maxSize,
        onValueChange = { range ->
          minSize = range.start
          maxSize = range.endInclusive
          onRangeChange(minSize.toInt(), maxSize.toInt())
        },
        valueRange = 0f..200f,
        steps = 39,
        modifier = Modifier.testTag(C.FilterTestTags.SLIDER_SIZE),
        colors =
            SliderDefaults.colors(
                thumbColor = MainColor,
                activeTrackColor = MainColor,
                inactiveTrackColor = MainColor.copy(alpha = 0.3f)))
  }
}
