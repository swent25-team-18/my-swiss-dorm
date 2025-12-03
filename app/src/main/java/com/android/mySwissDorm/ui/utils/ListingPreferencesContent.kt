package com.android.mySwissDorm.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.resources.C.FilterTestTags.BUDGET
import com.android.mySwissDorm.resources.C.FilterTestTags.LOCATION_PREFERENCE
import com.android.mySwissDorm.resources.C.FilterTestTags.PREFERRED_ROOM_TYPE
import com.android.mySwissDorm.resources.C.FilterTestTags.PREFERRED_SIZE
import com.android.mySwissDorm.resources.C.FilterTestTags.SIGN_UP_WITH_PREFERENCES
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.White
import kotlin.collections.chunked
import kotlin.collections.forEach

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
      },
      bottomBar = {
        Column(modifier = Modifier.padding(16.dp)) {
          if (errorMsg != null) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp))
          }

          Button(
              onClick = onBottomButtonClick,
              modifier = Modifier.fillMaxWidth().height(52.dp).testTag(SIGN_UP_WITH_PREFERENCES),
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MainColor)) {
                if (isLoading) {
                  CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                } else {
                  Text(text = bottomButtonText, color = White)
                }
              }
        }
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
                          .testTag(LOCATION_PREFERENCE),
                  placeholder = { Text("e.g. Lausanne") },
                  colors =
                      OutlinedTextFieldDefaults.colors(
                          disabledBorderColor = BackGroundColor,
                          disabledTextColor = TextColor,
                          disabledPlaceholderColor = BackGroundColor,
                          disabledContainerColor = BackGroundColor),
                  shape = RoundedCornerShape(5.dp),
                  trailingIcon = {
                    Icon(Icons.Default.Place, contentDescription = null, tint = MainColor)
                  })

              HorizontalDivider()

              // --- Budget ---
              Text(
                  stringResource(R.string.budget),
                  fontWeight = FontWeight.SemiBold,
                  color = TextColor,
                  modifier = Modifier.testTag(BUDGET))
              PriceFilterContent(
                  priceRange = Pair(minPrice, maxPrice), onRangeChange = onPriceRangeChange)

              HorizontalDivider()

              // --- Size ---
              Text(
                  stringResource(R.string.preferred_size),
                  fontWeight = FontWeight.SemiBold,
                  color = TextColor,
                  modifier = Modifier.testTag(PREFERRED_SIZE))
              SizeFilterContent(
                  sizeRange = Pair(minSize, maxSize), onRangeChange = onSizeRangeChange)

              HorizontalDivider()

              // --- Room Types ---
              Text(
                  stringResource(R.string.preferred_room_type),
                  fontWeight = FontWeight.SemiBold,
                  color = TextColor,
                  modifier = Modifier.testTag(PREFERRED_ROOM_TYPE))
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

              Spacer(modifier = Modifier.height(20.dp))
            }
      }
}
