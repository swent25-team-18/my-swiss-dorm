package com.android.mySwissDorm.ui.share

import android.graphics.Bitmap
import android.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

private fun generateQrCodeBitmap(content: String, size: Int): Bitmap? {
  return try {
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
      for (y in 0 until height) {
        bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
      }
    }
    bitmap
  } catch (_: Exception) {
    null
  }
}

/**
 * This composable represents the share dialog that appears when the share button is pressed. It
 * displays a QR code embedding a link to the corresponding listing/review, and a button to copy
 * that link.
 */
// Implemented with the help of AI

@Composable
fun ShareLinkDialog(link: String, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  val qrBitmap =
      remember(link) {
        // 512px QR code is usually sharp enough for most screens
        generateQrCodeBitmap(link, size = 512)
      }
  Dialog(onDismissRequest = onDismiss) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.wrapContentWidth()) {
      Surface(
          shape = RoundedCornerShape(Dimens.IconSizeLarge),
          tonalElevation = Dimens.PaddingXSmall,
          color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = Dimens.IconSizeDefault, vertical = Dimens.SpacingXLarge),
                horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                      text = stringResource(R.string.share_dialog_title),
                      style =
                          MaterialTheme.typography.titleLarge.copy(
                              color = TextColor, fontWeight = FontWeight.SemiBold),
                      modifier = Modifier.testTag(C.ShareLinkDialogTags.DIALOG_TITLE))

                  Spacer(modifier = Modifier.height(Dimens.SpacingXSmall))

                  if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.share_dialog_qr_content_desc),
                        modifier =
                            Modifier.size(Dimens.DialogQRCodeSize)
                                .testTag(C.ShareLinkDialogTags.QR_CODE))
                  } else {
                    Text(
                        text = stringResource(R.string.share_dialog_qr_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextColor,
                        modifier = Modifier.testTag(C.ShareLinkDialogTags.QR_ERROR))
                  }

                  Spacer(modifier = Modifier.height(Dimens.SpacingXSmall))

                  Button(
                      onClick = {
                        clipboardManager.setText(AnnotatedString(link))
                        Toast.makeText(
                                context,
                                context.getString(R.string.share_dialog_link_copied),
                                Toast.LENGTH_SHORT)
                            .show()
                        onDismiss()
                      },
                      modifier =
                          Modifier.fillMaxWidth().testTag(C.ShareLinkDialogTags.COPY_LINK_BUTTON),
                      shape = RoundedCornerShape(Dimens.IconSizeLarge),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = MainColor,
                              contentColor = androidx.compose.ui.graphics.Color.White)) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconSizeSmall))
                        Spacer(modifier = Modifier.size(Dimens.SpacingDefault))
                        Text(
                            text = stringResource(R.string.share_dialog_copy_link),
                            style = MaterialTheme.typography.labelLarge)
                      }

                  Spacer(modifier = Modifier.height(Dimens.SpacingDefault))

                  Text(
                      text = stringResource(R.string.cancel),
                      style = MaterialTheme.typography.bodyMedium,
                      color = MainColor,
                      modifier =
                          Modifier.clickable { onDismiss() }
                              .testTag(C.ShareLinkDialogTags.CANCEL_TEXT))
                }
          }
    }
  }
}
