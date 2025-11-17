package com.android.mySwissDorm.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import java.util.UUID

@Composable
fun GalleryButton(
    onSelect: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    choosePictureContract: ActivityResultContract<String, Uri?> =
        ActivityResultContracts.GetContent(),
    content: @Composable (RowScope.() -> Unit) = {}
) {
  val galleryLauncher =
      rememberLauncherForActivityResult(choosePictureContract) {
        it?.let { uri ->
          onSelect(
              Photo(
                  image = uri,
                  fileName = UUID.randomUUID().toString() + uri.path!!.substringAfterLast('.')))
        }
      }
  Button(
      onClick = { galleryLauncher.launch("image/*") },
      modifier = modifier.testTag(tag = C.GalleryButtonTag.SINGLE_TAG),
      enabled = enabled,
      shape = shape,
      colors = colors,
      elevation = elevation,
      border = border,
      contentPadding = contentPadding,
      interactionSource = interactionSource) {
        content()
      }
}

@Composable
fun GalleryButtonMultiplePick(
    onSelect: (List<Photo>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    choosePicturesContract:
        ActivityResultContract<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>> =
        ActivityResultContracts.PickMultipleVisualMedia(),
    content: @Composable (RowScope.() -> Unit) = {}
) {
  val galleryLauncher =
      rememberLauncherForActivityResult(choosePicturesContract) { uris ->
        if (uris.isNotEmpty()) {
          onSelect(
              uris.map {
                Photo(
                    image = it,
                    fileName = UUID.randomUUID().toString() + it.path!!.substringAfterLast('.'))
              })
        }
      }
  Button(
      onClick = {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
      },
      modifier = modifier.testTag(tag = C.GalleryButtonTag.MULTIPLE_TAG),
      enabled = enabled,
      shape = shape,
      colors = colors,
      elevation = elevation,
      border = border,
      contentPadding = contentPadding,
      interactionSource = interactionSource) {
        content()
      }
}

@Composable
fun DefaultGalleryButton(
    onSelect: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: ButtonColors =
        ButtonColors(
            containerColor = TextBoxColor,
            contentColor = MainColor,
            disabledContentColor = TextColor,
            disabledContainerColor = TextBoxColor),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    choosePictureContract: ActivityResultContract<String, Uri?> =
        ActivityResultContracts.GetContent(),
) {
  GalleryButton(
      onSelect = onSelect,
      modifier = modifier,
      enabled = enabled,
      shape = shape,
      colors = colors,
      elevation = elevation,
      border = border,
      contentPadding = contentPadding,
      interactionSource = interactionSource,
      choosePictureContract = choosePictureContract) {
        Icon(Icons.Default.Photo, null, tint = if (enabled) MainColor else TextColor)
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.gallery_button_default_text))
      }
}

@Composable
fun DefaultGalleryButtonMultiplePick(
    onSelect: (List<Photo>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: ButtonColors =
        ButtonColors(
            containerColor = TextBoxColor,
            contentColor = MainColor,
            disabledContentColor = TextColor,
            disabledContainerColor = TextBoxColor),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    choosePicturesContract:
        ActivityResultContract<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>> =
        ActivityResultContracts.PickMultipleVisualMedia(),
) {
  GalleryButtonMultiplePick(
      onSelect = onSelect,
      modifier = modifier,
      enabled = enabled,
      shape = shape,
      colors = colors,
      elevation = elevation,
      border = border,
      contentPadding = contentPadding,
      interactionSource = interactionSource,
      choosePicturesContract = choosePicturesContract) {
        Icon(Icons.Default.Photo, null, tint = if (enabled) MainColor else TextColor)
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.gallery_button_default_text))
      }
}

@Preview
@Composable
private fun Preview() {
  MySwissDormAppTheme { DefaultGalleryButton({ Log.d("GalleryButton", "Selected : ${it.image}") }) }
}
