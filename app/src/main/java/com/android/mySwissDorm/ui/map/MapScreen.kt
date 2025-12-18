package com.android.mySwissDorm.ui.map

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.android.mySwissDorm.R
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.utils.NetworkUtils
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
  // Reactively observe network state changes
  val isNetworkAvailable by
      NetworkUtils.networkStateFlow(context)
          .collectAsState(initial = NetworkUtils.isNetworkAvailable(context))
  val isOffline = !isNetworkAvailable

  val location = LatLng(latitude, longitude)
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(location, 15f)
  }

  MapScreenScaffold(
      title = stringResource(nameId),
      onGoBack = onGoBack,
      cameraPositionState = cameraPositionState,
      googleMapUiSettings =
          if (isOffline) MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
          else MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true),
      onFabClick = {
        if (!isOffline) {
          launchGoogleMaps(context, "google.navigation:q=$latitude,$longitude")
        }
      },
      content = {
        if (!isOffline) {
          Marker(state = MarkerState(position = location), title = title)
        }
      },
      overlayContent = {
        if (isOffline) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.maps_not_available_offline),
                style = MaterialTheme.typography.bodyLarge,
                color = TextColor.copy(alpha = Dimens.AlphaHigh),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(Dimens.PaddingDefault))
          }
        }
      })
}
