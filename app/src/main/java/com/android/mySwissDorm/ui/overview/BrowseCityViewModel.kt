package com.android.mySwissDorm.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatDate
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents the UI state of a single listing.
 *
 * @property title The title of the listing
 * @property leftBullets A list of strings to be displayed as bullet points on the left side
 * @property rightBullets A list of strings to be displayed as bullet points on the right side
 * @property listingUid The unique identifier of the listing
 */
data class ListingCardUI(
    val title: String,
    val leftBullets: List<String>,
    val rightBullets: List<String>,
    val listingUid: String,
)

/**
 * Represents the state of listings being loaded, including loading status, list of items, and any
 * error message.
 *
 * @property loading A boolean indicating if the listings are currently being loaded
 * @property items A list of `ListingCardUI` items representing the loaded listings
 * @property error An optional error message if loading failed
 */
data class ListingsState(
    val loading: Boolean = false,
    val items: List<ListingCardUI> = emptyList(),
    val error: String? = null
)

/**
 * Represents the overall UI state for browsing previews and listings in a city.
 *
 * @property listings The state of the listings being displayed // Future enhancement: add reviews
 *   state here as well
 * @property customLocationQuery The user's input query for a custom location.
 * @property customLocation The selected custom location.
 * @property locationSuggestions A list of location suggestions based on the user's query.
 * @property showCustomLocationDialog A flag to control the visibility of the custom location
 *   dialog.
 */
data class BrowseCityUiState(
    val listings: ListingsState = ListingsState(),
    val customLocationQuery: String = "",
    val customLocation: Location? = null,
    val locationSuggestions: List<Location> = emptyList(),
    val showCustomLocationDialog: Boolean = false
    // will then add equivalent for reviews
)

/**
 * ViewModel for browsing listings in a specific city.
 *
 * Responsible for managing the UI state, by fetching and providing rental listings via the
 * [RentalListingRepository].
 *
 * @property listingsRepository The repository used to fetch and manage rental listings. // Future
 *   enhancement: add reviews repository here as well
 * @property locationRepository The repository for searching locations.
 * @property profileRepository The repository for managing user profile data.
 * @property auth The Firebase Auth instance for getting the current user.
 */
class BrowseCityViewModel(
    private val listingsRepository: RentalListingRepository =
        RentalListingRepositoryProvider.repository,
    private val locationRepository: LocationRepository = LocationRepositoryProvider.repository,
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(BrowseCityUiState())
  val uiState: StateFlow<BrowseCityUiState> = _uiState.asStateFlow()

  fun loadListings(location: Location) {
    _uiState.update { it.copy(listings = it.listings.copy(loading = true, error = null)) }

    viewModelScope.launch {
      try {
        // Fetch all and filter by residency.city value matching the given cityName string
        val filtered = listingsRepository.getAllRentalListingsByLocation(location, 10.0)
        val mapped = filtered.map { it.toCardUI() }

        _uiState.update {
          it.copy(listings = it.listings.copy(loading = false, items = mapped, error = null))
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              listings =
                  it.listings.copy(loading = false, error = e.message ?: "Failed to load listings"))
        }
      }
    }
  }

  /**
   * Sets the custom location query and fetches suggestions if the query is not empty.
   *
   * @param query The user's search query.
   */
  fun setCustomLocationQuery(query: String) {
    _uiState.update { it.copy(customLocationQuery = query) }
    if (query.isNotEmpty()) {
      viewModelScope.launch {
        try {
          val results = locationRepository.search(query)
          _uiState.update { it.copy(locationSuggestions = results) }
        } catch (e: Exception) {
          Log.e("BrowseCityViewModel", "Error fetching location suggestions", e)
          _uiState.update { it.copy(locationSuggestions = emptyList()) }
        }
      }
    } else {
      _uiState.update { it.copy(locationSuggestions = emptyList()) }
    }
  }

  /**
   * Sets the selected custom location and updates the query to match.
   *
   * @param location The selected location.
   */
  fun setCustomLocation(location: Location) {
    _uiState.update { it.copy(customLocation = location, customLocationQuery = location.name) }
  }

  /** Shows the custom location dialog. */
  fun onCustomLocationClick(currentLocation: Location? = null) {
    _uiState.update {
      it.copy(
          showCustomLocationDialog = true,
          customLocationQuery = currentLocation?.name ?: "",
          customLocation = currentLocation)
    }
  }

  /** Hides the custom location dialog and resets its state. */
  fun dismissCustomLocationDialog() {
    _uiState.update {
      it.copy(showCustomLocationDialog = false, customLocationQuery = "", customLocation = null)
    }
  }

  /**
   * Saves the selected location to the user's profile.
   *
   * @param location The location to save to the profile.
   */
  fun saveLocationToProfile(location: Location) {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      Log.e("BrowseCityViewModel", "Cannot save location: user not logged in")
      return
    }

    viewModelScope.launch {
      try {
        // get current profile
        val profile = profileRepository.getProfile(uid)
        // update location in userInfo
        val updatedUserInfo = profile.userInfo.copy(location = location)
        val updatedProfile = profile.copy(userInfo = updatedUserInfo)
        // save updated profile
        profileRepository.editProfile(updatedProfile)
        Log.d("BrowseCityViewModel", "Location saved to profile: ${location.name}")
      } catch (e: Exception) {
        Log.e("BrowseCityViewModel", "Error saving location to profile", e)
      }
    }
  }
}

// Mapping RentalListing to ListingCardUI
private fun RentalListing.toCardUI(): ListingCardUI {
  val price = String.format(Locale.getDefault(), "%.0f.-/month", pricePerMonth)
  val area = "${areaInM2}m²"
  val start = "Starting ${formatDate(startDate)}"
  val resName = residencyName

  return ListingCardUI(
      title = title,
      leftBullets = listOf(roomType.toString(), price, area),
      rightBullets = listOf(start, resName),
      listingUid = uid)
}
