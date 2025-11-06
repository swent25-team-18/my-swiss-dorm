package com.android.mySwissDorm.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.utils.CustomLocationDialog

/**
 * The main screen for browsing listings and reviews in a specific location.
 *
 * This screen displays a tabbed interface with "Reviews" and "Listings" tabs. The top bar shows a
 * clickable location name with a pin icon that opens a dialog to change the location. When a new
 * location is selected, the [onLocationChange] callback is invoked to navigate to the new location.
 *
 * @param browseCityViewModel The ViewModel for managing the screen's state and data.
 * @param location The current location being browsed.
 * @param onGoBack A callback invoked when the back button is clicked.
 * @param onSelectListing A callback invoked when a listing card is clicked, passing the selected
 *   listing.
 * @param onLocationChange A callback invoked when the user selects a new location from the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseCityScreen(
    browseCityViewModel: BrowseCityViewModel = viewModel(),
    location: Location,
    onGoBack: () -> Unit = {},
    onSelectListing: (ListingCardUI) -> Unit = {},
    onLocationChange: (Location) -> Unit = {}
) {
  LaunchedEffect(location) { browseCityViewModel.loadListings(location) }

  val uiState by browseCityViewModel.uiState.collectAsState()

  val onLocationClick = remember { { browseCityViewModel.onCustomLocationClick() } }

  val onValueChange =
      remember<(String) -> Unit> { { query -> browseCityViewModel.setCustomLocationQuery(query) } }
  val onDropDownLocationSelect =
      remember<(Location) -> Unit> {
        { location -> browseCityViewModel.setCustomLocation(location) }
      }
  val onDismiss = remember<() -> Unit> { { browseCityViewModel.dismissCustomLocationDialog() } }
  val onConfirm =
      remember<(Location) -> Unit> {
        { newLocation ->
          onLocationChange(newLocation)
          browseCityViewModel.dismissCustomLocationDialog()
        }
      }

  BrowseCityScreenUI(
      location = location,
      listingsState = uiState.listings,
      onGoBack = onGoBack,
      onSelectListing = onSelectListing,
      onLocationClick = onLocationClick)

  if (uiState.showCustomLocationDialog) {
    CustomLocationDialog(
        value = uiState.customLocationQuery,
        currentLocation = uiState.customLocation,
        locationSuggestions = uiState.locationSuggestions,
        onValueChange = onValueChange,
        onDropDownLocationSelect = onDropDownLocationSelect,
        onDismiss = onDismiss,
        onConfirm = onConfirm)
  }
}

/**
 * Pure UI composable for the browse city screen (stateless).
 *
 * This is a stateless UI component that displays the browse city interface with tabs for Reviews
 * and Listings. The top bar shows a clickable location name that triggers the location change
 * dialog.
 *
 * @param location The current location being displayed.
 * @param listingsState The state of the listings (loading, items, error).
 * @param onGoBack A callback invoked when the back button is clicked.
 * @param onSelectListing A callback invoked when a listing card is clicked.
 * @param onLocationClick A callback invoked when the location title is clicked to open the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseCityScreenUI(
    location: Location,
    listingsState: ListingsState,
    onGoBack: () -> Unit,
    onSelectListing: (ListingCardUI) -> Unit,
    onLocationClick: () -> Unit
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 Reviews, 1 Listings

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              TextButton(onClick = onLocationClick) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Location",
                    tint = MainColor)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = location.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MainColor)
              }
            },
            navigationIcon = {
              IconButton(
                  onClick = onGoBack, modifier = Modifier.testTag(C.BrowseCityTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MainColor)
                  }
            })
      }) { pd ->
        Column(modifier = Modifier.padding(pd).fillMaxSize().testTag(C.BrowseCityTags.ROOT)) {
          TabRow(
              selectedTabIndex = selectedTab,
              containerColor = BackGroundColor,
              contentColor = TextColor,
              divider = { HorizontalDivider(thickness = 1.dp, color = TextColor) },
              indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                    height = 2.dp,
                    color = MainColor)
              }) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = TextColor,
                    unselectedContentColor = TextColor,
                    text = { Text("Reviews") },
                    modifier = Modifier.testTag(C.BrowseCityTags.TAB_REVIEWS))
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = TextColor,
                    unselectedContentColor = TextColor,
                    text = { Text("Listings") },
                    modifier = Modifier.testTag(C.BrowseCityTags.TAB_LISTINGS))
              }

          when (selectedTab) {
            0 -> {
              Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Not implemented yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray)
              }
            }
            else -> {
              when {
                listingsState.loading -> {
                  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.testTag(C.BrowseCityTags.LOADING))
                  }
                }
                listingsState.error != null -> {
                  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = listingsState.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(C.BrowseCityTags.ERROR))
                  }
                }
                listingsState.items.isEmpty() -> {
                  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No listings yet.", modifier = Modifier.testTag(C.BrowseCityTags.EMPTY))
                  }
                }
                else -> {
                  LazyColumn(
                      modifier =
                          Modifier.fillMaxSize()
                              .padding(horizontal = 16.dp)
                              .testTag(C.BrowseCityTags.LIST),
                      contentPadding = PaddingValues(vertical = 12.dp),
                      verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(listingsState.items) { item -> ListingCard(item, onSelectListing) }
                      }
                }
              }
            }
          }
        }
      }
}

/**
 * A card component that displays information about a rental listing.
 *
 * The card shows a placeholder image on the left, and the listing title with bullet points on the
 * right. The card is clickable and invokes [onClick] when tapped.
 *
 * @param data The listing data to display, including title, bullets, and listing UID.
 * @param onClick A callback invoked when the card is clicked, passing the listing data.
 */
@Composable
private fun ListingCard(data: ListingCardUI, onClick: (ListingCardUI) -> Unit) {
  OutlinedCard(
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().testTag(C.BrowseCityTags.card(data.listingUid)),
      onClick = { onClick(data) }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          // Image placeholder (left)
          Box(
              modifier =
                  Modifier.height(140.dp)
                      .fillMaxWidth(0.35F)
                      .clip(RoundedCornerShape(12.dp))
                      .background(Color(0xFFEAEAEA))) {
                Text(
                    "IMAGE",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray)
              }

          Spacer(Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = TextColor,
                modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
              BulletColumn(data.leftBullets, modifier = Modifier.weight(1f))
              Spacer(Modifier.width(8.dp))
              BulletColumn(data.rightBullets, modifier = Modifier.weight(1f))
            }
          }
        }
      }
}

/**
 * A column that displays a list of items as bullet points.
 *
 * Each item is displayed with a bullet point (•) prefix and proper spacing between items.
 *
 * @param items The list of strings to display as bullet points.
 * @param modifier Optional modifier to apply to the column.
 */
@Composable
private fun BulletColumn(items: List<String>, modifier: Modifier = Modifier) {
  Column(modifier) {
    items.forEach {
      Text("• $it", style = MaterialTheme.typography.bodyMedium, color = TextColor)
      Spacer(Modifier.height(6.dp))
    }
  }
}

/**
 * Preview composable for the BrowseCityScreen UI.
 *
 * Displays a preview of the browse city screen with sample listing data for design and testing
 * purposes.
 */
@Preview(showBackground = true, widthDp = 420)
@Composable
private fun BrowseCityScreen_Preview() {

  val sampleUi =
      ListingsState(
          loading = false,
          items =
              listOf(
                  ListingCardUI(
                      title = "Subletting my room",
                      leftBullets = listOf("Room in flatshare", "600.-/month", "19m²"),
                      rightBullets = listOf("Starting 15/09/2025", "Vortex"),
                      listingUid = "preview1"),
                  ListingCardUI(
                      title = "Bright studio near EPFL",
                      leftBullets = listOf("Studio", "1’150.-/month", "24m²"),
                      rightBullets = listOf("Starting 30/09/2025", "Private Accommodation"),
                      listingUid = "preview2")),
      )
  MySwissDormAppTheme {
    BrowseCityScreenUI(
        location = Location("Lausanne", 46.5197, 6.6323),
        listingsState = sampleUi,
        onGoBack = {},
        onSelectListing = {},
        onLocationClick = {})
  }
}
