package com.android.mySwissDorm.ui.map

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.mySwissDorm.ui.theme.MainColor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

// This file was implemented with some help of the bootcamp and AI and me

/**
 * A full-screen composable that displays an interactive Google Map.
 *
 * This screen is centered on the provided [latitude] and [longitude], displaying a single [Marker]
 * with the given [title]. It features a top app bar with a back button and a Floating Action Button
 * (FAB) that launches the Google Maps app for turn-by-turn navigation.
 *
 * @param latitude The latitude of the location to center the map on.
 * @param longitude The longitude of the location to center the map on.
 * @param title The title for the [Marker] placed at the location.
 * @param onGoBack A lambda function to be invoked when the back icon is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    latitude: Double,
    longitude: Double,
    title: String,
    onGoBack: () -> Unit,
    @StringRes nameId: Int
) {
  val context = LocalContext.current
  val location = remember { LatLng(latitude, longitude) }
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(location, 15f)
  }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(nameId)) },
            navigationIcon = {
              IconButton(onClick = onGoBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MainColor)
              }
            })
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = {
              val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude")
              val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
              mapIntent.setPackage("com.google.android.apps.maps")
              context.startActivity(mapIntent)
            },
            containerColor = MainColor) {
              Icon(Icons.Filled.Navigation, "Start navigation", tint = Color.White)
            }
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
            Marker(state = MarkerState(position = location), title = title)
          }
        }
      }
}
