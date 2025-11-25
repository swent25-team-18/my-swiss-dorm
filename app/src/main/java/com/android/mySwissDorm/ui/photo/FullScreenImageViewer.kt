package com.android.mySwissDorm.ui.photo

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.mySwissDorm.ui.theme.Red

@Composable
fun FullScreenImageViewer(
    imageUris: List<Uri>,
    onDismiss: () -> Unit,
    initialIndex: Int = 0,
) {
  var currentIndex by remember { mutableIntStateOf(initialIndex) }
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }
  var showControls by remember { mutableStateOf(true) }
  var isLoading by remember { mutableStateOf(false) }

  val transformState = rememberTransformableState { zoomChange, panChange, _ ->
    scale = (scale * zoomChange).coerceIn(1f, 5f)

    val newOffset = offset + panChange
    offset =
        if (scale > 1f) {
          newOffset
        } else {
          Offset.Zero
        }
  }

  // Reset zoom when navigating
  LaunchedEffect(currentIndex) {
    scale = 1f
    offset = Offset.Zero
  }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary)) {
    // Image
    AsyncImage(
        model = imageUris[currentIndex],
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier =
            Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { showControls = !showControls }) }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y)
                .transformable(state = transformState),
        onLoading = { isLoading = true },
        onSuccess = { isLoading = false },
        onError = { isLoading = false })
    // Loading circle
    if (isLoading) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Red)
      }
    }
    val buttonBackGroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    // Close control
    AnimatedVisibility(
        visible = showControls,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
          IconButton(
              onClick = onDismiss, modifier = Modifier.background(color = buttonBackGroundColor)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Red)
              }
        }
    // Navigation arrows
    AnimatedVisibility(
        visible = showControls && imageUris.size > 1, enter = fadeIn(), exit = fadeOut()) {
          Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                      if (currentIndex > 0) {
                        currentIndex--
                      } else {
                        currentIndex = imageUris.size - 1
                      }
                    },
                    modifier = Modifier.background(color = buttonBackGroundColor)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                          contentDescription = "Previous",
                          tint = Red)
                    }

                IconButton(
                    onClick = {
                      if (currentIndex < imageUris.size - 1) {
                        currentIndex++
                      } else {
                        currentIndex = 0
                      }
                    },
                    modifier = Modifier.background(color = buttonBackGroundColor)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                          contentDescription = "Next",
                          tint = Red)
                    }
              }
        }
  }
}

// @Preview
// @Composable
// fun FullScreenImageViewerPreview() {
//    val context = LocalContext.current
//    val uri = "android.resource://${context.packageName}/${R.drawable.zurich}".toUri()
//    MySwissDormAppTheme {
//        FullScreenImageViewer(
//            imageUris = listOf(uri,uri),
//            onDismiss = {},
//            initialIndex = 0
//        )
//    }
// }
