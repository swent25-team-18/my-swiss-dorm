package com.android.mySwissDorm.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.core.content.ContextCompat
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.Gray
import com.android.mySwissDorm.ui.theme.LightGray
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import java.util.UUID

/**
 * A button that open the camera and allow the user to take a photo. If the photo is successfully
 * taken, the `onImageTaken` is executed. Note that the resulted [Photo] is stored temporarily on
 * cache, it should be deleted if not needed anymore.
 *
 * @param onSave called when an image is successfully taken. Not called if the process is cancelled
 *   by the user.
 * @param modifier the [Modifier] applied to this button
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
    takePictureContract: ActivityResultContract<Uri, Boolean> =
        ActivityResultContracts.TakePicture(),
    content: @Composable (RowScope.() -> Unit) = {}
) {
  val context = LocalContext.current
  var uriCaptured by remember { mutableStateOf<Uri>(Uri.EMPTY) }
  var photoCaptured by remember { mutableStateOf<Photo?>(null) }

  val cameraLauncher =
      rememberLauncherForActivityResult(takePictureContract) {
        if (it && photoCaptured != null && uriCaptured.path?.isNotEmpty() ?: false) {
          onSave(photoCaptured!!)
        }
      }
  val permissionLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
          cameraLauncher.launch(photoCaptured?.image ?: return@rememberLauncherForActivityResult)
        } else {
          Toast.makeText(context, R.string.camera_button_permission_denied_text, Toast.LENGTH_SHORT)
              .show()
        }
      }
  Button(
      onClick = {
        val photo = Photo.createCapturablePhoto(context, UUID.randomUUID().toString() + ".jpg")
        uriCaptured = photo.image
        photoCaptured = photo
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED) {
          permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
          cameraLauncher.launch(input = photo.image)
        }
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
            contentColor = MainColor,
            disabledContentColor = MainColor,
            disabledContainerColor = Gray),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    takePictureContract: ActivityResultContract<Uri, Boolean> =
        ActivityResultContracts.TakePicture(),
) {
  CameraButton(
      onSave = onSave,
      modifier = modifier,
      takePictureContract = takePictureContract,
      enabled = enabled,
      shape = shape,
      colors = colors,
      elevation = elevation,
      border = border,
      contentPadding = contentPadding,
      interactionSource = interactionSource) {
        Icon(Icons.Default.AddAPhoto, null, tint = MainColor)
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.camera_button_default_text))
      }
}

@Preview
@Composable
private fun Preview() {
  MySwissDormAppTheme { DefaultCameraButton(onSave = { Log.d("CameraButton", "Photo taken") }) }
}
