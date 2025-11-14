package com.android.mySwissDorm.ui.homepage

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.ui.navigation.BottomNavigationMenu
import com.android.mySwissDorm.ui.navigation.NavigationActions
import com.android.mySwissDorm.ui.navigation.Screen
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.utils.CustomLocationDialog
import com.android.mySwissDorm.ui.utils.fetchDeviceLocation
import com.google.android.gms.location.LocationServices

/** Test tags for the Home Page screen, used for UI testing. */
object HomePageScreenTestTags {
  const val SEARCH_BAR = "searchBar"
  const val SEARCH_BAR_TEXT_FIELD = "searchBarTextField"
  const val CITIES_LIST = "citiesList"

  fun getTestTagForCityCard(cityName: String): String = "cityCard${cityName}"

  fun getTestTagForCityCardTitle(cityName: String): String = "cityCardTitle${cityName}"

  fun getTestTagForCityCardDescription(cityName: String): String = "cityCardDescription${cityName}"
}

/**
 * The main screen for the home page, displaying a list of cities and a search bar.
 *
 * @param homePageViewModel The ViewModel for this screen.
 * @param credentialManager The credential manager for handling user credentials.
 * @param onSelectLocation A callback for when a city or custom location is selected.
 * @param navigationActions Actions for navigating to other screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScreen(
    homePageViewModel: HomePageViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSelectLocation: (Location) -> Unit = {},
    navigationActions: NavigationActions? = null
) {
  val uiState by homePageViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val lazyState = LazyListState()
  val fusedLocationClient =
      remember(context) { LocationServices.getFusedLocationProviderClient(context) }

  var hasLocationPermission by remember {
    mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED)
  }

  val onFetchLocationName =
      remember<(Double, Double) -> Unit> {
        { lat, lon -> homePageViewModel.fetchLocationName(lat, lon) }
      }

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

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message -> Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
  }

  Scaffold(
      bottomBar = {
        BottomNavigationMenu(
            selectedScreen = Screen.Homepage, onTabSelected = { navigationActions?.navigateTo(it) })
      }) { paddingValues ->
        var inputText by remember { mutableStateOf("") }
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
          Box(
              modifier =
                  Modifier.testTag(HomePageScreenTestTags.SEARCH_BAR)
                      .fillMaxWidth()
                      .wrapContentHeight(),
              contentAlignment = Alignment.Center) {
                TextField(
                    modifier =
                        Modifier.testTag(HomePageScreenTestTags.SEARCH_BAR_TEXT_FIELD)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 32.dp)
                            .border(2.dp, TextColor, RoundedCornerShape(20.dp)),
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Browse", fontSize = 18.sp) },
                    leadingIcon = {
                      Icon(
                          Icons.Default.Search,
                          modifier = Modifier.size(30.dp),
                          contentDescription = "Search",
                          tint = MainColor)
                    },
                    singleLine = true,
                    colors =
                        TextFieldDefaults.colors(
                            unfocusedContainerColor = BackGroundColor,
                            focusedContainerColor = BackGroundColor,
                            unfocusedIndicatorColor = BackGroundColor,
                            focusedIndicatorColor = BackGroundColor))
              }

          TextButton(
              onClick = { homePageViewModel.onCustomLocationClick() },
              modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Custom Location",
                    tint = MainColor)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Custom location", color = MainColor)
              }

          LazyColumn(
              modifier =
                  Modifier.testTag(HomePageScreenTestTags.CITIES_LIST)
                      .fillMaxWidth()
                      .padding(horizontal = 32.dp)
                      .padding(top = 10.dp),
              state = lazyState) {
                items(uiState.cities.size) { index ->
                  val city = uiState.cities[index]
                  if (city.name.contains(inputText, ignoreCase = true) ||
                      city.description.contains(inputText, ignoreCase = true)) {
                    CityCard(
                        city = city,
                        onClick = {
                          val cityLocation = city.location
                          homePageViewModel.saveLocationToProfile(cityLocation)
                          onSelectLocation(cityLocation)
                        })
                    Spacer(modifier = Modifier.height(16.dp))
                  }
                }
              }
        }

        if (uiState.showCustomLocationDialog) {
          val onValueChange =
              remember<(String) -> Unit> {
                { query -> homePageViewModel.setCustomLocationQuery(query) }
              }
          val onDropDownLocationSelect =
              remember<(Location) -> Unit> {
                { location -> homePageViewModel.setCustomLocation(location) }
              }
          val onDismiss = remember { { homePageViewModel.dismissCustomLocationDialog() } }
          val onConfirm =
              remember<(Location) -> Unit> {
                { location ->
                  homePageViewModel.saveLocationToProfile(location)
                  onSelectLocation(location)
                  homePageViewModel.dismissCustomLocationDialog()
                }
              }

          CustomLocationDialog(
              value = uiState.customLocationQuery,
              currentLocation = uiState.customLocation,
              locationSuggestions = uiState.locationSuggestions,
              onValueChange = onValueChange,
              onDropDownLocationSelect = onDropDownLocationSelect,
              onDismiss = onDismiss,
              onConfirm = onConfirm,
              onUseCurrentLocationClick = onUseCurrentLocationClick)
        }
      }
}

/**
 * A card that displays information about a city.
 *
 * @param city The city to display.
 * @param onClick A callback for when the card is clicked.
 */
@Composable
fun CityCard(city: City, onClick: () -> Unit) {
  Card(
      modifier =
          Modifier.testTag(HomePageScreenTestTags.getTestTagForCityCard(city.name))
              .fillMaxWidth()
              .padding(vertical = 6.dp)
              .border(2.dp, TextColor, RoundedCornerShape(10.dp))
              .clickable { onClick() },
  ) {
    Box {
      Image(
          painter = painterResource(city.imageId),
          contentDescription = city.name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxWidth().height(180.dp))
      Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart).padding(8.dp)) {
        Text(
            modifier =
                Modifier.testTag(HomePageScreenTestTags.getTestTagForCityCardTitle(city.name)),
            text = city.name,
            color = TextColor,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier =
                Modifier.testTag(
                    HomePageScreenTestTags.getTestTagForCityCardDescription(city.name)),
            text = city.description,
            color = TextColor,
            fontSize = 12.sp)
      }
    }
  }
}
