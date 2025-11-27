package com.android.mySwissDorm.ui.listing

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.ui.overview.ListingCardUI
import com.android.mySwissDorm.ui.photo.PhotoManager
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatDate
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookmarkedListingsUIState(
    val loading: Boolean = false,
    val listings: List<ListingCardUI> = emptyList(),
    val errorMsg: String? = null
)

class BookmarkedListingsViewModel(
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val rentalListingRepository: RentalListingRepository =
        RentalListingRepositoryProvider.repository,
    private val photoRepositoryCloud: PhotoRepositoryCloud =
        PhotoRepositoryProvider.cloud_repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(BookmarkedListingsUIState())
  val uiState: StateFlow<BookmarkedListingsUIState> = _uiState.asStateFlow()

  fun loadBookmarkedListings(context: Context) {
    viewModelScope.launch {
      try {
        _uiState.update { it.copy(loading = true, errorMsg = null) }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null || currentUser.isAnonymous) {
          _uiState.update { it.copy(loading = false, listings = emptyList()) }
          return@launch
        }

        val bookmarkedIds = profileRepository.getBookmarkedListingIds(currentUser.uid)

        if (bookmarkedIds.isEmpty()) {
          _uiState.update { it.copy(loading = false, listings = emptyList()) }
          return@launch
        }

        val listings = mutableListOf<ListingCardUI>()
        for (listingId in bookmarkedIds) {
          try {
            val listing = rentalListingRepository.getRentalListing(listingId)
            var imageUri: android.net.Uri? = null
            if (listing.imageUrls.isNotEmpty()) {
              val photoManager = PhotoManager(photoRepositoryCloud = photoRepositoryCloud)
              photoManager.initialize(listing.imageUrls)
              val photos = photoManager.photoLoaded
              imageUri = photos.firstOrNull()?.image
            }

            val price =
                String.format(
                    Locale.getDefault(),
                    "%.0f${context.getString(R.string.browse_city_vm_price_per_month)}",
                    listing.pricePerMonth)
            val area = "${listing.areaInM2}m²"
            val start = "${context.getString(R.string.starting)} ${formatDate(listing.startDate)}"
            val resName = listing.residencyName

            val listingCard =
                ListingCardUI(
                    title = listing.title,
                    leftBullets = listOf(listing.roomType.toString(), price, area),
                    rightBullets = listOf(start, resName),
                    listingUid = listing.uid,
                    image = imageUri,
                    location = listing.location)
            listings.add(listingCard)
          } catch (e: Exception) {
            Log.e(
                "BookmarkedListingsViewModel", "Error loading listing $listingId: ${e.message}", e)
            // Continue loading other listings even if one fails
          }
        }

        _uiState.update { it.copy(loading = false, listings = listings) }
      } catch (e: Exception) {
        Log.e("BookmarkedListingsViewModel", "Error loading bookmarked listings", e)
        _uiState.update {
          it.copy(
              loading = false,
              errorMsg =
                  "${context.getString(R.string.bookmarked_listings_failed_to_load)} ${e.message}")
        }
      }
    }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMsg = null) }
  }
}
