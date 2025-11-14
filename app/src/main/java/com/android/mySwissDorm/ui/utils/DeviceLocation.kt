package com.android.mySwissDorm.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Fetches the device's current location using the FusedLocationProviderClient.
 *
 * @param context The current Android context.
 * @param fusedLocationClient The client to use for fetching location.
 * @param onLocationFetched A callback that provides the (latitude, longitude) on success.
 * @param onPermissionDenied A callback for when location access is denied or fails.
 */
// This function was implemented using AI
fun fetchDeviceLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationFetched: (Double, Double) -> Unit,
    onPermissionDenied: () -> Unit
) {
  if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
      PackageManager.PERMISSION_GRANTED) {
    onPermissionDenied()
    return
  }
  val cancellationTokenSource = CancellationTokenSource()
  fusedLocationClient
      .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
      .addOnSuccessListener { location: android.location.Location? ->
        if (location != null) {
          onLocationFetched(location.latitude, location.longitude)
        } else {
          onPermissionDenied()
        }
      }
      .addOnFailureListener { onPermissionDenied() }
}
/**
 * A Composable utility that creates and remembers a click handler for fetching the user's current
 * location.
 *
 * This function is designed to be called once within a Composable screen. It encapsulates all the
 * logic for checking, requesting, and handling `ACCESS_FINE_LOCATION` permission.
 *
 * When the returned lambda is invoked:
 * 1. It checks if the location permission is already granted.
 * 2. If **granted**, it attempts to fetch the device's location.
 * 3. If **not granted**, it launches the system's runtime permission request dialog.
 *
 * Upon a successful location fetch, it calls [vm.fetchLocationName] with the coordinates. It also
 * handles permission denial or location fetch failures (e.g., GPS is off) by showing appropriate
 * [Toast] messages.
 *
 * @param context The current Android `Context`, used for location services, permission checks, and
 *   `Toast` messages.
 * @param vm The [BaseLocationSearchViewModel] instance that will receive the location coordinates
 *   via `fetchLocationName`.
 * @return A stable, remembered `() -> Unit` lambda function that should be used as the `onClick`
 *   handler for a "use current location" button.
 */
// Documentation was made using AI
@Composable
fun onUserLocationClickFunc(context: Context, vm: BaseLocationSearchViewModel): () -> Unit {
  val fusedLocationClient =
      remember(context) { LocationServices.getFusedLocationProviderClient(context) }

  var hasLocationPermission by remember {
    mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED)
  }

  val onFetchLocationName =
      remember<(Double, Double) -> Unit> { { lat, lon -> vm.fetchLocationName(lat, lon) } }

  val permissionLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { isGranted ->
            if (isGranted) {
              hasLocationPermission = true
              fetchDeviceLocation(
                  context = context,
                  fusedLocationClient = fusedLocationClient,
                  onLocationFetched = onFetchLocationName,
                  onPermissionDenied = {
                    Toast.makeText(
                            context, "Could not get location. Is GPS on?", Toast.LENGTH_SHORT)
                        .show()
                  })
            } else {
              Toast.makeText(context, "Permission denied. Cannot get location.", Toast.LENGTH_SHORT)
                  .show()
            }
          })

  val onUseCurrentLocationClick =
      remember<() -> Unit> {
        {
          if (hasLocationPermission) {
            fetchDeviceLocation(
                context = context,
                fusedLocationClient = fusedLocationClient,
                onLocationFetched = onFetchLocationName,
                onPermissionDenied = {
                  Toast.makeText(context, "Could not get location. Is GPS on?", Toast.LENGTH_SHORT)
                      .show()
                })
          } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
          }
        }
      }
  return onUseCurrentLocationClick
}
