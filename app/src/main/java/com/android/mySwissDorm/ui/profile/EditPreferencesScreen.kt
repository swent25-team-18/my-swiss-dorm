package com.android.mySwissDorm.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.mySwissDorm.R
import com.android.mySwissDorm.ui.authentification.ListingPreferencesContent
import com.android.mySwissDorm.ui.utils.onUserLocationClickFunc

@Composable
fun EditPreferencesScreen(viewModel: ProfileScreenViewModel, onBack: () -> Unit) {
  val state by viewModel.uiState.collectAsState()
  val context = LocalContext.current

  val onUseCurrentLocationClick = onUserLocationClickFunc(context, viewModel)

  ListingPreferencesContent(
      title = stringResource(R.string.listing_preferences),
      selectedLocation = state.prefLocation,
      minPrice = state.minPrice,
      maxPrice = state.maxPrice,
      minSize = state.minSize,
      maxSize = state.maxSize,
      selectedRoomTypes = state.selectedRoomTypes,
      showLocationDialog = state.showLocationDialog,
      locationQuery = state.locationQuery,
      locationSuggestions = state.locationSuggestions,
      onLocationClick = { viewModel.onCustomLocationClick(state.prefLocation) },
      onLocationQueryChange = { viewModel.setCustomLocationQuery(it) },
      onLocationSelected = { viewModel.setCustomLocation(it) },
      onDismissDialog = { viewModel.dismissCustomLocationDialog() },
      onPriceRangeChange = { min, max -> viewModel.onPriceRangeChange(min, max) },
      onSizeRangeChange = { min, max -> viewModel.onSizeRangeChange(min, max) },
      onToggleRoomType = { viewModel.onToggleRoomType(it) },
      onBack = onBack,
      isLoading = state.isSaving,
      errorMsg = state.errorMsg,
      bottomButtonText = stringResource(R.string.save_preferences),
      onBottomButtonClick = { viewModel.savePreferences(context, onSuccess = onBack) },
      onUseCurrentLocationClick = onUseCurrentLocationClick)
}
