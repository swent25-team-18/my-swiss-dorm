package com.android.mySwissDorm.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.LightGray
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.Red0
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
        it?.let { uri -> onSelect(Photo(image = uri, uid = UUID.randomUUID().toString())) }
      }
  Button(
      onClick = { galleryLauncher.launch("image/*") },
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

@Preview
@Composable
private fun Preview() {
  MySwissDormAppTheme {
    GalleryButton(
        { Log.d("GalleryButton", "Selected : ${it.image}") },
        colors =
            ButtonColors(
                contentColor = Red0,
                containerColor = LightGray,
                disabledContentColor = Red0,
                disabledContainerColor = LightGray))
  }
}
