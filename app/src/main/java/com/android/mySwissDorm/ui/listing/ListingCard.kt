package com.android.mySwissDorm.ui.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.resources.C.BrowseCityTags.RECOMMENDED
import com.android.mySwissDorm.ui.overview.ListingCardUI
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.ListingCardColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.White

/**
 * A card component that displays information about a rental listing.
 *
 * The card shows an image on the left, and the listing title with bullet points on the right. The
 * card is clickable and invokes [onClick] when tapped.
 *
 * @param data The listing data to display, including title, bullets, and listing UID.
 * @param onClick A callback invoked when the card is clicked, passing the listing data.
 * @param isBookmarked Whether the listing is currently bookmarked.
 * @param onToggleBookmark A callback invoked when the bookmark button is clicked.
 * @param isGuest Whether the current user is a guest (bookmark button hidden if true).
 */
@Composable
fun ListingCard(
    data: ListingCardUI,
    onClick: (ListingCardUI) -> Unit,
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    isGuest: Boolean = false
) {
  OutlinedCard(
      shape = RoundedCornerShape(Dimens.CardCornerRadius),
      modifier = Modifier.fillMaxWidth().testTag(C.BrowseCityTags.listingCard(data.listingUid)),
      onClick = { onClick(data) }) {
        Box(modifier = Modifier.fillMaxWidth()) {
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      // Fix the content height so all cards have the same height
                      .height(Dimens.CardImageHeight)
                      .padding(end = if (!isGuest) Dimens.IconSizeButton else 0.dp),
              verticalAlignment = Alignment.CenterVertically) {
                // Image (left) - fills the left side vertically within the fixed card height
                Box(
                    modifier =
                        Modifier.fillMaxHeight()
                            .weight(0.35f)
                            .clip(RoundedCornerShape(Dimens.CornerRadiusDefault))
                            .background(ListingCardColor)) {
                      AsyncImage(
                          model = data.image.firstOrNull(),
                          contentDescription = null,
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop)
                      if (data.isRecommended) {
                        Surface(
                            color = MainColor,
                            shape =
                                RoundedCornerShape(
                                    topStart = Dimens.CornerRadiusDefault,
                                    bottomEnd = Dimens.CornerRadiusMedium),
                            modifier = Modifier.align(Alignment.TopStart)) {
                              Text(
                                  text = stringResource(R.string.recommended),
                                  color = White,
                                  style = MaterialTheme.typography.labelSmall,
                                  modifier =
                                      Modifier.padding(
                                              horizontal = Dimens.PaddingMedium,
                                              vertical = Dimens.PaddingXSmall)
                                          .testTag(RECOMMENDED),
                                  fontSize = 10.sp)
                            }
                      }
                    }

                Spacer(Modifier.width(Dimens.SpacingLarge))

                Column(modifier = Modifier.weight(0.65f)) {
                  Text(
                      text = data.title,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.SemiBold,
                      textAlign = TextAlign.Center,
                      color = TextColor,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.fillMaxWidth())

                  Spacer(Modifier.height(Dimens.SpacingDefault))

                  Row(modifier = Modifier.fillMaxWidth()) {
                    BulletColumn(data.leftBullets, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(Dimens.SpacingDefault))
                    BulletColumn(data.rightBullets, modifier = Modifier.weight(1f))
                  }
                }
              }

          // Bookmark button (top-right corner, above title)
          if (!isGuest) {
            IconButton(
                onClick = onToggleBookmark,
                modifier =
                    Modifier.align(Alignment.TopEnd)
                        .padding(top = Dimens.PaddingXSmall, end = Dimens.PaddingXSmall)) {
                  Icon(
                      imageVector =
                          if (isBookmarked) Icons.Filled.Bookmark
                          else Icons.Outlined.BookmarkBorder,
                      contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                      tint = MainColor)
                }
          }
        }
      }
}

/**
 * A column that displays a list of items as bullet points.
 *
 * Each item is displayed with a bullet point (•) prefix and proper spacing between items.
 *
 * @param items The list of strings to display as bullet points.
 * @param modifier Optional modifier to apply to the column.
 */
@Composable
private fun BulletColumn(items: List<String>, modifier: Modifier = Modifier) {
  Column(modifier) {
    items.forEach {
      Text("• $it", style = MaterialTheme.typography.bodyMedium, color = TextColor)
      Spacer(Modifier.height(Dimens.SpacingMedium))
    }
  }
}
