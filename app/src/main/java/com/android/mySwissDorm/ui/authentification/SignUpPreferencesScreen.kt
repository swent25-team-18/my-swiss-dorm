package com.android.mySwissDorm.ui.authentification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.ui.theme.Gray
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.utils.CustomLocationDialog
import com.android.mySwissDorm.ui.utils.PriceFilterContent
import com.android.mySwissDorm.ui.utils.SizeFilterContent
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
      onUseCurrentLocationClick = onUserLocationClickFunc(context, signUpViewModel))
}

/**
 * A reusable UI component that displays the preferences form (Location, Price, Size, Room Type). It
 * is agnostic of whether we are Signing Up or Editing a Profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingPreferencesContent(
    title: String,
    selectedLocation: Location?,
    minPrice: Double?,
    maxPrice: Double?,
    minSize: Int?,
    maxSize: Int?,
    selectedRoomTypes: Set<RoomType>,
    showLocationDialog: Boolean,
    locationQuery: String,
    locationSuggestions: List<Location>,
    onLocationClick: () -> Unit,
    onLocationQueryChange: (String) -> Unit,
    onLocationSelected: (Location) -> Unit,
    onDismissDialog: () -> Unit,
    onPriceRangeChange: (Double?, Double?) -> Unit,
    onSizeRangeChange: (Int?, Int?) -> Unit,
    onToggleRoomType: (RoomType) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
    errorMsg: String?,
    bottomButtonText: String,
    onBottomButtonClick: () -> Unit,
    onUseCurrentLocationClick: () -> Unit
) {
  val scrollState = rememberScrollState()

  if (showLocationDialog) {
    CustomLocationDialog(
        value = locationQuery,
        currentLocation = selectedLocation,
        locationSuggestions = locationSuggestions,
        onValueChange = onLocationQueryChange,
        onDropDownLocationSelect = onLocationSelected,
        onDismiss = onDismissDialog,
        onConfirm = { onDismissDialog() },
        onUseCurrentLocationClick = onUseCurrentLocationClick)
  }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(title, color = TextColor, fontWeight = FontWeight.Bold) },
            navigationIcon = {
              IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MainColor)
              }
            })
      }) { innerPadding ->
        Column(
            modifier =
                Modifier.padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
              Text(
                  text = stringResource(R.string.edit_preferences),
                  style = MaterialTheme.typography.titleMedium,
                  color = TextColor)
              // Location
              Text(
                  stringResource(R.string.custom_location),
                  fontWeight = FontWeight.SemiBold,
                  color = TextColor)
              OutlinedTextField(
                  value = selectedLocation?.name ?: "",
                  onValueChange = {},
                  enabled = false,
                  modifier =
                      Modifier.fillMaxWidth()
                          .clickable { onLocationClick() }
                          .testTag("locationField"),
                  label = { Text(stringResource(R.string.screen_browse_overview)) },
                  placeholder = { Text("e.g. Lausanne") },
                  colors =
                      OutlinedTextFieldDefaults.colors(
                          disabledBorderColor = TextBoxColor,
                          disabledTextColor = TextColor,
                          disabledLabelColor = Gray,
                          disabledPlaceholderColor = Gray,
                          disabledContainerColor = TextBoxColor),
                  shape = RoundedCornerShape(10.dp),
                  trailingIcon = {
                    Icon(Icons.Default.Place, contentDescription = null, tint = MainColor)
                  })

              HorizontalDivider()

              // --- Budget ---
              Text(
                  stringResource(R.string.budget),
                  fontWeight = FontWeight.SemiBold,
                  color = TextColor)
              PriceFilterContent(
                  priceRange = Pair(minPrice, maxPrice), onRangeChange = onPriceRangeChange)

              HorizontalDivider()

              // --- Size ---
              Text(
                  stringResource(R.string.preferred_size),
                  fontWeight = FontWeight.SemiBold,
                  color = TextColor)
              SizeFilterContent(
                  sizeRange = Pair(minSize, maxSize), onRangeChange = onSizeRangeChange)

              HorizontalDivider()

              // --- Room Types ---
              Text(
                  stringResource(R.string.preferred_room_type),
                  fontWeight = FontWeight.SemiBold,
                  color = TextColor)
              val roomTypes = RoomType.entries.toList()
              roomTypes.chunked(2).forEach { rowTypes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      rowTypes.forEach { type ->
                        val isSelected = selectedRoomTypes.contains(type)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleRoomType(type) },
                            label = { Text(type.toString()) },
                            leadingIcon =
                                if (isSelected) {
                                  { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null,
                            modifier = Modifier.weight(1f),
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MainColor.copy(alpha = 0.2f),
                                    selectedLabelColor = TextColor))
                      }
                      if (rowTypes.size < 2) Spacer(modifier = Modifier.weight(1f))
                    }
              }

              Spacer(modifier = Modifier.height(24.dp))

              // --- Bottom Button (Dynamic) ---
              Button(
                  onClick = onBottomButtonClick,
                  modifier = Modifier.fillMaxWidth().height(52.dp),
                  shape = RoundedCornerShape(16.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = MainColor)) {
                    if (isLoading) {
                      CircularProgressIndicator(
                          color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                      Text(bottomButtonText, color = Color.White)
                    }
                  }

              if (errorMsg != null) {
                Text(errorMsg, color = MaterialTheme.colorScheme.error)
              }

              Spacer(modifier = Modifier.height(20.dp))
            }
      }
}
