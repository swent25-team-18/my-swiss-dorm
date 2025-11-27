package com.android.mySwissDorm.ui.listing

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.overview.ListingCardUI
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkedListingsScreen(
    onGoBack: () -> Unit = {},
    onSelectListing: (ListingCardUI) -> Unit = {},
    viewModel: BookmarkedListingsViewModel = viewModel()
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(Unit) { viewModel.loadBookmarkedListings(context) }

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      viewModel.clearError()
    }
  }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  stringResource(R.string.bookmarked_listings_title),
                  style = MaterialTheme.typography.titleMedium)
            },
            navigationIcon = {
              IconButton(onClick = onGoBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MainColor)
              }
            })
      },
      containerColor = BackGroundColor) { paddingValues ->
        Box(
            modifier =
                Modifier.fillMaxSize().padding(paddingValues).testTag("bookmarkedListingsRoot")) {
              when {
                uiState.loading -> {
                  CircularProgressIndicator(
                      modifier = Modifier.align(Alignment.Center), color = MainColor)
                }
                uiState.listings.isEmpty() -> {
                  Text(
                      text = stringResource(R.string.bookmarked_listings_no_bookmarks_yet),
                      style = MaterialTheme.typography.bodyLarge,
                      color = TextColor,
                      modifier = Modifier.align(Alignment.Center).padding(16.dp))
                }
                else -> {
                  LazyColumn(
                      modifier = Modifier.fillMaxSize(),
                      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                      verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.listings, key = { it.listingUid }) { listing ->
                          ListingCard(
                              listing = listing,
                              onClick = { onSelectListing(listing) },
                              modifier =
                                  Modifier.testTag(
                                      C.BrowseCityTags.listingCard(listing.listingUid)))
                        }
                      }
                }
              }
            }
      }
}

@Composable
private fun ListingCard(
    listing: ListingCardUI,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Card(
      modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
      onClick = onClick,
      colors = CardDefaults.cardColors(containerColor = TextBoxColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              // Image
              if (listing.image != null) {
                AsyncImage(
                    model = listing.image,
                    contentDescription = listing.title,
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop)
              } else {
                Box(
                    modifier =
                        Modifier.size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TextBoxColor),
                    contentAlignment = Alignment.Center) {
                      Text(
                          text = "No image",
                          style = MaterialTheme.typography.bodySmall,
                          color = TextColor.copy(alpha = 0.6f))
                    }
              }

              // Content
              Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis)

                    // Left bullets
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      listing.leftBullets.forEach { bullet ->
                        Text(
                            text = "• $bullet",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextColor)
                      }
                    }

                    // Right bullets
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      listing.rightBullets.forEach { bullet ->
                        Text(
                            text = "• $bullet",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextColor)
                      }
                    }
                  }
            }
      }
}
