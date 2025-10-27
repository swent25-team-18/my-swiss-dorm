package com.android.mySwissDorm.ui.listing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.net.URL
import kotlin.let
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddListingUIState(
    val title: String = "",
    val residency: Residency,
    val price: String = "",
    val housingType: RoomType,
    val sizeSqm: String = "",
    val startDate: Timestamp,
    val description: String = "",
    val pickedImages: List<String> = emptyList(),
    val mapLat: Double? = null,
    val mapLng: Double? = null,
    val errorMsg: String? = null,
    val residencies: List<Residency> = emptyList(),
) {
  val isFormValid: Boolean
    get() {
      val sizeOk = sizeSqm.toIntOrNull()?.let { it in 1 ..< 1000 } == true
      val priceOk = price.toDoubleOrNull()?.let { it in 1.0 ..< 10000.0 } == true
      return title.isNotEmpty() && description.isNotEmpty() && sizeOk && priceOk
    }
}

class AddListingViewModel(
    private val repository: RentalListingRepository = RentalListingRepositoryProvider.repository
) : ViewModel() {
  private val _uiState =
      MutableStateFlow(
          AddListingUIState(
              residency =
                  Residency(
                      name = "Vortex",
                      description = "",
                      location = Location(name = "Vortex", latitude = 46.5191, longitude = 6.5668),
                      city = "Lausanne",
                      email = "",
                      phone = "",
                      website = URL("https://www.google.com")),
              startDate = Timestamp.now(),
              housingType = RoomType.STUDIO))

  val uiState: StateFlow<AddListingUIState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      _uiState.update {
        it.copy(residencies = ResidenciesRepositoryProvider.repository.getAllResidencies())
      }
    }
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  // Validation logic
  private fun addRentalListingToRepository(rentalListing: RentalListing) {
    viewModelScope.launch {
      try {
        repository.addRentalListing(rentalListing)
      } catch (e: Exception) {
        Log.e("AddToDoViewModel", "Error adding ToDo", e)
        setErrorMsg("Failed to add rental listing: ${e.message}")
      }
    }
  }

  fun addRentalListingState(): Boolean {
    val state = _uiState.value
    if (!state.isFormValid) {
      setErrorMsg("At least one field is not valid")
      return false
    }
    val uid = Firebase.auth.currentUser?.uid ?: "User not logged in"
    addRentalListingToRepository(
        RentalListing(
            uid = repository.getNewUid(),
            ownerId = uid,
            postedAt = Timestamp.now(),
            residency = state.residency,
            title = state.title,
            roomType = state.housingType,
            pricePerMonth = state.price.toDouble(),
            areaInM2 = state.sizeSqm.toInt(),
            startDate = state.startDate,
            description = state.description,
            imageUrls = emptyList(), // Image upload not implemented yet
            status = RentalStatus.POSTED))
    clearErrorMsg()
    return true
  }

  fun setTitle(title: String) {
    _uiState.value = _uiState.value.copy(title = title)
  }

  fun setResidency(residency: Residency) {
    _uiState.value = _uiState.value.copy(residency = residency)
  }

  fun setPrice(price: String) {
    _uiState.value = _uiState.value.copy(price = price)
  }

  fun setHousingType(housingType: RoomType) {
    _uiState.value = _uiState.value.copy(housingType = housingType)
  }

  fun setSizeSqm(sizeSqm: String) {
    _uiState.value = _uiState.value.copy(sizeSqm = sizeSqm)
  }

  fun setStartDate(startDate: Timestamp) {
    _uiState.value = _uiState.value.copy(startDate = startDate)
  }

  fun setDescription(description: String) {
    _uiState.value = _uiState.value.copy(description = description)
  }

  fun submitForm(onConfirm: (RentalListing) -> Unit) {
    val state = _uiState.value
    if (!state.isFormValid) {
      setErrorMsg("At least one field is not valid")
      return
    }
    val listingToAdd =
        RentalListing(
            uid = repository.getNewUid(),
            ownerId = Firebase.auth.currentUser?.uid ?: "User not logged in",
            postedAt = Timestamp.now(),
            residency = state.residency,
            title = state.title,
            roomType = state.housingType,
            pricePerMonth = state.price.toDouble(),
            areaInM2 = state.sizeSqm.toInt(),
            startDate = state.startDate,
            description = state.description,
            imageUrls = emptyList(), // Image upload not implemented yet
            status = RentalStatus.POSTED)

    viewModelScope.launch {
      try {
        repository.addRentalListing(listingToAdd)
        clearErrorMsg()
        onConfirm(listingToAdd)
      } catch (e: Exception) {
        setErrorMsg("Failed to add rental listing: ${e.message}")
      }
    }
  }
}
