package com.android.mySwissDorm.ui.homepage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.city.CitiesRepository
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Home Page screen.
 *
 * @param cities The list of cities to display.
 * @param errorMsg An optional error message to be shown to the user.
 * @param customLocationQuery The user's input query for a custom location.
 * @param customLocation The selected custom location.
 * @param locationSuggestions A list of location suggestions based on the user's query.
 * @param showCustomLocationDialog A flag to control the visibility of the custom location dialog.
 */
data class HomePageUIState(
    val cities: List<City> = emptyList(),
    val errorMsg: String? = null,
    val customLocationQuery: String = "",
    val customLocation: Location? = null,
    val locationSuggestions: List<Location> = emptyList(),
    val showCustomLocationDialog: Boolean = false
)

/**
 * ViewModel for the Home Page screen. Manages the UI state, including city data and custom location
 * search.
 *
 * @param citiesRepository The repository for fetching city data.
 * @param locationRepository The repository for searching locations.
 * @param profileRepository The repository for managing user profile data.
 * @param auth The Firebase Auth instance for getting the current user.
 */
class HomePageViewModel(
    private val citiesRepository: CitiesRepository = CitiesRepositoryProvider.repository,
    private val locationRepository: LocationRepository = LocationRepositoryProvider.repository,
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomePageUIState())
  val uiState: StateFlow<HomePageUIState> = _uiState.asStateFlow()

  init {
    loadCities()
  }

  /** Loads the list of cities from the repository and updates the UI state. */
  private fun loadCities() {
    viewModelScope.launch {
      try {
        val cities = citiesRepository.getAllCities()
        _uiState.value = _uiState.value.copy(cities = cities)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMsg = e.message)
      }
    }
  }

  /**
   * Sets the custom location query and fetches suggestions if the query is not empty.
   *
   * @param query The user's search query.
   */
  fun setCustomLocationQuery(query: String) {
    _uiState.value = _uiState.value.copy(customLocationQuery = query)
    if (query.isNotEmpty()) {
      viewModelScope.launch {
        try {
          val results = locationRepository.search(query)
          _uiState.value = _uiState.value.copy(locationSuggestions = results)
        } catch (e: Exception) {
          Log.e("HomePageViewModel", "Error fetching location suggestions", e)
          _uiState.value = _uiState.value.copy(locationSuggestions = emptyList())
        }
      }
    } else {
      _uiState.value = _uiState.value.copy(locationSuggestions = emptyList())
    }
  }

  /**
   * Sets the selected custom location and updates the query to match.
   *
   * @param location The selected location.
   */
  fun setCustomLocation(location: Location) {
    _uiState.value =
        _uiState.value.copy(customLocation = location, customLocationQuery = location.name)
  }

  /** Shows the custom location dialog. */
  fun onCustomLocationClick() {
    _uiState.value = _uiState.value.copy(showCustomLocationDialog = true)
  }

  /** Hides the custom location dialog and resets its state. */
  fun dismissCustomLocationDialog() {
    _uiState.value =
        _uiState.value.copy(
            showCustomLocationDialog = false, customLocationQuery = "", customLocation = null)
  }

  /**
   * Saves the selected location to the user's profile.
   *
   * @param location The location to save to the profile.
   */
  fun saveLocationToProfile(location: Location) {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      Log.e("HomePageViewModel", "Cannot save location: user not logged in")
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
        Log.d("HomePageViewModel", "Location saved to profile: ${location.name}")
      } catch (e: Exception) {
        Log.e("HomePageViewModel", "Error saving location to profile", e)
      }
    }
  }
}
