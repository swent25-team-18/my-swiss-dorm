package com.android.mySwissDorm.ui.listing

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.map.LocationRepository
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.photo.PhotoRepository
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.ui.InputSanitizers
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class AddListingViewModel(
    rentalListingRepository: RentalListingRepository = RentalListingRepositoryProvider.repository,
    residenciesRepository: ResidenciesRepository = ResidenciesRepositoryProvider.repository,
    locationRepository: LocationRepository = LocationRepositoryProvider.repository,
    photoRepositoryLocal: PhotoRepository = PhotoRepositoryProvider.local_repository,
    photoRepositoryCloud: PhotoRepositoryCloud = PhotoRepositoryProvider.cloud_repository
) :
    BaseListingFormViewModel(
        rentalListingRepository = rentalListingRepository,
        residenciesRepository = residenciesRepository,
        locationRepository = locationRepository,
        photoRepositoryLocal = photoRepositoryLocal,
        photoRepositoryCloud = photoRepositoryCloud) {

  init {
    // For Add, residency must be chosen
    _uiState.value = _uiState.value.copy(requireResidencyName = true)
  }

  fun submitForm(onConfirm: (RentalListing) -> Unit, context: Context) {
    val state = uiState.value

    // Prevent duplicate submissions
    if (state.isSubmitting) {
      return
    }

    // will probably never reach this if but it's just here for security
    if ((FirebaseAuth.getInstance().currentUser?.isAnonymous ?: true)) {
      setErrorMsg(context.getString(R.string.add_listing_vm_guests_cannot_create_listings))
      return
    }
    if (!state.isFormValid) {
      setErrorMsg(context.getString(R.string.add_listing_vm_at_least_one_field_not_valid))
      return
    }

    val location = resolveLocationOrNull() ?: return

    val titleRes =
        InputSanitizers.validateFinal<String>(InputSanitizers.FieldType.Title, state.title)
    val sizeRes =
        InputSanitizers.validateFinal<Double>(InputSanitizers.FieldType.RoomSize, state.sizeSqm)
    val priceRes = InputSanitizers.validateFinal<Int>(InputSanitizers.FieldType.Price, state.price)
    val descRes =
        InputSanitizers.validateFinal<String>(
            InputSanitizers.FieldType.Description, state.description)

    val listingToAdd =
        RentalListing(
            uid = rentalListingRepository.getNewUid(),
            ownerId = Firebase.auth.currentUser?.uid ?: "User not logged in",
            postedAt = Timestamp.now(),
            residencyName = state.residencyName,
            title = titleRes.value!!,
            roomType = state.housingType,
            pricePerMonth = priceRes.value!!.toDouble(),
            areaInM2 = sizeRes.value!!.roundToInt(),
            startDate = state.startDate,
            description = descRes.value!!,
            imageUrls = state.pickedImages.map { it.fileName },
            status = RentalStatus.POSTED,
            location = location)

    // Mark as submitting
    _uiState.value = _uiState.value.copy(isSubmitting = true)

    viewModelScope.launch {
      try {
        rentalListingRepository.addRentalListing(listingToAdd)
        photoManager.commitChanges()
        clearErrorMsg()
        onConfirm(listingToAdd)
      } catch (e: Exception) {
        setErrorMsg(
            "${context.getString(R.string.add_listing_vm_failed_to_add_listing)} ${e.message}")
        // Reset submitting state on error so user can retry
        _uiState.value = _uiState.value.copy(isSubmitting = false)
      }
    }
  }
}
