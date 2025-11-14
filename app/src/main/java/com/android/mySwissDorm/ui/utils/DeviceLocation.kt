package com.android.mySwissDorm.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

// This file was implemented using AI
/**
 * Fetches the device's current location using the FusedLocationProviderClient.
 *
 * @param context The current Android context.
 * @param fusedLocationClient The client to use for fetching location.
 * @param onLocationFetched A callback that provides the (latitude, longitude) on success.
 * @param onPermissionDenied A callback for when location access is denied or fails.
 */
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
