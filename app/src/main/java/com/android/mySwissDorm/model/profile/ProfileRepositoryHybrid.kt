package com.android.mySwissDorm.model.profile

import android.content.Context
import android.util.Log
import com.android.mySwissDorm.model.HybridRepositoryBase
import com.android.mySwissDorm.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth

/**
 * Hybrid implementation of [ProfileRepository] that combines local and remote data sources.
 *
 * This repository provides offline-first functionality:
 * - When offline: Uses [ProfileRepositoryLocal] for all operations
 * - When online: Uses [ProfileRepositoryFirestore] for remote operations and syncs data
 *
 * Sync strategy: Local is the source of truth for offline changes. When going online, the entire
 * local profile is pushed to remote, ensuring all offline changes (name, settings, bookmarks,
 * blocked users, etc.) are synced. This prevents remote data from overwriting local offline
 * changes.
 */
class ProfileRepositoryHybrid(
    context: Context,
    private val localRepo: ProfileRepositoryLocal,
    private val remoteRepo: ProfileRepositoryFirestore
) : HybridRepositoryBase<Profile>(context, "ProfileRepository"), ProfileRepository {

  /**
   * Creates a new profile in both local and remote repositories.
   *
   * Saves to local first (offline-first), then to remote if online. If remote save fails, the local
   * save is preserved.
   *
   * @param profile The profile to create.
   */
  override suspend fun createProfile(profile: Profile) {
    localRepo.createProfile(profile)

    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        remoteRepo.createProfile(profile)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to create profile remotely, but saved locally", e)
      }
    }
  }

  /**
   * Retrieves a profile from remote or local repository.
   *
   * When online: Syncs local profile to remote first (local is source of truth for offline
   * changes), then returns local profile. If local profile doesn't exist, fetches from remote and
   * syncs to local.
   *
   * When offline: Returns local profile only.
   *
   * @param ownerId The identifier of the profile to retrieve.
   * @return The profile with the specified identifier.
   * @throws NoSuchElementException if the profile is not found in either repository.
   */
  override suspend fun getProfile(ownerId: String): Profile {
    if (NetworkUtils.isNetworkAvailable(context)) {
      syncProfile(ownerId)
    }

    return try {
      localRepo.getProfile(ownerId)
    } catch (e: NoSuchElementException) {
      if (NetworkUtils.isNetworkAvailable(context)) {
        try {
          val remoteProfile = remoteRepo.getProfile(ownerId)
          // Sync remote to local for future offline access
          syncProfileToLocal(remoteProfile)
          remoteProfile
        } catch (remoteException: Exception) {
          throw e // Throw original NoSuchElementException
        }
      } else {
        throw e
      }
    }
  }

  /**
   * Retrieves all profiles from the remote repository.
   *
   * This operation requires network connectivity. If offline, calls local repository which throws
   * [UnsupportedOperationException].
   *
   * @return A list of all profiles.
   * @throws UnsupportedOperationException if offline (only current user's profile is stored
   *   locally).
   */
  override suspend fun getAllProfile(): List<Profile> {
    if (!NetworkUtils.isNetworkAvailable(context)) {
      return localRepo.getAllProfile()
    }

    return remoteRepo.getAllProfile()
  }

  /**
   * Edits an existing profile in both local and remote repositories.
   *
   * Saves to local first (offline-first), then to remote if online. If remote save fails, the local
   * save is preserved.
   *
   * @param profile The updated profile.
   */
  override suspend fun editProfile(profile: Profile) {
    localRepo.editProfile(profile)

    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        remoteRepo.editProfile(profile)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to edit profile remotely, but saved locally", e)
      }
    }
  }

  /**
   * Deletes a profile from both local and remote repositories.
   *
   * Deletes from local first (offline-first), then from remote if online. If remote delete fails,
   * the local delete is preserved.
   *
   * @param ownerId The identifier of the profile to delete.
   */
  override suspend fun deleteProfile(ownerId: String) {
    localRepo.deleteProfile(ownerId)

    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        remoteRepo.deleteProfile(ownerId)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to delete profile remotely, but deleted locally", e)
      }
    }
  }

  /**
   * Syncs the entire profile between local and remote repositories.
   *
   * Strategy: Local is the source of truth for offline changes. When going online, push local
   * profile to remote to sync all offline changes (name, settings, bookmarks, blocked users, etc.).
   *
   * IMPORTANT: Only syncs if the profile belongs to the current user.
   *
   * @param ownerId The identifier of the profile to sync.
   */
  private suspend fun syncProfile(ownerId: String) {
    if (!NetworkUtils.isNetworkAvailable(context)) {
      Log.d(TAG, "[syncProfile] No network available, skipping sync")
      return
    }

    // Only sync if it's the current user's profile
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    if (ownerId != currentUserId) {
      Log.d(
          TAG,
          "[syncProfile] Skipping sync - profile belongs to different user (ownerId: $ownerId, currentUser: $currentUserId)")
      return
    }

    try {
      val localProfile = localRepo.getProfile(ownerId)
      Log.d(TAG, "[syncProfile] Pushing local profile to remote for ownerId: $ownerId")

      // Push local profile to remote (local is source of truth for offline changes)
      try {
        remoteRepo.editProfile(localProfile)
        Log.d(TAG, "[syncProfile] Successfully pushed local profile to remote")
      } catch (e: Exception) {
        Log.w(TAG, "[syncProfile] Failed to push local profile to remote", e)
      }
    } catch (e: NoSuchElementException) {
      // Local profile doesn't exist, nothing to sync
      Log.d(TAG, "[syncProfile] Local profile doesn't exist, nothing to sync")
    } catch (e: Exception) {
      Log.w(TAG, "[syncProfile] Failed to sync profile", e)
    }
  }

  /** Checks if the given ownerId belongs to the current logged-in user. */
  private fun isCurrentUserProfile(ownerId: String): Boolean {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    return ownerId == currentUserId
  }

  /**
   * Attempts to sync a profile to local storage, catching all exceptions.
   *
   * IMPORTANT: Only syncs if the profile belongs to the current user. If syncing fails (e.g.,
   * profile belongs to different user, no user logged in), logs the error but doesn't throw,
   * allowing the remote data to still be returned successfully.
   *
   * @param profile The profile to sync to local storage.
   */
  private suspend fun syncProfileToLocal(profile: Profile) {
    // Only sync if it's the current user's profile
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    if (profile.ownerId != currentUserId) {
      Log.d(
          TAG,
          "[syncProfileToLocal] Skipping sync - profile belongs to different user (ownerId: ${profile.ownerId}, currentUser: $currentUserId)")
      return
    }

    try {
      localRepo.createProfile(profile)
    } catch (e: Exception) {
      // If createProfile fails (e.g., no user logged in),
      // log but don't throw - we still want to return the remote data
      Log.w(TAG, "Failed to sync profile to local storage", e)
    }
  }

  /**
   * Returns the list of blocked user IDs for the given profile.
   *
   * Strategy: Offline-first for current user's profile. For other users' profiles, fetches from
   * remote when online.
   *
   * @param ownerId The identifier of the profile.
   * @return The list of blocked user IDs, or empty list if profile not found.
   */
  override suspend fun getBlockedUserIds(ownerId: String): List<String> {
    val isCurrentUser = isCurrentUserProfile(ownerId)

    // For current user: offline-first approach
    if (isCurrentUser) {
      return getCurrentUserBlockedUserIds(ownerId)
    }

    // For other users: fetch from remote when online
    return getOtherUserBlockedUserIds(ownerId)
  }

  /** Gets blocked user IDs for the current user (offline-first). */
  private suspend fun getCurrentUserBlockedUserIds(ownerId: String): List<String> {
    return getCurrentUserListField(
        ownerId = ownerId,
        getLocal = { localRepo.getBlockedUserIds(it) },
        getRemote = { remoteRepo.getBlockedUserIds(it) })
  }

  /** Gets blocked user IDs for other users (remote-only). */
  private suspend fun getOtherUserBlockedUserIds(ownerId: String): List<String> {
    if (!NetworkUtils.isNetworkAvailable(context)) {
      return emptyList() // Can't fetch other users' data offline
    }

    return try {
      remoteRepo.getBlockedUserIds(ownerId)
    } catch (e: Exception) {
      emptyList()
    }
  }

  /**
   * Adds a user to the blocked list in both local and remote repositories.
   *
   * Adds to local first (offline-first), then to remote if online. If remote operation fails, the
   * local change is preserved. Fetches the target user's display name when online to store locally
   * for offline access.
   *
   * @param ownerId The identifier of the profile.
   * @param targetUid The identifier of the user to block.
   */
  override suspend fun addBlockedUser(ownerId: String, targetUid: String) {
    // Try to fetch the target user's display name if online
    var targetDisplayName: String? = null
    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        val targetProfile = remoteRepo.getProfile(targetUid)
        // Construct display name from name and lastName
        targetDisplayName =
            "${targetProfile.userInfo.name} ${targetProfile.userInfo.lastName}".trim()
      } catch (e: Exception) {
        Log.w(
            TAG, "Failed to fetch target user's profile for display name, continuing without it", e)
      }
    }

    // Ensure local profile exists before adding blocked user
    try {
      localRepo.getProfile(ownerId)
    } catch (e: NoSuchElementException) {
      // Profile doesn't exist locally - fetch from remote if online
      if (NetworkUtils.isNetworkAvailable(context)) {
        try {
          val remoteProfile = remoteRepo.getProfile(ownerId)
          localRepo.createProfile(remoteProfile)
        } catch (e2: Exception) {
          Log.w(TAG, "Failed to fetch and create local profile before adding blocked user", e2)
          throw e // Re-throw original exception
        }
      } else {
        throw e // Offline and no local profile, cannot proceed
      }
    }

    // Add to local with the display name (if available)
    if (targetDisplayName != null) {
      localRepo.addBlockedUserWithName(ownerId, targetUid, targetDisplayName)
    } else {
      localRepo.addBlockedUser(ownerId, targetUid)
    }

    // Add to remote if online
    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        remoteRepo.addBlockedUser(ownerId, targetUid)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to add blocked user remotely, but saved locally", e)
      }
    }
  }

  /**
   * Removes a user from the blocked list in both local and remote repositories.
   *
   * Removes from local first (offline-first), then from remote if online. If remote operation
   * fails, the local change is preserved.
   *
   * @param ownerId The identifier of the profile.
   * @param targetUid The identifier of the user to unblock.
   */
  override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {
    localRepo.removeBlockedUser(ownerId, targetUid)

    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        remoteRepo.removeBlockedUser(ownerId, targetUid)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to remove blocked user remotely, but saved locally", e)
      }
    }
  }

  /**
   * Returns the list of bookmarked listing IDs for the given profile.
   *
   * Strategy: Offline-first for current user's profile. For other users' profiles, fetches from
   * remote when online.
   *
   * @param ownerId The identifier of the profile.
   * @return The list of bookmarked listing IDs, or empty list if profile not found.
   */
  override suspend fun getBookmarkedListingIds(ownerId: String): List<String> {
    val isCurrentUser = isCurrentUserProfile(ownerId)

    // For current user: offline-first approach
    if (isCurrentUser) {
      return getCurrentUserBookmarkedListingIds(ownerId)
    }

    // For other users: fetch from remote when online
    return getOtherUserBookmarkedListingIds(ownerId)
  }

  /** Gets bookmarked listing IDs for the current user (offline-first). */
  private suspend fun getCurrentUserBookmarkedListingIds(ownerId: String): List<String> {
    return getCurrentUserListField(
        ownerId = ownerId,
        getLocal = { localRepo.getBookmarkedListingIds(it) },
        getRemote = { remoteRepo.getBookmarkedListingIds(it) },
        logTag = "[getBookmarkedListingIds]")
  }

  /**
   * Generic helper for getting list fields (blocked users, bookmarks) for current user. Implements
   * offline-first strategy: tries local first, syncs if online, falls back to remote.
   */
  private suspend fun getCurrentUserListField(
      ownerId: String,
      getLocal: suspend (String) -> List<String>,
      getRemote: suspend (String) -> List<String>,
      logTag: String = ""
  ): List<String> {
    // Try to get from local first
    val localIds =
        try {
          getLocal(ownerId)
        } catch (e: Exception) {
          if (logTag.isNotEmpty()) {
            Log.w(TAG, "$logTag Failed to get local data", e)
          }
          null
        }

    // If we have local data and we're online, sync to remote
    if (localIds != null && NetworkUtils.isNetworkAvailable(context)) {
      syncProfile(ownerId)
      return localIds
    }

    // If we have local data but offline, return it
    if (localIds != null) {
      return localIds
    }

    // No local data - fetch from remote if online
    if (NetworkUtils.isNetworkAvailable(context)) {
      return try {
        getRemote(ownerId)
      } catch (e: Exception) {
        if (logTag.isNotEmpty()) {
          Log.w(TAG, "$logTag Failed to get remote data", e)
        }
        emptyList()
      }
    }

    // Offline and no local data
    return emptyList()
  }

  /** Gets bookmarked listing IDs for other users (remote-only). */
  private suspend fun getOtherUserBookmarkedListingIds(ownerId: String): List<String> {
    if (!NetworkUtils.isNetworkAvailable(context)) {
      return emptyList() // Can't fetch other users' data offline
    }

    return try {
      remoteRepo.getBookmarkedListingIds(ownerId)
    } catch (e: Exception) {
      Log.w(TAG, "[getBookmarkedListingIds] Failed to get remote bookmarks", e)
      emptyList()
    }
  }

  /**
   * Adds a listing to the bookmarked list in both local and remote repositories.
   *
   * Adds to local first (offline-first), then to remote if online. If remote operation fails, the
   * local change is preserved.
   *
   * @param ownerId The identifier of the profile.
   * @param listingId The identifier of the listing to bookmark.
   */
  override suspend fun addBookmark(ownerId: String, listingId: String) {
    localRepo.addBookmark(ownerId, listingId)

    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        remoteRepo.addBookmark(ownerId, listingId)
      } catch (e: Exception) {
        Log.w(TAG, "[addBookmark] Failed to add bookmark remotely, but saved locally", e)
      }
    }
  }

  /**
   * Removes a listing from the bookmarked list in both local and remote repositories.
   *
   * Removes from local first (offline-first), then from remote if online. If remote operation
   * fails, the local change is preserved.
   *
   * @param ownerId The identifier of the profile.
   * @param listingId The identifier of the listing to unbookmark.
   */
  override suspend fun removeBookmark(ownerId: String, listingId: String) {
    localRepo.removeBookmark(ownerId, listingId)

    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        remoteRepo.removeBookmark(ownerId, listingId)
      } catch (e: Exception) {
        Log.w(TAG, "[removeBookmark] Failed to remove bookmark remotely, but saved locally", e)
      }
    }
  }
}
