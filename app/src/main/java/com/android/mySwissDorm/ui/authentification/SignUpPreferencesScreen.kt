package com.android.mySwissDorm.ui.authentification

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import com.android.mySwissDorm.R
import com.android.mySwissDorm.ui.utils.ListingPreferencesContent
import com.android.mySwissDorm.ui.utils.onUserLocationClickFunc

/**
 * The specific screen used during Sign Up. It uses the shared content but adds the "Sign Up"
 * specific button logic.
 */
@Composable
fun SignUpPreferencesScreen(
    signUpViewModel: SignUpViewModel,
    credentialManager: CredentialManager,
    onBack: () -> Unit,
    onSignedUp: () -> Unit
) {
  val uiState by signUpViewModel.uiState.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(uiState.user) { uiState.user?.let { onSignedUp() } }
  val isFormValid =
      uiState.minPrice != null &&
          uiState.maxPrice != null &&
          uiState.minSize != null &&
          uiState.maxSize != null &&
          uiState.selectedRoomTypes.isNotEmpty()
  ListingPreferencesContent(
      title = stringResource(R.string.screen_sign_up),
      selectedLocation = uiState.selectedLocation,
      minPrice = uiState.minPrice,
      maxPrice = uiState.maxPrice,
      minSize = uiState.minSize,
      maxSize = uiState.maxSize,
      selectedRoomTypes = uiState.selectedRoomTypes,
      showLocationDialog = uiState.showLocationDialog,
      locationQuery = uiState.locationQuery,
      locationSuggestions = uiState.locationSuggestions,
      onLocationClick = { signUpViewModel.onCustomLocationClick(uiState.selectedLocation) },
      onLocationQueryChange = { signUpViewModel.setCustomLocationQuery(it) },
      onLocationSelected = { signUpViewModel.setCustomLocation(it) },
      onDismissDialog = { signUpViewModel.dismissCustomLocationDialog() },
      onPriceRangeChange = { min, max -> signUpViewModel.updatePriceRange(min, max) },
      onSizeRangeChange = { min, max -> signUpViewModel.updateSizeRange(min, max) },
      onToggleRoomType = { signUpViewModel.toggleRoomType(it) },
      onBack = onBack,
      isLoading = uiState.isLoading,
      errorMsg = uiState.errMsg,
      bottomButtonText = stringResource(R.string.sign_up_button_text),
      onBottomButtonClick = { signUpViewModel.signUp(context, credentialManager) },
      onUseCurrentLocationClick = onUserLocationClickFunc(context, signUpViewModel),
      onClearClick = { signUpViewModel.clearPreferences() },
      isButtonEnabled = isFormValid,
      skipButtonText = "Skip",
      onSkipClick = {
        signUpViewModel.clearPreferences()
        signUpViewModel.signUp(context, credentialManager)
      })
}
