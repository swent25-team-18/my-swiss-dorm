package com.android.mySwissDorm.ui.photo

import android.net.Uri
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C

/**
 * An image grid is a scrollable row which displays a list of images.
 *
 * @param imageUris the images uri to be displayed
 * @param isEditingMode indicate if an option to remove the image is available
 * @param onRemove the action on removing an image of the grid
 * @param modifier the modifier of the grid
 * @param imageSize the size of an image
 *
 * Note: onRemove should update the list of images given to the function in order the displays only
 * the non deleted images.
 */
@Composable
fun ImageGrid(
    imageUris: List<Uri>,
    isEditingMode: Boolean,
    onRemove: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    imageSize: Dp = 120.dp
) {
  LazyRow(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(imageUris) { uri ->
          Box(modifier = Modifier.size(imageSize)) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier =
                    Modifier.fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .testTag(C.ImageGridTags.imageTag(uri)),
                contentScale = ContentScale.Crop)
          }

          if (isEditingMode) {
            FloatingActionButton(
                onClick = { onRemove(uri) },
                modifier = Modifier.offset(x = 8.dp, y = (-8).dp).size(32.dp)) {
                  Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Remove image",
                      modifier = Modifier.size(20.dp).testTag(C.ImageGridTags.deleteButtonTag(uri)))
                }
          }
        }
      }
}

@Composable
@Preview
private fun ImageGridPreview() {
  val uri1 = "android.resource://com.android.mySwissDorm/${R.drawable.geneve}".toUri()
  val uri2 = "android.resource://com.android.mySwissDorm/${R.drawable.zurich}".toUri()
  val uri3 = "android.resource://com.android.mySwissDorm/${R.drawable.fribourg}".toUri()
  val list = remember { mutableStateListOf(uri1, uri2, uri3) }
  ImageGrid(imageUris = list, isEditingMode = true, { list.remove(it) })
}
