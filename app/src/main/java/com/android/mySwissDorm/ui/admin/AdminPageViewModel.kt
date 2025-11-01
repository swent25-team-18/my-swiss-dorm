package com.android.mySwissDorm.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.city.CitiesRepository
import com.android.mySwissDorm.model.city.CitiesRepositoryProvider
import com.android.mySwissDorm.model.city.City
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.university.UniversitiesRepository
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.android.mySwissDorm.model.university.University
import java.net.URL
import kotlinx.coroutines.launch

class AdminPageViewModel(
    private val citiesRepo: CitiesRepository = CitiesRepositoryProvider.repository,
    private val residenciesRepo: ResidenciesRepository = ResidenciesRepositoryProvider.repository,
    private val universitiesRepo: UniversitiesRepository =
        UniversitiesRepositoryProvider.repository,
) : ViewModel() {

  enum class EntityType {
    CITY,
    RESIDENCY,
    UNIVERSITY
  }

  data class UiState(
      val selected: EntityType = EntityType.CITY,
      val name: String = "",
      val locName: String = "",
      val longitude: String = "",
      val latitude: String = "",
      val description: String = "",
      val imageId: String = "",
      val city: String = "",
      val email: String = "",
      val phone: String = "",
      val website: String = "",
      val isSubmitting: Boolean = false,
      val message: String? = null
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

  fun onLocName(v: String) {
    uiState = uiState.copy(locName = v)
  }

  fun onLongitude(v: String) {
    uiState = uiState.copy(longitude = v)
  }

  fun onLatitude(v: String) {
    uiState = uiState.copy(latitude = v)
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

  private fun parseLocation(): Location? {
    val name = uiState.locName.trim()
    val lon = uiState.longitude.trim().toDoubleOrNull()
    val lat = uiState.latitude.trim().toDoubleOrNull()
    if (name.isBlank() || lon == null || lat == null) return null
    return Location(name = name, longitude = lon, latitude = lat)
  }

  private fun validate(): String? {
    if (uiState.name.isBlank()) return "Name is required."
    if (parseLocation() == null) return "Location (name, longitude, latitude) is required."

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
    // Parse location after validation and make sure to not return a null value with "!!"
    val location = parseLocation()!!

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
