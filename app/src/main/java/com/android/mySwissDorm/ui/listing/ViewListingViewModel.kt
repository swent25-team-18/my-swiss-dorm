package com.android.mySwissDorm.ui.listing

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.requestedmessage.MessageStatus
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessage
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepository
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepositoryProvider
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import com.android.mySwissDorm.model.poi.DistanceService
import com.android.mySwissDorm.model.poi.POIDistance
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.supermarket.SupermarketsRepositoryProvider
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.android.mySwissDorm.ui.photo.PhotoManager
import com.android.mySwissDorm.ui.utils.BookmarkHandler
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
        residencyName = "",
        location = Location(name = "", latitude = 0.0, longitude = 0.0))

data class ViewListingUIState(
    val listing: RentalListing = defaultListing,
    val fullNameOfPoster: String = "",
    val errorMsg: String? = null,
    val contactMessage: String = "",
    val isOwner: Boolean = false,
    val isBlockedByOwner: Boolean = false,
    val locationOfListing: Location = Location(name = "", latitude = 0.0, longitude = 0.0),
    val images: List<Photo> = emptyList(),
    val isGuest: Boolean = false,
    val isBookmarked: Boolean = false,
    val poiDistances: List<POIDistance> = emptyList()
)

class ViewListingViewModel(
    private val rentalListingRepository: RentalListingRepository =
        RentalListingRepositoryProvider.repository,
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val residenciesRepository: ResidenciesRepository =
        ResidenciesRepositoryProvider.repository,
    private val photoRepositoryCloud: PhotoRepositoryCloud =
        PhotoRepositoryProvider.cloud_repository,
    private val requestedMessageRepository: RequestedMessageRepository =
        RequestedMessageRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(ViewListingUIState())
  val uiState: StateFlow<ViewListingUIState> = _uiState.asStateFlow()

  val photoManager = PhotoManager(photoRepositoryCloud = photoRepositoryCloud)
  private val bookmarkHandler = BookmarkHandler(profileRepository)

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  fun setLocationOfListing(rentalUid: String) {
    viewModelScope.launch {
      try {
        val listing = rentalListingRepository.getRentalListing(rentalUid)
        // Use location directly from the listing (now stored in the model)
        _uiState.value = _uiState.value.copy(locationOfListing = listing.location)
      } catch (e: Exception) {
        Log.e(
            "ViewListingViewModel",
            "Failed to load location, this is expected if listing is new or missing.",
            e)
      }
    }
  }
  /**
   * Loads a RentalListing by its ID and updates the UI state.
   *
   * @param listingId The ID of the RentalListing to be loaded.
   */
  fun loadListing(listingId: String, context: Context) {
    viewModelScope.launch {
      try {
        val listing = rentalListingRepository.getRentalListing(listingId)
        val fullNameOfPoster = loadOwnerInfo(listing)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid
        val isOwner = currentUserId == listing.ownerId
        val isGuest = currentUser?.isAnonymous ?: false

        val isBlockedByOwner = checkIfBlockedByOwner(listing, currentUserId, isOwner)
        val isBookmarked = checkIfBookmarked(listingId, currentUserId, isGuest)
        val photos = loadPhotos(listing)
        val userUniversityName = getUserUniversityName(currentUserId, isGuest)
        val poiDistances = calculatePOIDistances(listing, userUniversityName)

        updateUIState(
            listing = listing,
            fullNameOfPoster = fullNameOfPoster,
            isOwner = isOwner,
            isBlockedByOwner = isBlockedByOwner,
            photos = photos,
            isGuest = isGuest,
            isBookmarked = isBookmarked,
            poiDistances = poiDistances)
      } catch (e: Exception) {
        Log.e("ViewListingViewModel", "Error loading listing by ID: $listingId", e)
        setErrorMsg(
            "${context.getString(R.string.view_listing_failed_to_load_listings)} ${e.message}")
      }
    }
  }

  private suspend fun loadOwnerInfo(listing: RentalListing): String {
    val ownerUserInfo = profileRepository.getProfile(listing.ownerId).userInfo
    return ownerUserInfo.name + " " + ownerUserInfo.lastName
  }

  private suspend fun checkIfBlockedByOwner(
      listing: RentalListing,
      currentUserId: String?,
      isOwner: Boolean
  ): Boolean {
    if (currentUserId == null || isOwner) {
      return false
    }
    return runCatching { profileRepository.getBlockedUserIds(listing.ownerId) }
        .onFailure { e -> Log.e("ViewListingViewModel", "Error checking blocked status", e) }
        .getOrDefault(emptyList())
        .contains(currentUserId)
  }

  private suspend fun checkIfBookmarked(
      listingId: String,
      currentUserId: String?,
      isGuest: Boolean
  ): Boolean {
    if (currentUserId == null || isGuest) {
      return false
    }
    return runCatching { profileRepository.getBookmarkedListingIds(currentUserId) }
        .onFailure { e -> Log.e("ViewListingViewModel", "Error checking bookmark status", e) }
        .getOrDefault(emptyList())
        .contains(listingId)
  }

  private suspend fun loadPhotos(listing: RentalListing): List<Photo> {
    photoManager.initialize(listing.imageUrls)
    return photoManager.photoLoaded
  }

  private suspend fun getUserUniversityName(currentUserId: String?, isGuest: Boolean): String? {
    if (currentUserId == null || isGuest) {
      return null
    }
    return try {
      val userProfile = profileRepository.getProfile(currentUserId)
      userProfile.userInfo.universityName
    } catch (e: Exception) {
      Log.d(
          "ViewListingViewModel",
          "Could not get user profile for university, showing 2 nearest universities",
          e)
      null
    }
  }

  private suspend fun calculatePOIDistances(
      listing: RentalListing,
      userUniversityName: String?
  ): List<POIDistance> {
    return try {
      val distanceService =
          DistanceService(
              universitiesRepository = UniversitiesRepositoryProvider.repository,
              supermarketsRepository = SupermarketsRepositoryProvider.repository,
              walkingRouteService =
                  com.android.mySwissDorm.model.map.WalkingRouteServiceProvider.service)
      val distances = distanceService.calculateDistancesToPOIs(listing.location, userUniversityName)
      Log.d(
          "ViewListingViewModel",
          "Calculated ${distances.size} POI distances for listing ${listing.uid}")
      Log.d(
          "ViewListingViewModel",
          "Listing location: lat=${listing.location.latitude}, lng=${listing.location.longitude}")
      if (userUniversityName != null) {
        Log.d("ViewListingViewModel", "User's university: $userUniversityName")
      } else {
        Log.d("ViewListingViewModel", "User has no university in profile - showing 2 nearest")
      }
      distances
    } catch (e: Exception) {
      Log.e("ViewListingViewModel", "Error calculating POI distances", e)
      emptyList()
    }
  }

  private fun updateUIState(
      listing: RentalListing,
      fullNameOfPoster: String,
      isOwner: Boolean,
      isBlockedByOwner: Boolean,
      photos: List<Photo>,
      isGuest: Boolean,
      isBookmarked: Boolean,
      poiDistances: List<POIDistance>
  ) {
    _uiState.update {
      it.copy(
          listing = listing,
          fullNameOfPoster = fullNameOfPoster,
          isOwner = isOwner,
          isBlockedByOwner = isBlockedByOwner,
          locationOfListing = listing.location,
          images = photos,
          isGuest = isGuest,
          isBookmarked = isBookmarked,
          poiDistances = poiDistances)
    }
  }

  fun setContactMessage(contactMessage: String) {
    _uiState.value = _uiState.value.copy(contactMessage = contactMessage)
  }

  /**
   * Submits a contact message for the current listing. Creates a RequestedMessage that requires
   * approval from the listing owner.
   *
   * @return true if the message was successfully submitted, false otherwise
   */
  fun submitContactMessage(): Boolean {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val listing = _uiState.value.listing
    val contactMessage = _uiState.value.contactMessage.trim()

    if (currentUser == null || currentUser.isAnonymous) {
      Log.e("ViewListingViewModel", "User not authenticated or is anonymous")
      return false
    }

    if (contactMessage.isBlank()) {
      Log.e("ViewListingViewModel", "Contact message is blank")
      return false
    }

    if (listing.uid.isBlank() || listing.ownerId.isBlank()) {
      Log.e("ViewListingViewModel", "Listing ID or owner ID is blank")
      return false
    }

    if (currentUser.uid == listing.ownerId) {
      Log.e("ViewListingViewModel", "User cannot send message to themselves")
      return false
    }

    viewModelScope.launch {
      try {
        // Generate a unique ID for the message
        val messageId = requestedMessageRepository.getNewUid()
        val requestedMessage =
            RequestedMessage(
                id = messageId,
                fromUserId = currentUser.uid,
                toUserId = listing.ownerId,
                listingId = listing.uid,
                listingTitle = listing.title,
                message = contactMessage,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.PENDING)

        requestedMessageRepository.createRequestedMessage(requestedMessage)
        Log.d("ViewListingViewModel", "Contact message submitted successfully with ID: $messageId")
      } catch (e: Exception) {
        Log.e("ViewListingViewModel", "Error submitting contact message", e)
      }
    }

    return true
  }

  fun toggleBookmark(listingId: String, context: Context) {
    val currentUserId = bookmarkHandler.getCurrentUserId()
    if (currentUserId == null) {
      return
    }

    val isCurrentlyBookmarked = _uiState.value.isBookmarked
    viewModelScope.launch {
      try {
        val newBookmarkStatus =
            bookmarkHandler.toggleBookmark(
                listingId = listingId,
                currentUserId = currentUserId,
                isCurrentlyBookmarked = isCurrentlyBookmarked)

        _uiState.update { it.copy(isBookmarked = newBookmarkStatus) }
      } catch (e: Exception) {
        setErrorMsg(
            "${context.getString(R.string.view_listing_failed_to_toggle_bookmark)} ${e.message}")
      }
    }
  }
}
