package com.android.mySwissDorm.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.city.CitiesRepository
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.university.UniversitiesRepository
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.android.mySwissDorm.model.university.University
import com.android.mySwissDorm.ui.utils.BaseLocationSearchViewModel
import java.net.URL
import kotlinx.coroutines.launch

class AdminPageViewModel(
    private val citiesRepo: CitiesRepository = CitiesRepositoryProvider.repository,
    private val residenciesRepo: ResidenciesRepository = ResidenciesRepositoryProvider.repository,
    private val universitiesRepo: UniversitiesRepository =
        UniversitiesRepositoryProvider.repository,
    override val locationRepository: LocationRepository = LocationRepositoryProvider.repository,
) : BaseLocationSearchViewModel() {
  override val logTag = "HomePageViewModel"

  enum class EntityType {
    CITY,
    RESIDENCY,
    UNIVERSITY
  }

  data class UiState(
      val selected: EntityType = EntityType.CITY,
      val name: String = "",
      val location: Location? = null,
      val description: String = "",
      val imageId: String = "",
      val city: String = "",
      val email: String = "",
      val phone: String = "",
      val website: String = "",
      val isSubmitting: Boolean = false,
      val message: String? = null,
      val customLocationQuery: String = "",
      val customLocation: Location? = null,
      val locationSuggestions: List<Location> = emptyList(),
      val showCustomLocationDialog: Boolean = false
  )

  var uiState by mutableStateOf(UiState())
    private set

  // Handlers for all necessary form field changes
  fun onTypeChange(t: EntityType) {
    uiState = uiState.copy(selected = t, message = null)
  }

  fun onName(v: String) {
    uiState = uiState.copy(name = v)
  }

  override fun updateStateWithQuery(query: String) {
    uiState = uiState.copy(customLocationQuery = query)
  }

  override fun updateStateWithSuggestions(suggestions: List<Location>) {
    uiState = uiState.copy(locationSuggestions = suggestions)
  }

  override fun updateStateWithLocation(location: Location) {
    uiState = uiState.copy(customLocation = location, customLocationQuery = location.name)
  }

  override fun updateStateShowDialog(currentLocation: Location?) {
    uiState = uiState.copy(showCustomLocationDialog = true)
  }

  override fun updateStateDismissDialog() {
    uiState =
        uiState.copy(
            showCustomLocationDialog = false, customLocationQuery = "", customLocation = null)
  }

  fun onCustomLocationClick() {
    uiState =
        uiState.copy(
            showCustomLocationDialog = true,
            customLocationQuery = uiState.location?.name ?: "",
            customLocation = uiState.location)
  }

  fun onLocationConfirm(location: Location) {
    uiState = uiState.copy(location = location)
    dismissCustomLocationDialog()
  }

  fun onDescription(v: String) {
    uiState = uiState.copy(description = v)
  }

  fun onImageId(v: String) {
    uiState = uiState.copy(imageId = v)
  }

  fun onCity(v: String) {
    uiState = uiState.copy(city = v)
  }

  fun onEmail(v: String) {
    uiState = uiState.copy(email = v)
  }

  fun onPhone(v: String) {
    uiState = uiState.copy(phone = v)
  }

  fun onWebsite(v: String) {
    uiState = uiState.copy(website = v)
  }

  private fun validate(): String? {
    if (uiState.name.isBlank()) return "Name is required."
    if (uiState.location == null) return "Location is required."

    return when (uiState.selected) {
      EntityType.CITY -> {
        when {
          uiState.description.isBlank() -> "Description is required for a City."
          uiState.imageId.isBlank() -> "Image ID is required for a City."
          else -> null
        }
      }
      EntityType.RESIDENCY -> {
        if (uiState.city.isBlank()) "City name is required for a Residency." else null
      }
      EntityType.UNIVERSITY -> {
        when {
          uiState.city.isBlank() -> "City name is required for a University."
          uiState.email.isBlank() -> "Email is required for a University."
          uiState.phone.isBlank() -> "Phone is required for a University."
          uiState.website.isBlank() -> "Website URL is required for a University."
          else -> null
        }
      }
    }
  }

  fun submit() {
    val error = validate()
    if (error != null) {
      uiState = uiState.copy(message = error)
      return
    }
    // Location is validated to be non-null
    val location = uiState.location!!

    viewModelScope.launch {
      uiState = uiState.copy(isSubmitting = true, message = null)
      try {
        // Submit based on selected entity type
        when (uiState.selected) {
          EntityType.CITY -> {
            val city =
                City(
                    name = uiState.name.trim(),
                    description = uiState.description.trim(),
                    location = location,
                    imageId = uiState.imageId.trim().toIntOrNull() ?: 0)
            citiesRepo.addCity(city)
          }
          EntityType.RESIDENCY -> {
            val residency =
                Residency(
                    name = uiState.name.trim(),
                    description = uiState.description.trim(),
                    location = location,
                    city = uiState.city.trim(),
                    email = uiState.email.trim().ifBlank { null },
                    phone = uiState.phone.trim().ifBlank { null },
                    website = uiState.website.trim().takeIf { it.isNotBlank() }?.let { URL(it) })
            residenciesRepo.addResidency(residency)
          }
          EntityType.UNIVERSITY -> {
            val university =
                University(
                    name = uiState.name.trim(),
                    location = location,
                    city = uiState.city.trim(),
                    email = uiState.email.trim(),
                    phone = uiState.phone.trim(),
                    websiteURL = URL(uiState.website.trim()))
            universitiesRepo.addUniversity(university)
          }
        }
        uiState = UiState(selected = uiState.selected, message = "Saved successfully!")
      } catch (e: Exception) {
        uiState = uiState.copy(isSubmitting = false, message = "Error: ${e.message ?: "unknown"}")
      }
    }
  }
}
