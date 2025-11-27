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
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.ui.overview.ListingCardUI
import com.android.mySwissDorm.ui.utils.BookmarkHandler
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
    val errorMsg: String? = null,
    val bookmarkedListingIds: Set<String> = emptySet()
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

  private val bookmarkHandler = BookmarkHandler(profileRepository)

  init {
    // Load bookmarked listings in init block as suggested in PR review
    // Note: We need context, so we'll call loadBookmarkedListings from the screen
    // but the logic is ready to be called from init if context is available
  }

  fun loadBookmarkedListings(context: Context) {
    viewModelScope.launch {
      try {
        _uiState.update { it.copy(loading = true, errorMsg = null) }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null || currentUser.isAnonymous) {
          _uiState.update {
            it.copy(loading = false, listings = emptyList(), bookmarkedListingIds = emptySet())
          }
          return@launch
        }

        val bookmarkedIds = profileRepository.getBookmarkedListingIds(currentUser.uid)

        if (bookmarkedIds.isEmpty()) {
          _uiState.update {
            it.copy(loading = false, listings = emptyList(), bookmarkedListingIds = emptySet())
          }
          return@launch
        }

        val listings = mutableListOf<ListingCardUI>()
        for (listingId in bookmarkedIds) {
          try {
            val listing = rentalListingRepository.getRentalListing(listingId)
            var listingCardUI = listing.toCardUI(context)

            // Only load the first image as suggested in PR review
            if (listing.imageUrls.isNotEmpty()) {
              try {
                val photo = photoRepositoryCloud.retrievePhoto(listing.imageUrls.first())
                listingCardUI = listingCardUI.copy(image = photo.image)
              } catch (_: NoSuchElementException) {
                Log.e(
                    "BookmarkedListingsViewModel",
                    "Failed to retrieve the photo ${listing.imageUrls.first()}")
              }
            }

            listings.add(listingCardUI)
          } catch (e: Exception) {
            Log.e(
                "BookmarkedListingsViewModel", "Error loading listing $listingId: ${e.message}", e)
            // Continue loading other listings even if one fails
          }
        }

        _uiState.update {
          it.copy(
              loading = false, listings = listings, bookmarkedListingIds = bookmarkedIds.toSet())
        }
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

  fun toggleBookmark(listingId: String, context: Context) {
    val currentUserId = bookmarkHandler.getCurrentUserId()
    if (currentUserId == null) {
      return
    }

    val isCurrentlyBookmarked = _uiState.value.bookmarkedListingIds.contains(listingId)
    viewModelScope.launch {
      try {
        val newBookmarkStatus =
            bookmarkHandler.toggleBookmark(
                listingId = listingId,
                currentUserId = currentUserId,
                isCurrentlyBookmarked = isCurrentlyBookmarked)

        _uiState.update { state ->
          val newBookmarkedIds =
              if (newBookmarkStatus) {
                state.bookmarkedListingIds + listingId
              } else {
                state.bookmarkedListingIds - listingId
              }
          // Reload listings to reflect the change
          loadBookmarkedListings(context)
          state.copy(bookmarkedListingIds = newBookmarkedIds)
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
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

// Reuse the same conversion logic from BrowseCityViewModel
private fun RentalListing.toCardUI(context: Context): ListingCardUI {
  val price =
      String.format(
          Locale.getDefault(),
          "%.0f${context.getString(R.string.browse_city_vm_price_per_month)}",
          pricePerMonth)
  val area = "${areaInM2}m²"
  val start = "${context.getString(R.string.starting)} ${formatDate(startDate)}"
  val resName = residencyName

  return ListingCardUI(
      title = title,
      leftBullets = listOf(roomType.toString(), price, area),
      rightBullets = listOf(start, resName),
      listingUid = uid,
      location = location)
}
