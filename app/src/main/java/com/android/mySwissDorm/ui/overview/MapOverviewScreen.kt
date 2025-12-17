package com.android.mySwissDorm.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.ui.map.MapScreenScaffold
import com.android.mySwissDorm.ui.map.MultiListingCarouselCard
import com.android.mySwissDorm.ui.map.SmallListingPreviewCard
import com.android.mySwissDorm.ui.map.launchGoogleMaps
import com.android.mySwissDorm.ui.theme.LightBlue
import com.android.mySwissDorm.ui.theme.White
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

/**
 * The primary Map View screen for browsing dormitory listings.
 *
 * This screen displays a Google Map with markers representing available listings. It handles the
 * following features:
 * 1. **Clustering/Grouping:** Listings at the exact same coordinates are grouped together into a
 *    single marker.
 * 2. **Dynamic Overlays:** Clicking a marker opens a floating preview card. If the marker
 *    represents multiple listings, a carousel (MultiListingCarouselCard) is shown; otherwise, a
 *    single card (SmallListingPreviewCard) is shown.
 * 3. **Smart Camera Positioning:** Automatically centers on a specific location (if provided), the
 *    first listing, or a default fallback (Switzerland center).
 *
 * @param listings The complete list of ListingCardUI objects to display on the map.
 * @param centerLocation An optional specific location to center the map on initially (e.g., if the
 *   user navigated from a specific city search).
 * @param onGoBack Callback to navigate back to the previous screen.
 * @param onListingClick Callback triggered when a user clicks the preview card to view full details
 *   of a specific listing.
 */
@Composable
fun MapOverviewScreen(
    listings: List<ListingCardUI>,
    centerLocation: Location? = null,
    onGoBack: () -> Unit,
    onListingClick: (String) -> Unit
) {
  val context = LocalContext.current
  val groupedListings =
      remember(listings) { listings.groupBy { it.location.latitude to it.location.longitude } }
  var selectedListingsGroup by remember { mutableStateOf<List<ListingCardUI>?>(null) }

  val startLat =
      centerLocation?.latitude
          ?: if (listings.isNotEmpty()) listings[0].location.latitude else 46.8182
  val startLng =
      centerLocation?.longitude
          ?: if (listings.isNotEmpty()) listings[0].location.longitude else 8.2275

  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(LatLng(startLat, startLng), 13f)
  }

  MapScreenScaffold(
      title = stringResource(R.string.map_view),
      onGoBack = onGoBack,
      cameraPositionState = cameraPositionState,
      googleMapUiSettings =
          MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true),
      onMapClick = { selectedListingsGroup = null },
      onFabClick = {
        val target = cameraPositionState.position.target
        launchGoogleMaps(
            context,
            "geo:${target.latitude},${target.longitude}?z=${cameraPositionState.position.zoom}")
      },
      content = {
        if (centerLocation != null) {
          MarkerComposable(
              keys = arrayOf("centerLoc"),
              state =
                  MarkerState(
                      position = LatLng(centerLocation.latitude, centerLocation.longitude))) {
                Box(
                    modifier =
                        Modifier.size(24.dp)
                            .clip(CircleShape)
                            .background(White)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(LightBlue))
              }
        }

        groupedListings.forEach { (locationPair, listingsInGroup) ->
          if (locationPair.first != 0.0 && locationPair.second != 0.0) {
            val position = LatLng(locationPair.first, locationPair.second)
            Marker(
                state = MarkerState(position = position),
                title =
                    if (listingsInGroup.size == 1) "1 Listing"
                    else "${listingsInGroup.size} Listings",
                onClick = {
                  selectedListingsGroup = listingsInGroup
                  true
                })
          }
        }
      },
      overlayContent = {
        if (selectedListingsGroup != null) {
          val group = selectedListingsGroup!!
          Box(modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp).width(260.dp)) {
            if (group.size == 1) {
              SmallListingPreviewCard(
                  listing = group.first(),
                  onClick = { onListingClick(group.first().listingUid) },
                  onClose = { selectedListingsGroup = null })
            } else {
              MultiListingCarouselCard(
                  listings = group,
                  onListingClick = onListingClick,
                  onClose = { selectedListingsGroup = null })
            }
          }
        }
      })
}
