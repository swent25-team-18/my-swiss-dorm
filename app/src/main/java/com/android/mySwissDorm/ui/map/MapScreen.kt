package com.android.mySwissDorm.ui.map

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapScreen(
    latitude: Double,
    longitude: Double,
    title: String,
    onGoBack: () -> Unit,
    @StringRes nameId: Int
) {
  val context = LocalContext.current
  val location = LatLng(latitude, longitude)
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(location, 15f)
  }

  MapScreenScaffold(
      title = stringResource(nameId),
      onGoBack = onGoBack,
      cameraPositionState = cameraPositionState,
      googleMapUiSettings =
          MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true),
      onFabClick = { launchGoogleMaps(context, "google.navigation:q=$latitude,$longitude") },
      content = { Marker(state = MarkerState(position = location), title = title) })
}
