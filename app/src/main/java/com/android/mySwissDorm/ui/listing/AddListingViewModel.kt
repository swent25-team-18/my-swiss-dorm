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
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.ui.InputSanitizers
import com.android.mySwissDorm.ui.InputSanitizers.FieldType
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.net.URL
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ⬇️ NEW: central sanitizers

data class AddListingUIState(
    val title: String = "",
    val residency: Residency,
    val residencies: List<Residency>,
    val price: String = "",
    val housingType: RoomType,
    val sizeSqm: String = "",
    val startDate: Timestamp,
    val description: String = "",
    val pickedImages: List<String> = emptyList(),
    val mapLat: Double? = null,
    val mapLng: Double? = null,
    val errorMsg: String? = null,
) {
  // ✅ validation against central policy
  val isFormValid: Boolean
    get() {
      val titleOk =
          InputSanitizers.validateFinal<String>(InputSanitizers.FieldType.Title, title).isValid
      val sizeOk =
          InputSanitizers.validateFinal<Double>(InputSanitizers.FieldType.RoomSize, sizeSqm).isValid
      val priceOk =
          InputSanitizers.validateFinal<Int>(InputSanitizers.FieldType.Price, price).isValid
      val descOk =
          InputSanitizers.validateFinal<String>(InputSanitizers.FieldType.Description, description)
              .isValid
      return titleOk && descOk && sizeOk && priceOk
    }
}

class AddListingViewModel(
    private val rentalListingRepository: RentalListingRepository =
        RentalListingRepositoryProvider.repository,
    private val residenciesRepository: ResidenciesRepository =
        ResidenciesRepositoryProvider.repository
) : ViewModel() {
  private val _uiState =
      MutableStateFlow(
          AddListingUIState(
              residency =
                  Residency(
                      name = "",
                      description = "",
                      location = Location(name = "Vortex", latitude = 46.5191, longitude = 6.5668),
                      city = "",
                      email = "",
                      phone = "",
                      website = URL("https://www.google.com")),
              residencies = listOf(),
              startDate = Timestamp.now(),
              housingType = RoomType.STUDIO))

  val uiState: StateFlow<AddListingUIState> = _uiState.asStateFlow()

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  // --- Repository write (unchanged) ---
  private fun addRentalListingToRepository(rentalListing: RentalListing) {
    viewModelScope.launch {
      try {
        rentalListingRepository.addRentalListing(rentalListing)
      } catch (e: Exception) {
        Log.e("AddListingViewModel", "Error adding listing", e)
        setErrorMsg("Failed to add rental listing: ${e.message}")
      }
    }
  }

  init {
    loadResidencies()
  }

  private fun loadResidencies() {
    viewModelScope.launch {
      try {
        val residencies = residenciesRepository.getAllResidencies()
        _uiState.value = _uiState.value.copy(residencies = residencies)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMsg = e.message)
      }
    }
  }

  // --- Update handlers: normalize while typing via central rules ---
  fun setTitle(title: String) {
    val norm = InputSanitizers.normalizeWhileTyping(InputSanitizers.FieldType.Title, title)
    _uiState.value = _uiState.value.copy(title = norm)
  }

  fun setResidency(residency: Residency) {
    _uiState.value = _uiState.value.copy(residency = residency)
  }

  fun setPrice(price: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Price, price)
    _uiState.value = _uiState.value.copy(price = norm)
  }

  fun setHousingType(housingType: RoomType) {
    _uiState.value = _uiState.value.copy(housingType = housingType)
  }

  fun setSizeSqm(sizeSqm: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.RoomSize, sizeSqm)
    _uiState.value = _uiState.value.copy(sizeSqm = norm)
  }

  fun setStartDate(startDate: Timestamp) {
    _uiState.value = _uiState.value.copy(startDate = startDate)
  }

  fun setDescription(description: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Description, description)
    _uiState.value = _uiState.value.copy(description = norm)
  }

  // --- Submit: strict validation + mapped types ---
  fun submitForm(onConfirm: (RentalListing) -> Unit) {
    val state = _uiState.value

    val titleRes = InputSanitizers.validateFinal<String>(FieldType.Title, state.title)
    val sizeRes = InputSanitizers.validateFinal<Double>(FieldType.RoomSize, state.sizeSqm)
    val priceRes = InputSanitizers.validateFinal<Int>(FieldType.Price, state.price)
    val descRes = InputSanitizers.validateFinal<String>(FieldType.Description, state.description)

    if (!titleRes.isValid || !sizeRes.isValid || !priceRes.isValid || !descRes.isValid) {
      setErrorMsg("At least one field is not valid")
      return
    }

    val listingToAdd =
        RentalListing(
            uid = rentalListingRepository.getNewUid(),
            ownerId = Firebase.auth.currentUser?.uid ?: "User not logged in",
            postedAt = Timestamp.now(),
            residency = state.residency,
            title = titleRes.value!!,
            roomType = state.housingType,
            pricePerMonth = priceRes.value!!.toDouble(),
            // Model expects Int; we store one-decimal UX but round for persistence
            areaInM2 = sizeRes.value!!.roundToInt(),
            startDate = state.startDate,
            description = descRes.value!!,
            imageUrls = emptyList(),
            status = RentalStatus.POSTED)

    viewModelScope.launch {
      try {
        rentalListingRepository.addRentalListing(listingToAdd)
        clearErrorMsg()
        onConfirm(listingToAdd)
      } catch (e: Exception) {
        setErrorMsg("Failed to add rental listing: ${e.message}")
      }
    }
  }
}
