package com.android.mySwissDorm.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.Gray
import com.android.mySwissDorm.ui.theme.LightGray
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.Red0
import java.util.UUID

/**
 * A button that open the camera and allow the user to take a photo. If the photo is successfully
 * taken, the `onImageTaken` is executed. Note that the resulted [Photo] is stored temporarily on
 * cache, it should be deleted if not needed anymore.
 *
 * @param onSave called when an image is successfully taken. Not called if the process is cancelled
 *   by the user.
 * @param modifier the [Modifier] applied to this button
 * @param context the [Context] with which the image is created
 */
@Composable
fun CameraButton(
    onSave: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    context: Context = LocalContext.current,
    takePictureContract: ActivityResultContract<Uri, Boolean> =
        ActivityResultContracts.TakePicture(),
    content: @Composable (RowScope.() -> Unit) = {}
) {
  var uriCaptured by remember { mutableStateOf<Uri>(Uri.EMPTY) }
  var photoCaptured by remember { mutableStateOf<Photo?>(null) }

  val cameraLauncher =
      rememberLauncherForActivityResult(takePictureContract) {
        if (it && photoCaptured != null && uriCaptured.path?.isNotEmpty() ?: false) {
          onSave(photoCaptured!!)
        }
      }
  Button(
      onClick = {
        val photo = Photo.createNewPhotoOnCache(context, UUID.randomUUID().toString())
        uriCaptured = photo.image
        photoCaptured = photo
        cameraLauncher.launch(input = photo.image)
      },
      modifier = modifier.testTag(tag = C.CameraButtonTag.TAG),
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
fun DefaultCameraButton(
    onSave: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: ButtonColors =
        ButtonColors(
            containerColor = LightGray,
            contentColor = Red0,
            disabledContentColor = Red0,
            disabledContainerColor = Gray),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    context: Context = LocalContext.current,
    takePictureContract: ActivityResultContract<Uri, Boolean> =
        ActivityResultContracts.TakePicture(),
) {
  CameraButton(
      onSave = onSave,
      modifier = modifier,
      context = context,
      takePictureContract = takePictureContract,
      enabled = enabled,
      shape = shape,
      colors = colors,
      elevation = elevation,
      border = border,
      contentPadding = contentPadding,
      interactionSource = interactionSource) {
        Icon(Icons.Default.AddAPhoto, null, tint = Red0)
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.camera_button_default_text))
      }
}

@Preview
@Composable
private fun Preview() {
  MySwissDormAppTheme { DefaultCameraButton(onSave = { Log.d("CameraButton", "Photo taken") }) }
}
