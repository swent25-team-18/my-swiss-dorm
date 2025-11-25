package com.android.mySwissDorm.ui.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.mySwissDorm.R
import com.android.mySwissDorm.ui.overview.ListingCardUI
import com.android.mySwissDorm.ui.theme.*
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings

/**
 * A shared Scaffold for Map screens. Handles the TopBar, the FAB, the Map, and OVERLAYS (like
 * cards).
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

/** Helper to launch Google Maps Intent */
fun launchGoogleMaps(context: Context, uriString: String) {
  val gmmIntentUri = Uri.parse(uriString)
  val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
  mapIntent.setPackage("com.google.android.apps.maps")
  if (mapIntent.resolveActivity(context.packageManager) != null) {
    context.startActivity(mapIntent)
  } else {
    context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
  }
}

// --- SHARED CARDS ---

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
        modifier = Modifier.padding(bottom = 8.dp))

    Row(
        modifier =
            Modifier.fillMaxWidth().background(White, RoundedCornerShape(12.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          IconButton(
              onClick = {
                if (currentIndex > 0) currentIndex-- else currentIndex = listings.lastIndex
              },
              modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBackIos,
                    "Previous",
                    Modifier.size(16.dp),
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
              modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    "Next",
                    Modifier.size(16.dp),
                    tint = MainColor)
              }
        }
  }
}

@Composable
fun SmallListingPreviewCard(
    listing: ListingCardUI,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
  Card(
      modifier = modifier.clickable { onClick() },
      shape = RoundedCornerShape(16.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
      colors = CardDefaults.cardColors(containerColor = White)) {
        Box {
          Column(modifier = Modifier.fillMaxWidth()) {
            if (listing.image != null) {
              AsyncImage(
                  model = listing.image,
                  contentDescription = null,
                  modifier = Modifier.fillMaxWidth().height(120.dp),
                  contentScale = ContentScale.Crop)
            } else {
              Box(
                  modifier = Modifier.fillMaxWidth().height(120.dp).background(LightGray),
                  contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.No_image),
                        color = Gray,
                        style = MaterialTheme.typography.bodySmall)
                  }
            }

            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                  text = listing.title,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                  text = listing.leftBullets.getOrNull(1) ?: "",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MainColor,
                  fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                  text = listing.location.name,
                  style = MaterialTheme.typography.bodySmall,
                  color = Gray,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)
            }
          }

          IconButton(
              onClick = onClose,
              modifier =
                  Modifier.align(Alignment.TopEnd)
                      .padding(4.dp)
                      .size(24.dp)
                      .background(Dark.copy(alpha = 0.5f), RoundedCornerShape(12.dp))) {
                Icon(Icons.Default.Close, "Close", tint = White, modifier = Modifier.padding(4.dp))
              }
        }
      }
}
