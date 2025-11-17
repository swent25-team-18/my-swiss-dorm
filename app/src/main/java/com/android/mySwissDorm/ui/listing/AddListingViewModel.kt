package com.android.mySwissDorm.ui.listing

import androidx.lifecycle.viewModelScope
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
import com.android.mySwissDorm.ui.utils.AbstractAddFormViewModel
import com.android.mySwissDorm.ui.utils.AddFormUiState
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddListingUIState(
    val title: String = "",
    override val residencies: List<Residency>,
    val residencyName: String = "",
    val price: String = "",
    val housingType: RoomType,
    val sizeSqm: String = "",
    val startDate: Timestamp,
    val description: String = "",
    val pickedImages: List<String> = emptyList(),
    val mapLat: Double? = null,
    val mapLng: Double? = null,
    override val errorMsg: String? = null
) : AddFormUiState {
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
    residenciesRepository: ResidenciesRepository = ResidenciesRepositoryProvider.repository
) :
    AbstractAddFormViewModel<AddListingUIState>(
        residenciesRepository,
        AddListingUIState(
            residencies = listOf(), startDate = Timestamp.now(), housingType = RoomType.STUDIO)) {

  override fun updateStateWithError(errorMsg: String?) {
    _uiState.update { it.copy(errorMsg = errorMsg) }
  }

  override fun updateStateWithResidencies(residencies: List<Residency>) {
    _uiState.update { it.copy(residencies = residencies) }
  }

  // --- Update handlers: normalize while typing via central rules ---
  fun setTitle(title: String) {
    val norm = InputSanitizers.normalizeWhileTyping(FieldType.Title, title)
    _uiState.value = _uiState.value.copy(title = norm)
  }

  fun setResidency(residencyName: String) {
    _uiState.value = _uiState.value.copy(residencyName = residencyName)
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
            residencyName = state.residencyName,
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
