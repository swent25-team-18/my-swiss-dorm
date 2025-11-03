package com.android.mySwissDorm.ui.homepage

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.ui.navigation.BottomNavigationMenu
import com.android.mySwissDorm.ui.navigation.NavigationActions
import com.android.mySwissDorm.ui.navigation.Screen
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor

object HomePageScreenTestTags {
  const val SEARCH_BAR = "searchBar"
  const val SEARCH_BAR_TEXT_FIELD = "searchBarTextField"
  const val CITIES_LIST = "citiesList"

  fun getTestTagForCityCard(cityName: String): String = "cityCard${cityName}"

  fun getTestTagForCityCardTitle(cityName: String): String = "cityCardTitle${cityName}"

  fun getTestTagForCityCardDescription(cityName: String): String = "cityCardDescription${cityName}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScreen(
    homePageViewModel: HomePageViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSelectCity: (City) -> Unit = {},
    navigationActions: NavigationActions? = null
) {
  val uiState by homePageViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val lazyState = LazyListState()

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
                    CityCard(city = city, onClick = { onSelectCity(city) })
                    Spacer(modifier = Modifier.height(16.dp))
                  }
                }
              }
        }

        if (uiState.showCustomLocationDialog) {
          CustomLocationDialog(
              value = uiState.customLocation,
              onValueChange = { homePageViewModel.setCustomLocation(it) },
              onDismiss = { homePageViewModel.dismissCustomLocationDialog() },
              onConfirm = { homePageViewModel.onCustomLocationConfirm() })
        }
      }
}

@Composable
fun CustomLocationDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(shape = RoundedCornerShape(16.dp)) {
      Box {
        Column(
            modifier = Modifier.padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Text("Enter Custom Location", fontSize = 20.sp, fontWeight = FontWeight.Bold)
              TextField(value = value, onValueChange = onValueChange, label = { Text("Location") })
              Button(onClick = onConfirm) { Text("Confirm") }
            }
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Red)
        }
      }
    }
  }
}

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
                Modifier.testTag(HomePageScreenTestTags.getTestTagForCityCardDescription(city.name))
                    .fillMaxWidth(0.9f),
            text = city.description,
            color = TextColor,
            fontSize = 12.sp)
      }
    }
  }
}
