package com.android.mySwissDorm.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.White

/**
 * A generic button that displays a dialog allowing the user to either take a new photo or choose a
 * photo from his gallery.
 *
 * @param onSelectPhoto Function called when a Photo is selected.
 * @param multiplePick Allows multiple photos to be picked from the gallery.
 */
@Composable
fun AddPhotoButton(
    onSelectPhoto: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    multiplePick: Boolean = false,
    content: @Composable (RowScope.() -> Unit)
) {

  var displayDialog by remember { mutableStateOf(false) }

  Button(
      onClick = { displayDialog = true },
      modifier = modifier.testTag(C.AddPhotoButtonTags.BUTTON),
      enabled = enabled,
      shape = shape,
      colors = colors,
      elevation = elevation,
      border = border,
      contentPadding = contentPadding,
      interactionSource = interactionSource,
      content = content)

  if (displayDialog) {
    AddPhotoDialog(
        onSelectPhoto = {
          displayDialog = false
          onSelectPhoto(it)
        },
        onDismissRequest = { displayDialog = false },
        multiplePick = multiplePick)
  }
}

/**
 * A dialog allowing the user to select a photo, either by taking a new photo or by choosing a photo
 * from his gallery.
 *
 * @param onSelectPhoto Function called when a Photo is selected.
 * @param onDismissRequest Function called when the dialog is dismissed (e.g. tapping outside or
 *   pressing back). This should be considered a cancel action, make sure to handle cleanup.
 * @param multiplePick whether the user can pick one or multiple images from gallery.
 */
@Composable
fun AddPhotoDialog(
    onSelectPhoto: (Photo) -> Unit,
    onDismissRequest: () -> Unit,
    multiplePick: Boolean = false
) {
  Dialog(onDismissRequest = onDismissRequest) {
    Card(
        modifier =
            Modifier.fillMaxWidth()
                .testTag(C.AddPhotoButtonTags.DIALOG)
                .clip(RoundedCornerShape(16.dp))
                .background(BackGroundColor),
        elevation = CardDefaults.cardElevation(8.dp),
    ) {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              CameraButton(
                  onSave = { onSelectPhoto(it) },
                  modifier = Modifier.fillMaxWidth(0.9f),
                  colors =
                      ButtonDefaults.filledTonalButtonColors(
                          containerColor = MainColor, contentColor = White),
                  shape = RoundedCornerShape(14.dp),
              ) {
                Text(
                    text = stringResource(R.string.add_photo_button_take_photo),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(C.AddPhotoButtonTags.CAMERA_BUTTON_TEXT))
              }
              if (multiplePick) {
                GalleryButtonMultiplePick(
                    onSelect = { it.forEach { photo -> onSelectPhoto(photo) } },
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MainColor, contentColor = White),
                    shape = RoundedCornerShape(14.dp),
                ) {
                  GalleryText()
                }
              } else {
                GalleryButton(
                    onSelect = { onSelectPhoto(it) },
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MainColor, contentColor = White),
                    shape = RoundedCornerShape(14.dp),
                ) {
                  GalleryText()
                }
              }
            }
      }
    }
  }
}

@Composable
private fun GalleryText() {
  Text(
      text = stringResource(R.string.add_photo_button_from_gallery),
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.testTag(C.AddPhotoButtonTags.GALLERY_BUTTON_TEXT))
}

/**
 * A default implementation of the AddPhotoButton.
 *
 * @param onSelectPhoto Function called when a Photo is selected.
 */
@Composable
fun DefaultAddPhotoButton(onSelectPhoto: (Photo) -> Unit, multiplePick: Boolean = false) {
  AddPhotoButton(
      onSelectPhoto = onSelectPhoto,
      shape = RoundedCornerShape(14.dp),
      colors =
          ButtonColors(
              containerColor = TextBoxColor,
              contentColor = MainColor,
              disabledContentColor = TextBoxColor,
              disabledContainerColor = TextBoxColor),
      multiplePick = multiplePick) {
        Icon(Icons.Default.AddAPhoto, null, tint = MainColor)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.add_photo_button_default_text))
      }
}
