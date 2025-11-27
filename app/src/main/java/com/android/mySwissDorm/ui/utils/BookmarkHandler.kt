package com.android.mySwissDorm.ui.utils

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Handler for bookmark operations to avoid logic duplication across ViewModels.
 *
 * This handler provides a reusable way to toggle bookmark status for listings, ensuring consistent
 * behavior and easier testing.
 */
class BookmarkHandler(
    private val profileRepository: ProfileRepository,
    private val viewModelScope: kotlinx.coroutines.CoroutineScope
) {
  /**
   * Toggles the bookmark status for a listing.
   *
   * @param listingId The ID of the listing to bookmark/unbookmark
   * @param currentUserId The ID of the current user
   * @param isCurrentlyBookmarked Whether the listing is currently bookmarked
   * @param onSuccess Callback invoked when the bookmark toggle succeeds, with the new bookmark
   *   status
   * @param onError Callback invoked when an error occurs, with the error message
   */
  fun toggleBookmark(
      listingId: String,
      currentUserId: String,
      isCurrentlyBookmarked: Boolean,
      onSuccess: (Boolean) -> Unit,
      onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        if (isCurrentlyBookmarked) {
          profileRepository.removeBookmark(currentUserId, listingId)
        } else {
          profileRepository.addBookmark(currentUserId, listingId)
        }
        onSuccess(!isCurrentlyBookmarked)
      } catch (e: Exception) {
        Log.e("BookmarkHandler", "Error toggling bookmark", e)
        onError("${e.message}")
      }
    }
  }

  /**
   * Gets the current user ID, checking if the user is authenticated and not anonymous.
   *
   * @return The current user ID, or null if the user is not authenticated or is anonymous
   */
  fun getCurrentUserId(): String? {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid
    return if (currentUserId != null && !currentUser.isAnonymous) {
      currentUserId
    } else {
      null
    }
  }
}
