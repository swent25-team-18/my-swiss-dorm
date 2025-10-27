package com.android.mySwissDorm.ui.listing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.residency.ResidencyName
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlin.String
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val defaultListing =
    RentalListing(
        uid = "",
        ownerId = "",
        postedAt = Timestamp.now(),
        title = "",
        roomType = RoomType.STUDIO,
        pricePerMonth = 0.0,
        areaInM2 = 0,
        startDate = Timestamp.now(),
        description = "",
        imageUrls = emptyList(),
        status = RentalStatus.POSTED,
        residency =
            Residency(
                name = ResidencyName.PRIVATE,
                description = "",
                location = Location(name = "", latitude = 0.0, longitude = 0.0),
                city = "Lausanne",
                email = null,
                phone = null,
                website = null))

data class ViewListingUIState(
    val listing: RentalListing = defaultListing,
    val fullNameOfPoster: String = "",
    val errorMsg: String? = null,
    val contactMessage: String = "",
    val isOwner: Boolean = false,
)

class ViewListingViewModel(
    private val rentalListingRepository: RentalListingRepository =
        RentalListingRepositoryProvider.repository,
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(ViewListingUIState())
  val uiState: StateFlow<ViewListingUIState> = _uiState.asStateFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /**
   * Loads a RentalListing by its ID and updates the UI state.
   *
   * @param listingId The ID of the RentalListing to be loaded.
   */
  fun loadListing(listingId: String) {
    viewModelScope.launch {
      try {
        val listing = rentalListingRepository.getRentalListing(listingId)
        val ownerUserInfo = profileRepository.getProfile(listing.ownerId).userInfo
        val fullNameOfPoster = ownerUserInfo.name + " " + ownerUserInfo.lastName
        val isOwner = FirebaseAuth.getInstance().currentUser?.uid == listing.ownerId
        _uiState.update {
          it.copy(listing = listing, fullNameOfPoster = fullNameOfPoster, isOwner = isOwner)
        }
      } catch (e: Exception) {
        Log.e("EditTodoViewModel", "Error loading ToDo by ID: $listingId", e)
        setErrorMsg("Failed to load Listing: ${e.message}")
      }
    }
  }

  fun setContactMessage(contactMessage: String) {
    _uiState.value = _uiState.value.copy(contactMessage = contactMessage)
  }
}
