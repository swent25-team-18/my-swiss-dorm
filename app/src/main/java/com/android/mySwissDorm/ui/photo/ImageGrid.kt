package com.android.mySwissDorm.ui.photo

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.MainColor

/**
 * An image grid is a scrollable row which displays a list of images.
 *
 * @param imageUris the images uri to be displayed
 * @param isEditingMode indicate if an option to remove the image is available
 * @param onRemove the action on removing an image of the grid
 * @param modifier the modifier of the grid
 * @param imageWidth the width of an image
 * @param imageHeight the height of an image
 *
 * Note: onRemove should update the list of images given to the function in order the displays only
 * the non deleted images.
 */
@Composable
fun ImageGrid(
    imageUris: Set<Uri>,
    isEditingMode: Boolean,
    onImageClick: (Uri) -> Unit = {},
    onRemove: (Uri) -> Unit = {},
    modifier: Modifier = Modifier,
    imageWidth: Dp = 120.dp,
    imageHeight: Dp = 120.dp,
) {
  LazyRow(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingDefault),
      contentPadding = PaddingValues(horizontal = Dimens.PaddingDefault)) {
        items(imageUris.toList()) { uri ->
          Box(
              modifier =
                  Modifier.size(width = imageWidth, height = imageHeight)
                      .testTag(C.ImageGridTags.imageTag(uri))) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier =
                        Modifier.fillMaxSize()
                            .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                            .clickable { onImageClick(uri) },
                    contentScale = ContentScale.Crop)
                if (isEditingMode) {
                  FloatingActionButton(
                      onClick = { onRemove(uri) },
                      modifier =
                          Modifier.offset(x = Dimens.PaddingSmall, y = -Dimens.PaddingSmall)
                              .size(Dimens.IconSizeXXXLarge)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = C.ImageGridTags.ICON_DELETE_CONTENT_DESC,
                            modifier =
                                Modifier.size(Dimens.IconSizeDefault)
                                    .testTag(C.ImageGridTags.deleteButtonTag(uri)),
                            tint = MainColor)
                      }
                }
              }
        }
      }
}

// @Composable
// @Preview
// private fun ImageGridPreview() {
//  val uri1 = "android.resource://com.android.mySwissDorm/${R.drawable.geneve}".toUri()
//  val uri2 = "android.resource://com.android.mySwissDorm/${R.drawable.zurich}".toUri()
//  val uri3 = "android.resource://com.android.mySwissDorm/${R.drawable.fribourg}".toUri()
//  val list = remember { mutableStateListOf(uri1, uri2, uri3) }
//  MySwissDormAppTheme {
//    ImageGrid(imageUris = list.toSet(), isEditingMode = true, { list.remove(it) })
//  }
// }
