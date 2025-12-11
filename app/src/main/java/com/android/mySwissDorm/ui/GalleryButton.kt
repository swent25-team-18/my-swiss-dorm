package com.android.mySwissDorm.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import java.util.UUID

/**
 * This button takes a single picture from the gallery and if the selection is successful, the
 * [onSelect] is executed with the picture wrap in a [Photo]
 *
 * @param onSelect the lambda executed with the resulting photo only if the selection is successful
 *
 * The other parameters follow are just transitive to the wrapped [Button]
 */
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
    permissionContract: ActivityResultContract<String, Boolean> =
        ActivityResultContracts.RequestPermission(),
    content: @Composable (RowScope.() -> Unit) = {}
) {
  val context = LocalContext.current
  val permission =
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // Android 12 and below
        Manifest.permission.READ_EXTERNAL_STORAGE
      } else {
        // Android 13+
        Manifest.permission.READ_MEDIA_IMAGES
      }
  val galleryLauncher =
      rememberLauncherForActivityResult(choosePictureContract) {
        it?.let { uri ->
          onSelect(Photo(image = uri, fileName = getFileNameFromUri(context = context, uri = uri)))
        }
      }
  val permissionLauncher =
      rememberLauncherForActivityResult(permissionContract) { isGranted ->
        if (isGranted) {
          galleryLauncher.launch("image/*")
        } else {
          Toast.makeText(
                  context, R.string.gallery_button_permission_denied_text, Toast.LENGTH_SHORT)
              .show()
        }
      }
  Button(
      onClick = {
        if (ContextCompat.checkSelfPermission(context, permission) !=
            PackageManager.PERMISSION_GRANTED) {
          permissionLauncher.launch(permission)
        } else {
          galleryLauncher.launch("image/*")
        }
      },
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

/**
 * This button can take multiple pictures from the gallery and if the selection is not empty, the
 * [onSelect] lambda is executed with the resulting [Photo]s.
 *
 * @param onSelect the lambda executed when the resulting selection contains at least one element.
 *
 * The other parameters are transitive to the [Button] wrapped in this composable.
 */
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
    permissionContract: ActivityResultContract<String, Boolean> =
        ActivityResultContracts.RequestPermission(),
    content: @Composable (RowScope.() -> Unit) = {}
) {
  val context = LocalContext.current
  val permission =
      if (Build.VERSION.SDK_INT < 33) {
        Manifest.permission.READ_EXTERNAL_STORAGE
      } else {
        Manifest.permission.READ_MEDIA_IMAGES
      }
  val galleryLauncher =
      rememberLauncherForActivityResult(choosePicturesContract) { uris ->
        if (uris.isNotEmpty()) {
          onSelect(
              uris.map {
                Photo(image = it, fileName = getFileNameFromUri(context = context, uri = it))
              })
        }
      }
  val permissionLauncher =
      rememberLauncherForActivityResult(permissionContract) { isGranted ->
        if (isGranted) {
          galleryLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
          Toast.makeText(
                  context, R.string.gallery_button_permission_denied_text, Toast.LENGTH_SHORT)
              .show()
        }
      }
  Button(
      onClick = {
        if (ContextCompat.checkSelfPermission(context, permission) !=
            PackageManager.PERMISSION_GRANTED) {
          permissionLauncher.launch(permission)
        } else {
          galleryLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
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

/** Provides a default style of [GalleryButton] */
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
    permissionContract: ActivityResultContract<String, Boolean> =
        ActivityResultContracts.RequestPermission(),
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
      permissionContract = permissionContract,
      choosePictureContract = choosePictureContract) {
        Icon(Icons.Default.Photo, null, tint = if (enabled) MainColor else TextColor)
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.gallery_button_default_text))
      }
}

/** Provides a default style of [GalleryButtonMultiplePick] */
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
    permissionContract: ActivityResultContract<String, Boolean> =
        ActivityResultContracts.RequestPermission(),
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
      permissionContract = permissionContract,
      choosePicturesContract = choosePicturesContract) {
        Icon(Icons.Default.Photo, null, tint = if (enabled) MainColor else TextColor)
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.gallery_button_default_multiple_text))
      }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String {
  require(uri.scheme == "content")
  var fileName = ""
  val cursorQuery = context.contentResolver.query(uri, null, null, null)
  cursorQuery?.use { cursor ->
    if (cursor.moveToFirst()) {
      fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
    }
  } ?: throw IllegalArgumentException()
  return UUID.randomUUID().toString() + "." + fileName.substringAfterLast('.')
}

@Preview
@Composable
private fun Preview() {
  MySwissDormAppTheme { DefaultGalleryButton({ Log.d("GalleryButton", "Selected : ${it.image}") }) }
}
