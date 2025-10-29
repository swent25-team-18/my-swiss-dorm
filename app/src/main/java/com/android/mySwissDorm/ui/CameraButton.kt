package com.android.mySwissDorm.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.resources.C
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
 * @param context the [Context] with which the image is created
 */
@Composable
fun CameraButton(
    onSave: (Photo) -> Unit,
    modifier: Modifier = Modifier,
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
      modifier = modifier.testTag(tag = C.CameraButtonTag.TAG)) {
        content()
      }
}

@Preview
@Composable
private fun Preview() {
  MySwissDormAppTheme { CameraButton({ Log.d("CameraButton", "Photo taken") }) }
}
