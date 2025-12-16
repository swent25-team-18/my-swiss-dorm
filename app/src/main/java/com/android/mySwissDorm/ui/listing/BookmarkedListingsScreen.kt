package com.android.mySwissDorm.ui.listing

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.overview.ListingCardUI
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.MainColor
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

  // Load bookmarked listings on first composition (replaces LaunchedEffect as suggested in PR
  // review)
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
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .testTag(C.BrowseCityTags.BOOKMARKED_LISTINGS_ROOT)) {
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
                      modifier = Modifier.align(Alignment.Center).padding(Dimens.PaddingDefault))
                }
                else -> {
                  LazyColumn(
                      modifier = Modifier.fillMaxSize(),
                      contentPadding =
                          PaddingValues(
                              horizontal = Dimens.PaddingDefault, vertical = Dimens.PaddingSmall),
                      verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)) {
                        items(uiState.listings, key = { it.listingUid }) { listing ->
                          ListingCard(
                              data = listing,
                              onClick = { onSelectListing(listing) },
                              isBookmarked =
                                  uiState.bookmarkedListingIds.contains(listing.listingUid),
                              onToggleBookmark = {
                                viewModel.toggleBookmark(listing.listingUid, context)
                              },
                              isGuest = false)
                        }
                      }
                }
              }
            }
      }
}
