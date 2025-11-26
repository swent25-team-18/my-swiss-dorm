package com.android.mySwissDorm.ui.listing

import android.content.Context
import android.util.Log
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
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class EditListingViewModel(
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

  override val logTag = "EditListingViewModel"

  fun getRentalListing(rentalPostID: String, context: Context) {
    viewModelScope.launch {
      try {
        val listing = rentalListingRepository.getRentalListing(rentalPostID)
        val isPrivateAccommodation = listing.residencyName == "Private Accommodation"
        val listingLocation = listing.location
        photoManager.initialize(listing.imageUrls)
        val photos = photoManager.photoLoaded

        _uiState.value =
            uiState.value.copy(
                title = listing.title,
                residencyName = listing.residencyName,
                price = listing.pricePerMonth.toString(),
                housingType = listing.roomType,
                sizeSqm = listing.areaInM2.toString(),
                startDate = listing.startDate,
                description = listing.description,
                pickedImages = photos,
                mapLat = listingLocation.latitude,
                mapLng = listingLocation.longitude,
                customLocation = if (isPrivateAccommodation) listingLocation else null,
                customLocationQuery = if (isPrivateAccommodation) listingLocation.name else "",
                errorMsg = null)
      } catch (e: Exception) {
        Log.e(logTag, "Error loading listing by ID: $rentalPostID", e)
        setErrorMsg(
            "${context.getString(R.string.edit_listing_vm_failed_to_load_listings)} ${e.message}")
      }
    }
  }

  fun editRentalListing(id: String, context: Context): Boolean {
    val state = uiState.value
    if (!state.isFormValid) {
      setErrorMsg(context.getString(R.string.edit_listing_vm_at_least_one_field_not_valid))
      return false
    }

    val uid =
        Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated to edit a listing")

    val location = resolveLocationOrNull() ?: return false

    val listing =
        RentalListing(
            uid = id,
            ownerId = uid,
            postedAt = Timestamp.now(),
            residencyName = state.residencyName,
            title = state.title,
            roomType = state.housingType,
            pricePerMonth = state.price.toDouble(),
            areaInM2 = state.sizeSqm.toDouble().toInt(),
            startDate = state.startDate,
            description = state.description,
            imageUrls = state.pickedImages.map { it.fileName },
            status = RentalStatus.POSTED,
            location = location)

    viewModelScope.launch {
      try {
        rentalListingRepository.editRentalListing(rentalPostId = id, newValue = listing)
      } catch (e: Exception) {
        Log.e(logTag, "Error editing listing", e)
        setErrorMsg(
            "${context.getString(R.string.edit_listing_vm_failed_to_edit_listings)} ${e.message}")
      }
    }
    viewModelScope.launch {
      // TODO changes when implementing sync
      photoManager.commitChanges()
    }
    clearErrorMsg()
    return true
  }

  fun deleteRentalListing(rentalPostID: String, context: Context) {
    viewModelScope.launch {
      photoManager.deleteAll()
      try {
        rentalListingRepository.deleteRentalListing(rentalPostId = rentalPostID)
      } catch (e: Exception) {
        Log.e(logTag, "Error deleting listing", e)
        setErrorMsg(
            "${context.getString(R.string.edit_listing_vm_failed_to_delete_listing)} ${e.message}")
      }
    }
  }
}
