package com.android.mySwissDorm.ui.utils

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.Dimens
import kotlin.math.roundToInt

/**
 * A custom, dependency-free Composable for star ratings.
 *
 * This component displays a 5-star rating bar that supports both full and half-star ratings. Users
 * can tap anywhere on the bar to set a rating, and the component will calculate the appropriate
 * star value (0.5, 1.0, 1.5, ..., 5.0) based on the tap position.
 *
 * The rating is visually displayed using filled stars, half-filled stars, and outlined stars to
 * represent the current rating value. The minimum rating that can be set is 0.5.
 *
 * @param modifier Standard Compose [Modifier] for styling the rating bar.
 * @param rating The current rating value (0.0 to 5.0, in 0.5 increments).
 * @param onRatingChange Callback invoked when the user taps to change the rating.
 * @param activeColor The color for filled and half-filled stars.
 * @param inactiveColor The color for empty/outlined stars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarRatingBar(
    modifier: Modifier = Modifier,
    rating: Double,
    onRatingChange: (Double) -> Unit,
    activeColor: Color,
    inactiveColor: Color
) {
  var rowSize by remember { mutableStateOf(IntSize.Zero) }
  Row(
      modifier =
          modifier
              .width(Dimens.SpacerHeightSmall)
              .onSizeChanged { rowSize = it }
              .pointerInput(Unit) {
                detectTapGestures { offset ->
                  val widthInPx = rowSize.width.toFloat()
                  if (widthInPx <= 0) return@detectTapGestures
                  val xFraction = (offset.x / widthInPx).coerceIn(0f, 1f)
                  val rawRating = xFraction * 5
                  val newRating = (rawRating * 2).roundToInt() / 2.0
                  onRatingChange(newRating.coerceAtLeast(0.5))
                }
              }) {
        // For the rating stars
        for (i in 1..5) {
          val icon =
              when {
                i <= rating -> Icons.Filled.Star
                i - 0.5 <= rating -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Outlined.StarOutline
              }
          Icon(
              imageVector = icon,
              contentDescription = null,
              tint = if (i - 0.5 <= rating) activeColor else inactiveColor,
              modifier =
                  Modifier.weight(1f)
                      .height(Dimens.IconSizeXXLarge)
                      .testTag(C.StarRatingBarTags.getStarTag(i)))
        }
      }
}
