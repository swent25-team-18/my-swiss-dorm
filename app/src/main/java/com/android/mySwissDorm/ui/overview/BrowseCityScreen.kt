package com.android.mySwissDorm.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Place
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
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.navigation.BottomBarFromNav
import com.android.mySwissDorm.ui.navigation.NavigationActions
import com.android.mySwissDorm.ui.navigation.Screen
import com.android.mySwissDorm.ui.review.DisplayGrade
import com.android.mySwissDorm.ui.review.truncateText
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.utils.CustomLocationDialog
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatDate
import com.google.firebase.Timestamp

/**
 * The main screen for browsing listings and reviews in a specific location.
 *
 * This screen displays a tabbed interface with "Reviews" and "Listings" tabs. The top bar shows a
 * clickable location name with a pin icon that opens a dialog to change the location. When a new
 * location is selected, the [onLocationChange] callback is invoked to navigate to the new location.
 *
 * @param browseCityViewModel The ViewModel for managing the screen's state and data.
 * @param location The current location being browsed.
 * @param onSelectListing A callback invoked when a listing card is clicked, passing the selected
 *   listing.
 * @param onSelectResidency A callback invoked when a residency card is clicked, passing the
 *   selected residency.
 * @param onLocationChange A callback invoked when the user selects a new location from the dialog.
 * @param navigationActions Optional navigation helper.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseCityScreen(
    browseCityViewModel: BrowseCityViewModel = viewModel(),
    location: Location,
    onSelectListing: (ListingCardUI) -> Unit = {},
    onSelectResidency: (ResidencyCardUI) -> Unit = {},
    onLocationChange: (Location) -> Unit = {},
    onAddListingClick: () -> Unit = {},
    onAddReviewClick: () -> Unit = {},
    navigationActions: NavigationActions? = null
) {

  LaunchedEffect(location) {
    browseCityViewModel.loadResidencies(location)
    browseCityViewModel.loadListings(location)
  }

  val uiState by browseCityViewModel.uiState.collectAsState()

  val onLocationClick = remember { { browseCityViewModel.onCustomLocationClick() } }

  val onValueChange =
      remember<(String) -> Unit> { { query -> browseCityViewModel.setCustomLocationQuery(query) } }
  val onDropDownLocationSelect =
      remember<(Location) -> Unit> {
        { location -> browseCityViewModel.setCustomLocation(location) }
      }
  val onDismiss = remember { { browseCityViewModel.dismissCustomLocationDialog() } }
  val onConfirm =
      remember<(Location) -> Unit> {
        { newLocation ->
          browseCityViewModel.saveLocationToProfile(newLocation)
          onLocationChange(newLocation)
          browseCityViewModel.dismissCustomLocationDialog()
        }
      }

  BrowseCityScreenUI(
      location = location,
      listingsState = uiState.listings,
      residenciesState = uiState.residencies,
      onSelectListing = onSelectListing,
      onSelectResidency = onSelectResidency,
      onLocationClick = onLocationClick,
      onAddListingClick = { navigationActions?.navigateTo(Screen.AddListing) },
      onAddReviewClick = { navigationActions?.navigateTo(Screen.AddReview) },
      navigationActions = navigationActions)

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
 * @param residenciesState The state of the residencies (loading, items, error).
 * @param onSelectListing A callback invoked when a listing card is clicked.
 * @param onSelectResidency A callback invoked when a residency card is clicked.
 * @param onLocationClick A callback invoked when the location title is clicked to open the dialog.
 * @param navigationActions Optional navigation helper.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseCityScreenUI(
    location: Location,
    listingsState: ListingsState,
    residenciesState: ResidenciesState,
    onSelectListing: (ListingCardUI) -> Unit,
    onSelectResidency: (ResidencyCardUI) -> Unit,
    onLocationClick: () -> Unit,
    onAddListingClick: () -> Unit = {},
    onAddReviewClick: () -> Unit = {},
    navigationActions: NavigationActions? = null,
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(1) } // 0 Reviews, 1 Listings

  Scaffold(
      bottomBar = { BottomBarFromNav(navigationActions) },
      floatingActionButton = {
        AddFabMenu(
            onAddListing = onAddListingClick,
            onAddReview = onAddReviewClick,
            modifier = Modifier.navigationBarsPadding().imePadding())
      },
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              TextButton(
                  onClick = onLocationClick,
                  modifier = Modifier.testTag(C.BrowseCityTags.LOCATION_BUTTON)) {
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
                  onClick = { navigationActions?.navigateToHomepageDirectly() },
                  modifier = Modifier.testTag(C.BrowseCityTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Homepage",
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
              when {
                residenciesState.loading -> {
                  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.testTag(C.BrowseCityTags.LOADING))
                  }
                }
                residenciesState.error != null -> {
                  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = residenciesState.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(C.BrowseCityTags.ERROR))
                  }
                }
                residenciesState.items.isEmpty() -> {
                  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No residencies yet.",
                        color = TextColor,
                        modifier = Modifier.testTag(C.BrowseCityTags.EMPTY))
                  }
                }
                else -> {
                  LazyColumn(
                      modifier =
                          Modifier.fillMaxSize()
                              .padding(horizontal = 16.dp)
                              .testTag(C.BrowseCityTags.RESIDENCY_LIST),
                      contentPadding = PaddingValues(vertical = 12.dp),
                      verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(residenciesState.items) { item ->
                          ResidencyCard(item, onSelectResidency)
                        }
                      }
                }
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
                    Text(
                        "No listings yet.",
                        modifier = Modifier.testTag(C.BrowseCityTags.EMPTY),
                        color = TextColor)
                  }
                }
                else -> {
                  LazyColumn(
                      modifier =
                          Modifier.fillMaxSize()
                              .padding(horizontal = 16.dp)
                              .testTag(C.BrowseCityTags.LISTING_LIST),
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
      modifier = Modifier.fillMaxWidth().testTag(C.BrowseCityTags.listingCard(data.listingUid)),
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

@Composable
private fun ResidencyCard(data: ResidencyCardUI, onClick: (ResidencyCardUI) -> Unit) {
  OutlinedCard(
      shape = RoundedCornerShape(16.dp),
      modifier =
          Modifier.fillMaxWidth().padding(0.dp).testTag(C.BrowseCityTags.residencyCard(data.title)),
      onClick = { onClick(data) }) {
        Row(
            modifier = Modifier.fillMaxWidth(1f).fillMaxHeight(1f),
            verticalAlignment = Alignment.CenterVertically) {
              // Image placeholder (left)
              Box(
                  modifier =
                      Modifier.height(160.dp)
                          .fillMaxWidth(0.4F)
                          .clip(RoundedCornerShape(12.dp))
                          .background(Color(0xFFEAEAEA))) {
                    Text(
                        "IMAGE",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray)
                  }

              Spacer(Modifier.width(12.dp))

              Column(
                  modifier = Modifier.weight(1f).height(160.dp).padding(4.dp),
                  verticalArrangement = Arrangement.SpaceBetween) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                    ) { // Title, location, and grade
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically) { // Title + grade
                            Text( // Title
                                text = data.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Start,
                                color = TextColor,
                                modifier = Modifier.fillMaxWidth(0.5f),
                                maxLines = 1,
                            )
                            DisplayGrade(data.meanGrade, 16.dp) // Display the mean grade with stars
                      }
                      Row(verticalAlignment = Alignment.Top) { // Location
                        Icon(
                            Icons.Outlined.Place,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MainColor)
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = data.location,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.Start,
                            color = TextColor,
                            maxLines = 2,
                        )
                      }
                    }
                    if (data.latestReview == null) { // No latest review => No reviews yet
                      Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "No reviews yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = TextColor,
                            modifier =
                                Modifier.testTag(
                                    C.BrowseCityTags.residencyCardEmptyReview(data.title)))
                      }
                    } else { // Review, postedAt, postedBy
                      Column(
                          modifier = Modifier.fillMaxSize(),
                          verticalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.padding(top = 4.dp)) {
                              Row(
                                  modifier = Modifier.fillMaxWidth(),
                                  horizontalArrangement = Arrangement.SpaceBetween,
                                  verticalAlignment =
                                      Alignment.CenterVertically) { // Latest review + post date
                                    Text(
                                        text = "Latest review :",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Start,
                                        color = TextColor,
                                    )
                                    Text(
                                        text = formatDate(data.latestReview.postedAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.End,
                                        fontWeight = FontWeight.Light,
                                        color = TextColor,
                                    )
                                  }
                              val truncatedReview =
                                  truncateText(
                                      data.latestReview.reviewText,
                                      90) // truncate the review if it is too large
                              Text( // Review
                                  text = truncatedReview,
                                  style = MaterialTheme.typography.bodySmall,
                                  textAlign = TextAlign.Justify,
                                  color = TextColor,
                                  maxLines = 3,
                                  modifier =
                                      Modifier.testTag(
                                          C.BrowseCityTags.reviewText(data.latestReview.uid)))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End) { // posted by
                                  Text(
                                      text = "posted by ${data.fullNameOfPoster}",
                                      style = MaterialTheme.typography.bodySmall,
                                      fontWeight = FontWeight.Light,
                                      color = TextColor,
                                  )
                                }
                          }
                    }
                  }
              Spacer(Modifier.width(12.dp))
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
  val sampleListingUi =
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

  val sampleReview =
      Review(
          uid = "",
          ownerId = "",
          postedAt = Timestamp.now(),
          title = "",
          reviewText = "This is an example review, I can write anything here",
          grade = 4.0,
          residencyName = "Vortex",
          roomType = RoomType.STUDIO,
          pricePerMonth = 1000.0,
          areaInM2 = 44,
          imageUrls = emptyList())

  val sampleResidencyUi =
      ResidenciesState(
          loading = false,
          items =
              listOf(
                  ResidencyCardUI(
                      title = "Vortex",
                      meanGrade = 4.5,
                      location = "Rte de Praz Véguey 29, 1022 Chavannes-près-Renens",
                      latestReview = sampleReview,
                      fullNameOfPoster = "John Doe"),
                  ResidencyCardUI(
                      title = "Atrium",
                      meanGrade = 2.0,
                      location = "Rte Louis Favre 4, 1024 Ecublens",
                      latestReview = sampleReview,
                      fullNameOfPoster = "Doe John"),
              ),
      )
  MySwissDormAppTheme {
    BrowseCityScreenUI(
        listingsState = sampleListingUi,
        residenciesState = sampleResidencyUi,
        onSelectListing = {},
        onSelectResidency = {},
        location = Location("Lausanne", 46.5197, 6.6323),
        onLocationClick = {})
  }
}
