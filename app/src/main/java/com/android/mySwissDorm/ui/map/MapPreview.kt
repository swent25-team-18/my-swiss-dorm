package com.android.mySwissDorm.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.Transparent
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * A reusable, non-interactive map preview composable.
 *
 * @param location The Location object containing latitude and longitude.
 * @param title The title for the map marker.
 * @param modifier Modifier to be applied to the map.
 * @param onMapClick Lambda to be invoked when the map preview is clicked.
 */
@Composable
fun MapPreview(
    location: Location,
    title: String,
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit
) {
  val latLng = remember { LatLng(location.latitude, location.longitude) }
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(latLng, 13f)
  }
  Box(
      modifier =
          modifier.clip(RoundedCornerShape(Dimens.CardCornerRadius)).background(TextBoxColor),
      contentAlignment = Alignment.Center) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings =
                MapUiSettings(
                    zoomControlsEnabled = false,
                    zoomGesturesEnabled = false,
                    scrollGesturesEnabled = false,
                    tiltGesturesEnabled = false,
                    mapToolbarEnabled = false)) {
              Marker(state = MarkerState(position = latLng), title = title)
            }
        Box(
            modifier =
                Modifier.matchParentSize().background(Transparent).clickable(onClick = onMapClick))
      }
}
