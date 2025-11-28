package com.android.mySwissDorm.ui.utils

import android.util.Log
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.google.firebase.auth.FirebaseAuth

/**
 * Handler for bookmark operations to avoid logic duplication across ViewModels.
 *
 * This handler provides a reusable way to toggle bookmark status for listings, ensuring consistent
 * behavior and easier testing.
 */
class BookmarkHandler(private val profileRepository: ProfileRepository) {
  /**
   * Toggles the bookmark status for a listing.
   *
   * @param listingId The ID of the listing to bookmark/unbookmark
   * @param currentUserId The ID of the current user
   * @param isCurrentlyBookmarked Whether the listing is currently bookmarked
   * @return The new bookmark status (true if bookmarked, false if unbookmarked)
   * @throws Exception if the bookmark operation fails
   */
  suspend fun toggleBookmark(
      listingId: String,
      currentUserId: String,
      isCurrentlyBookmarked: Boolean
  ): Boolean {
    try {
      if (isCurrentlyBookmarked) {
        profileRepository.removeBookmark(currentUserId, listingId)
      } else {
        profileRepository.addBookmark(currentUserId, listingId)
      }
      return !isCurrentlyBookmarked
    } catch (e: Exception) {
      Log.e("BookmarkHandler", "Error toggling bookmark", e)
      throw e
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
