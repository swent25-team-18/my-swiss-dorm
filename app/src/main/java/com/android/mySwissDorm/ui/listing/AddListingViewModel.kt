package com.android.mySwissDorm.ui.listing

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
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
import com.android.mySwissDorm.ui.utils.BaseLocationSearchViewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddListingUIState(
    val title: String = "",
    val residencies: List<Residency>,
    val residencyName: String = "",
    val price: String = "",
    val housingType: RoomType,
    val sizeSqm: String = "",
    val startDate: Timestamp,
    val description: String = "",
    val pickedImages: List<String> = emptyList(),
    val mapLat: Double? = null,
    val mapLng: Double? = null,
    val errorMsg: String? = null,
    val customLocationQuery: String = "",
    val customLocation: Location? = null,
    val locationSuggestions: List<Location> = emptyList(),
    val showCustomLocationDialog: Boolean = false,
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

      // because residencies are predefined, no validation needed. We just need to check that the
      // field is not empty.
      val residencyOk = residencyName.isNotEmpty()

      return titleOk && descOk && sizeOk && priceOk && residencyOk
    }
}

class AddListingViewModel(
    private val rentalListingRepository: RentalListingRepository =
        RentalListingRepositoryProvider.repository,
    private val residenciesRepository: ResidenciesRepository =
        ResidenciesRepositoryProvider.repository,
    override val locationRepository: LocationRepository = LocationRepositoryProvider.repository
) : BaseLocationSearchViewModel() {
  override val logTag = "AddListingViewModel"
  private val _uiState =
      MutableStateFlow(
          AddListingUIState(
              residencies = listOf(), startDate = Timestamp.now(), housingType = RoomType.STUDIO))

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

  fun setResidency(residencyName: String) {
    val currentState = _uiState.value
    val isPrivateAccommodation = residencyName == "Private Accommodation"

    if (isPrivateAccommodation) {
      // For Private Accommodation, keep custom location if already set, otherwise clear it
      _uiState.value =
          currentState.copy(
              residencyName = residencyName,
              // Keep custom location if it exists, otherwise it stays null
          )
    } else {
      // For regular residencies, set location from the residency
      val residency = currentState.residencies.find { it.name == residencyName }
      _uiState.value =
          currentState.copy(
              residencyName = residencyName,
              customLocation = null, // Clear custom location for regular residencies
              customLocationQuery = "",
              mapLat = residency?.location?.latitude,
              mapLng = residency?.location?.longitude)
    }
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

  // --- Custom location methods (BaseLocationSearchViewModel) ---
  override fun updateStateWithQuery(query: String) {
    _uiState.value = _uiState.value.copy(customLocationQuery = query)
  }

  override fun updateStateWithSuggestions(suggestions: List<Location>) {
    _uiState.value = _uiState.value.copy(locationSuggestions = suggestions)
  }

  override fun updateStateWithLocation(location: Location) {
    _uiState.value =
        _uiState.value.copy(
            customLocation = location,
            customLocationQuery = location.name,
            mapLat = location.latitude,
            mapLng = location.longitude)
  }

  override fun updateStateShowDialog(currentLocation: Location?) {
    _uiState.value =
        _uiState.value.copy(
            showCustomLocationDialog = true,
            customLocation = currentLocation,
            customLocationQuery = currentLocation?.name ?: "")
  }

  override fun updateStateDismissDialog() {
    _uiState.value =
        _uiState.value.copy(
            showCustomLocationDialog = false, customLocationQuery = "", customLocation = null)
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

    // Determine location: use custom location for Private Accommodation, otherwise use residency
    // location
    val location =
        if (state.residencyName == "Private Accommodation") {
          if (state.customLocation != null) {
            state.customLocation
          } else if (state.mapLat != null && state.mapLng != null) {
            // Fallback: create location from mapLat/mapLng if customLocation is null
            Location(
                name = state.customLocationQuery.ifEmpty { "Custom Location" },
                latitude = state.mapLat,
                longitude = state.mapLng)
          } else {
            setErrorMsg("Please select a location for Private Accommodation")
            return
          }
        } else {
          // For regular residencies, get location from the residency
          val residency = state.residencies.find { it.name == state.residencyName }
          if (residency != null) {
            residency.location
          } else if (state.mapLat != null && state.mapLng != null) {
            // Fallback: use mapLat/mapLng if residency not found
            Location(name = state.residencyName, latitude = state.mapLat, longitude = state.mapLng)
          } else {
            setErrorMsg("Could not determine location for the selected residency")
            return
          }
        }

    val listingToAdd =
        RentalListing(
            uid = rentalListingRepository.getNewUid(),
            ownerId = Firebase.auth.currentUser?.uid ?: "User not logged in",
            postedAt = Timestamp.now(),
            residencyName = state.residencyName,
            title = titleRes.value!!,
            roomType = state.housingType,
            pricePerMonth = priceRes.value!!.toDouble(),
            // Model expects Int; we store one-decimal UX but round for persistence
            areaInM2 = sizeRes.value!!.roundToInt(),
            startDate = state.startDate,
            description = descRes.value!!,
            imageUrls = emptyList(),
            status = RentalStatus.POSTED,
            location = location)

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
