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

/**
 * A Composable screen that displays a Google Map centered on a specific location with a marker.
 *
 * This screen utilizes a [MapScreenScaffold] to provide a consistent UI structure (TopBar, FAB). It
 * automatically sets the camera position to the provided coordinates with a default zoom level of
 * 15f.
 *
 * **Key Features:**
 * - **Marker:** Displays a pin at the given [latitude] and [longitude].
 * - **Navigation:** The Floating Action Button (FAB) triggers an Intent to launch the external
 *   Google Maps app for navigation.
 * - **Controls:** Zoom controls and "My Location" button are enabled by default.
 *
 * @param latitude The latitude coordinate of the destination/marker.
 * @param longitude The longitude coordinate of the destination/marker.
 * @param title The text label to be displayed when the map marker is clicked.
 * @param onGoBack A callback function invoked when the back navigation button is pressed.
 * @param nameId The string resource ID (@StringRes) used for the screen's title in the TopAppBar.
 */

// Documentation made with AI
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
