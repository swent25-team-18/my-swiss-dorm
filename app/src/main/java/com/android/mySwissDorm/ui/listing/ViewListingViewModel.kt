package com.android.mySwissDorm.ui.listing

import android.content.Context
import android.net.Uri
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
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
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
    val showFullScreenImages: Boolean = false,
    val fullScreenImagesIndex: Int = 0,
    val isGuest: Boolean = false,
    val isBookmarked: Boolean = false,
    val hasExistingMessage: Boolean = false
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

  fun dismissFullScreenImages() {
    _uiState.value = _uiState.value.copy(showFullScreenImages = false)
  }

  fun onClickImage(uri: Uri) {
    val index = _uiState.value.images.map { it.image }.indexOf(uri)
    require(index >= 0)
    _uiState.value = _uiState.value.copy(showFullScreenImages = true, fullScreenImagesIndex = index)
  }

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
        val ownerUserInfo = profileRepository.getProfile(listing.ownerId).userInfo
        val fullNameOfPoster = ownerUserInfo.name + " " + ownerUserInfo.lastName
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid
        val isOwner = currentUserId == listing.ownerId
        val isGuest = currentUser?.isAnonymous ?: false

        // Check if the current user is blocked by the listing owner
        val isBlockedByOwner =
            if (currentUserId != null && !isOwner) {
              runCatching { profileRepository.getBlockedUserIds(listing.ownerId) }
                  .onFailure { e ->
                    Log.e("ViewListingViewModel", "Error checking blocked status", e)
                  }
                  .getOrDefault(emptyList())
                  .contains(currentUserId)
            } else {
              false
            }

        // Check if the listing is bookmarked by the current user
        val isBookmarked =
            if (currentUserId != null && !isGuest) {
              runCatching { profileRepository.getBookmarkedListingIds(currentUserId) }
                  .onFailure { e ->
                    Log.e("ViewListingViewModel", "Error checking bookmark status", e)
                  }
                  .getOrDefault(emptyList())
                  .contains(listingId)
            } else {
              false
            }

        // Check if user has already sent a message for this listing
        val hasExistingMessage =
            if (currentUserId != null && !isOwner && !isGuest) {
              runCatching {
                    requestedMessageRepository.hasExistingMessage(
                        fromUserId = currentUserId,
                        toUserId = listing.ownerId,
                        listingId = listingId)
                  }
                  .onFailure { e ->
                    Log.e("ViewListingViewModel", "Error checking existing message", e)
                  }
                  .getOrDefault(false)
            } else {
              false
            }

        photoManager.initialize(listing.imageUrls)
        val photos = photoManager.photoLoaded
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
              hasExistingMessage = hasExistingMessage)
        }
      } catch (e: Exception) {
        Log.e("ViewListingViewModel", "Error loading listing by ID: $listingId", e)
        setErrorMsg(
            "${context.getString(R.string.view_listing_failed_to_load_listings)} ${e.message}")
      }
    }
  }

  fun setContactMessage(contactMessage: String) {
    _uiState.value = _uiState.value.copy(contactMessage = contactMessage)
  }

  /**
   * Submits a contact message for the current listing. Creates a RequestedMessage that requires
   * approval from the listing owner.
   *
   * @param context Context for string resources
   * @return true if the message was successfully submitted, false otherwise
   */
  fun submitContactMessage(context: Context): Boolean {
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

    // Check if user has already sent a message for this listing
    viewModelScope.launch {
      try {
        val alreadyExists =
            requestedMessageRepository.hasExistingMessage(
                fromUserId = currentUser.uid, toUserId = listing.ownerId, listingId = listing.uid)

        if (alreadyExists) {
          Log.w(
              "ViewListingViewModel",
              "User has already sent a message for this listing. Preventing duplicate submission.")
          setErrorMsg(
              context.getString(R.string.view_listing_message_already_sent) +
                  " " +
                  context.getString(R.string.view_listing_please_wait_for_response))
          return@launch
        }

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

        // Update state to reflect that a message has been sent
        _uiState.update { it.copy(hasExistingMessage = true, contactMessage = "") }

        Log.d("ViewListingViewModel", "Contact message submitted successfully with ID: $messageId")
      } catch (e: Exception) {
        Log.e("ViewListingViewModel", "Error submitting contact message", e)
        setErrorMsg(
            "${context.getString(R.string.view_listing_failed_to_submit_message)} ${e.message}")
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
