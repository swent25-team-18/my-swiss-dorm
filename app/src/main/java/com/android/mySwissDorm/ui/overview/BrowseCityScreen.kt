package com.android.mySwissDorm.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.ui.theme.Red0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseCityScreen(
    browseCityViewModel: BrowseCityViewModel = viewModel(),
    cityName: String,
    onGoBack: () -> Unit = {},
    onSelectListing: (ListingCardUI) -> Unit = {}
) {
  LaunchedEffect(cityName) { browseCityViewModel.loadListings(cityName) }

  val uiState by browseCityViewModel.uiState.collectAsState()
  BrowseCityScreenUI(
      cityName = cityName,
      listingsState = uiState.listings,
      onGoBack = onGoBack,
      onSelectListing = onSelectListing)
}

// Pure UI (stateless) — easy to preview & test.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseCityScreenUI(
    cityName: String,
    listingsState: ListingsState,
    onGoBack: () -> Unit,
    onSelectListing: (ListingCardUI) -> Unit
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(1) } // 0 Reviews, 1 Listings

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(cityName) },
            navigationIcon = {
              IconButton(onClick = onGoBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Red0)
              }
            })
      }) { pd ->
        Column(modifier = Modifier.padding(pd).fillMaxSize()) {
          TabRow(
              selectedTabIndex = selectedTab,
              containerColor = Color.Transparent,
              contentColor = Color.Black,
              divider = { HorizontalDivider(thickness = 1.dp, color = Color.Black) },
              indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                    height = 2.dp,
                    color = Red0)
              }) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = Color.Black,
                    unselectedContentColor = Color.Black,
                    text = { Text("Reviews") })
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = Color.Black,
                    unselectedContentColor = Color.Black,
                    text = { Text("Listings") })
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
                    CircularProgressIndicator()
                  }
                }
                listingsState.error != null -> {
                  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = listingsState.error, color = MaterialTheme.colorScheme.error)
                  }
                }
                listingsState.items.isEmpty() -> {
                  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No listings yet.")
                  }
                }
                else -> {
                  LazyColumn(
                      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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

@Composable
private fun ListingCard(data: ListingCardUI, onClick: (ListingCardUI) -> Unit) {
  OutlinedCard(
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth(),
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
private fun BulletColumn(items: List<String>, modifier: Modifier = Modifier) {
  Column(modifier) {
    items.forEach {
      Text("• $it", style = MaterialTheme.typography.bodyMedium)
      Spacer(Modifier.height(6.dp))
    }
  }
}

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
                      listingUid = "preview1"
                  ),
                  ListingCardUI(
                      title = "Bright studio near EPFL",
                      leftBullets = listOf("Studio", "1’150.-/month", "24m²"),
                      rightBullets = listOf("Starting 30/09/2025", "Private Accommodation"),
                      listingUid = "preview2"
                  )
              ),
      )
  BrowseCityScreenUI(
      cityName = "Lausanne", listingsState = sampleUi, onGoBack = {}, onSelectListing = {})
}
