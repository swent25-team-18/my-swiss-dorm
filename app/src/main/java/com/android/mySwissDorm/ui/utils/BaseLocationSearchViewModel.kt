package com.android.mySwissDorm.ui.utils

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import kotlinx.coroutines.launch

/**
 * Base ViewModel for screens that include the custom location search feature.
 *
 * This class abstracts the common logic for searching, selecting, and fetching location names
 * (reverse geocoding).
 */
abstract class BaseLocationSearchViewModel : ViewModel() {

  // Common dependency to be provided by subclasses
  protected abstract val locationRepository: LocationRepository

  // This tag will be overridden by subclasses for correct logging
  protected abstract val logTag: String

  // --- Abstract methods to be implemented by subclasses ---

  /** Subclass must implement this to update its UI state with a new search query. */
  protected abstract fun updateStateWithQuery(query: String)

  /** Subclass must implement this to update its UI state with location suggestions. */
  protected abstract fun updateStateWithSuggestions(suggestions: List<Location>)

  /**
   * Subclass must implement this to update its UI state with a selected location. This should
   * update both the 'customLocation' and 'customLocationQuery'.
   */
  protected abstract fun updateStateWithLocation(location: Location)

  /**
   * Subclass must implement this to update its UI state to show the dialog.
   *
   * @param currentLocation The location to pre-fill in the dialog, if any.
   */
  protected abstract fun updateStateShowDialog(currentLocation: Location? = null)

  /**
   * Subclass must implement this to update its UI state to hide the dialog and reset the custom
   * location search state.
   */
  protected abstract fun updateStateDismissDialog()

  /** Sets the custom location query and fetches suggestions. */
  open fun setCustomLocationQuery(query: String) {
    updateStateWithQuery(query)
    if (query.isNotEmpty()) {
      viewModelScope.launch {
        try {
          val results = locationRepository.search(query)
          updateStateWithSuggestions(results)
        } catch (e: Exception) {
          Log.e(logTag, "Error fetching location suggestions", e)
          updateStateWithSuggestions(emptyList())
        }
      }
    } else {
      updateStateWithSuggestions(emptyList())
    }
  }

  /** Sets the selected custom location and updates the query to match. */
  open fun setCustomLocation(location: Location) {
    updateStateWithLocation(location)
  }

  /**
   * Shows the custom location dialog.
   *
   * @param currentLocation An optional location to pre-populate the search query.
   */
  open fun onCustomLocationClick(currentLocation: Location? = null) {
    updateStateShowDialog(currentLocation)
  }

  /** Hides the custom location dialog and resets its state. */
  open fun dismissCustomLocationDialog() {
    updateStateDismissDialog()
  }

  /**
   * Fetches the location name (address) from given coordinates. This is the function you asked
   * about, now implemented once!
   */
  open fun fetchLocationName(latitude: Double, longitude: Double) {
    viewModelScope.launch {
      try {
        val location = locationRepository.reverseSearch(latitude, longitude)
        if (location != null) {
          updateStateWithLocation(location)
        } else {
          Log.w(logTag, "Could not reverse geocode location")
        }
      } catch (e: Exception) {
        Log.e(logTag, "Error reverse geocoding", e)
      }
    }
  }
}
