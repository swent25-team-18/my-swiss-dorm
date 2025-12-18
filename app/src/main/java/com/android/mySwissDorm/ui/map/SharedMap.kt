package com.android.mySwissDorm.ui.map

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.resources.C.SharedMapTags.PREVIOUS_IMAGE
import com.android.mySwissDorm.ui.overview.ListingCardUI
import com.android.mySwissDorm.ui.theme.*
import com.android.mySwissDorm.ui.theme.Dimens
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings

/**
 * A shared Scaffold specialized for Map screens within the application.
 *
 * This component standardizes the layout for any screen that features a full-screen Google Map. It
 * manages:
 * 1. The Top Bar (Title and Back navigation).
 * 2. The Floating Action Button (FAB) for external map navigation.
 * 3. The Google Map instance itself.
 * 4. An overlay layer for drawing UI elements (like Listing Cards) on top of the map.
 *
 * @param title The text to display in the TopAppBar.
 * @param onGoBack Callback triggered when the back arrow is clicked.
 * @param onFabClick Callback triggered when the FAB is clicked.
 * @param cameraPositionState State object controlling the map's camera (zoom, location).
 * @param googleMapUiSettings Settings for map UI controls (compass, zoom controls, etc.).
 * @param onMapClick Callback triggered when the user taps somewhere on the map surface.
 * @param content The scope for map specific content (Markers, Polylines, etc.).
 * @param overlayContent The scope for UI elements floating *above* the map (e.g., Preview Cards).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenScaffold(
    title: String,
    onGoBack: () -> Unit,
    onFabClick: () -> Unit,
    cameraPositionState: CameraPositionState,
    googleMapUiSettings: MapUiSettings = MapUiSettings(),
    onMapClick: (LatLng) -> Unit = {},
    content: @Composable () -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit = {}
) {
  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(title) },
            navigationIcon = {
              IconButton(onClick = onGoBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MainColor)
              }
            })
      },
      floatingActionButtonPosition = FabPosition.Start,
      floatingActionButton = {
        FloatingActionButton(onClick = onFabClick, containerColor = MainColor) {
          Icon(Icons.Filled.Navigation, "Open in Maps", tint = White)
        }
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          GoogleMap(
              modifier = Modifier.fillMaxSize(),
              cameraPositionState = cameraPositionState,
              uiSettings = googleMapUiSettings,
              onMapClick = { onMapClick(it) }) {
                content()
              }
          overlayContent()
        }
      }
}

/**
 * Helper function to launch an external Intent to Google Maps.
 *
 * This function attempts to open the given URI specifically in the Google Maps app package. If the
 * app is not installed, it falls back to a generic ACTION_VIEW intent (which may open a browser).
 *
 * @param context The Android Context required to start the activity.
 * @param uriString The URI string (e.g., "geo:0,0?q=...") to be opened.
 */
fun launchGoogleMaps(context: Context, uriString: String) {
  val gmmIntentUri = uriString.toUri()
  val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
  mapIntent.setPackage("com.google.android.apps.maps")
  if (mapIntent.resolveActivity(context.packageManager) != null) {
    context.startActivity(mapIntent)
  } else {
    context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
  }
}

// --- SHARED CARDS ---
/**
 * A composite card component that displays a carousel of [SmallListingPreviewCard]s.
 *
 * This is used when multiple listings exist at the exact same location (or very close proximity).
 * It provides "Previous" and "Next" buttons to cycle through the provided list of listings.
 *
 * @param listings The list of [ListingCardUI] data objects to display.
 * @param onListingClick Callback triggered when the listing preview card is tapped. Passes the
 *   Listing UID.
 * @param onClose Callback triggered when the close (X) button is tapped.
 */
@Composable
fun MultiListingCarouselCard(
    listings: List<ListingCardUI>,
    onListingClick: (String) -> Unit,
    onClose: () -> Unit
) {
  var currentIndex by remember { mutableIntStateOf(0) }
  val currentListing = listings[currentIndex]

  Column {
    SmallListingPreviewCard(
        listing = currentListing,
        onClick = { onListingClick(currentListing.listingUid) },
        onClose = onClose,
        modifier = Modifier.padding(bottom = Dimens.PaddingSmall))

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(White, RoundedCornerShape(Dimens.CornerRadiusDefault))
                .padding(Dimens.PaddingXSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          IconButton(
              onClick = {
                if (currentIndex > 0) currentIndex-- else currentIndex = listings.lastIndex
              },
              modifier = Modifier.size(Dimens.IconSizeXXXLarge)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBackIos,
                    "Previous",
                    Modifier.size(Dimens.IconSizeMedium),
                    tint = MainColor)
              }

          Text(
              text = "${currentIndex + 1} ${stringResource(R.string.of)} ${listings.size}",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MainColor)

          IconButton(
              onClick = {
                if (currentIndex < listings.lastIndex) currentIndex++ else currentIndex = 0
              },
              modifier = Modifier.size(Dimens.IconSizeXXXLarge)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    "Next",
                    Modifier.size(Dimens.IconSizeMedium),
                    tint = MainColor)
              }
        }
  }
}

/**
 * A compact preview card representing a single dormitory listing.
 *
 * This card is designed to overlay the map when a marker is clicked. It displays the primary image,
 * title, price (via bullets), and location name.
 *
 * It includes a custom close button implemented as a clickable Box to ensure precise sizing and
 * hit-target control.
 *
 * @param listing The data object containing listing details.
 * @param onClick Callback triggered when the card body is clicked (navigate to details).
 * @param onClose Callback triggered when the top-right X is clicked.
 * @param modifier Modifier to be applied to the root Card.
 */
@Composable
fun SmallListingPreviewCard(
    listing: ListingCardUI,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
  var currentImageIndex by remember(listing.listingUid) { mutableIntStateOf(0) }
  val imageCount = listing.image.size
  val safeIndex = if (imageCount > 0) currentImageIndex.coerceIn(0, imageCount - 1) else 0
  Card(
      modifier = modifier.clickable { onClick() },
      shape = RoundedCornerShape(Dimens.CardCornerRadius),
      elevation = CardDefaults.cardElevation(defaultElevation = Dimens.PaddingSmall),
      colors = CardDefaults.cardColors(containerColor = White)) {
        Box {
          Column(modifier = Modifier.fillMaxWidth()) {
            if (imageCount > 0) {
              Box(modifier = Modifier.fillMaxWidth().height(Dimens.ImageSizeXLarge)) {
                if (listing.image.isNotEmpty()) {
                  AsyncImage(
                      model = listing.image[safeIndex],
                      contentDescription = null,
                      modifier = Modifier.fillMaxSize(),
                      contentScale = ContentScale.Crop)
                }
                if (imageCount > 1) {
                  if (safeIndex > 0) {
                    IconButton(
                        onClick = { currentImageIndex-- },
                        modifier =
                            Modifier.align(Alignment.CenterStart)
                                .padding(Dimens.PaddingXSmall)
                                .background(Dark.copy(alpha = Dimens.AlphaMedium), CircleShape)
                                .size(Dimens.IconSizeXLarge)
                                .testTag(PREVIOUS_IMAGE)) {
                          Icon(
                              imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                              contentDescription = "Previous Image",
                              tint = White,
                              modifier = Modifier.size(Dimens.IconSizeSmall))
                        }
                  }
                  if (safeIndex < imageCount - 1) {
                    IconButton(
                        onClick = { currentImageIndex++ },
                        modifier =
                            Modifier.align(Alignment.CenterEnd)
                                .padding(Dimens.PaddingXSmall)
                                .background(Dark.copy(alpha = Dimens.AlphaMedium), CircleShape)
                                .size(Dimens.IconSizeXLarge)
                                .testTag(C.SharedMapTags.NEXT_IMAGE)) {
                          Icon(
                              imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                              contentDescription = "Next Image",
                              tint = White,
                              modifier = Modifier.size(Dimens.IconSizeSmall))
                        }
                  }
                  Text(
                      text = "${safeIndex + 1}/$imageCount",
                      style = MaterialTheme.typography.labelSmall,
                      color = MainColor,
                      modifier =
                          Modifier.align(Alignment.BottomEnd)
                              .padding(Dimens.PaddingSmall)
                              .background(
                                  Dark.copy(alpha = Dimens.AlphaSecondary),
                                  RoundedCornerShape(Dimens.CornerRadiusSmall))
                              .padding(horizontal = Dimens.PaddingXSmall, vertical = 2.dp)
                              .testTag(C.SharedMapTags.IMAGE_COUNTER))
                }
              }
            }
            Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
              Text(
                  text = listing.title,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  color = Black)
              Spacer(modifier = Modifier.height(Dimens.SpacingXSmall))
              Text(
                  text = listing.leftBullets.getOrNull(1) ?: "",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MainColor,
                  fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                  text = listing.location.name,
                  style = MaterialTheme.typography.bodySmall,
                  color = Gray,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)
            }
          }
          Box(
              modifier =
                  Modifier.align(Alignment.TopEnd)
                      .padding(Dimens.PaddingXSmall)
                      .background(
                          Dark.copy(alpha = Dimens.AlphaMedium),
                          RoundedCornerShape(Dimens.CornerRadiusMedium))
                      .clickable(onClick = onClose)
                      .padding(Dimens.PaddingXSmall)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = White,
                    modifier = Modifier.size(Dimens.IconSizeMedium))
              }
        }
      }
}
