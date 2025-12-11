package com.android.mySwissDorm.model.profile

import android.content.Context
import android.util.Log
import com.android.mySwissDorm.model.HybridRepositoryBase
import com.android.mySwissDorm.utils.NetworkUtils

/**
 * Hybrid implementation of [ProfileRepository] that combines local and remote data sources.
 *
 * This repository provides offline-first functionality:
 * - When offline: Uses [ProfileRepositoryLocal] for all operations
 * - When online: Uses [ProfileRepositoryFirestore] for remote operations and syncs data
 *
 * For blocked users, this repository automatically syncs local and remote lists when online:
 * - When reading: Merges local and remote blocked lists (union)
 * - When writing: Saves to both local and remote (if online)
 * - Sync happens automatically when operations are performed while online
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
   * Tries remote first if online (with timeout), falls back to local on network errors or when
   * offline. Syncs successful remote results to local storage.
   *
   * @param ownerId The identifier of the profile to retrieve.
   * @return The profile with the specified identifier.
   * @throws NoSuchElementException if the profile is not found in either repository.
   */
  override suspend fun getProfile(ownerId: String): Profile =
      performRead(
          operationName = "getProfile",
          remoteCall = { remoteRepo.getProfile(ownerId) },
          localFallback = { localRepo.getProfile(ownerId) },
          syncToLocal = { profile ->
            try {
              localRepo.editProfile(profile)
            } catch (_: NoSuchElementException) {
              // Profile doesn't exist locally, try to create it
              syncProfileToLocal(profile)
            } catch (_: IllegalArgumentException) {
              // Profile exists but belongs to different user (user switched), try to create new one
              syncProfileToLocal(profile)
            }
          })

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
   * Attempts to sync a profile to local storage, catching all exceptions.
   *
   * If syncing fails (e.g., profile belongs to different user, no user logged in), logs the error
   * but doesn't throw, allowing the remote data to still be returned successfully.
   *
   * @param profile The profile to sync to local storage.
   */
  private suspend fun syncProfileToLocal(profile: Profile) {
    try {
      localRepo.createProfile(profile)
    } catch (e: Exception) {
      // If createProfile fails (e.g., ownerId mismatch, no user logged in),
      // log but don't throw - we still want to return the remote data
      Log.w(
          TAG, "Failed to sync profile to local storage (profile may belong to different user)", e)
    }
  }

  /**
   * Generic sync function for merging lists between local and remote repositories.
   *
   * Merges lists from both sources (union) and saves the merged result to both local and remote
   * storage. Only syncs if there are differences.
   *
   * @param ownerId The identifier of the profile to sync.
   * @param getLocalList Function to get the list from local repository.
   * @param getRemoteList Function to get the list from remote repository.
   * @param updateUserInfo Function to update UserInfo with the merged list.
   * @param logPrefix Prefix for log messages (e.g., "blocked users", "bookmarks").
   */
  private suspend fun syncList(
      ownerId: String,
      getLocalList: suspend (String) -> List<String>,
      getRemoteList: suspend (String) -> List<String>,
      updateUserInfo: (UserInfo, List<String>) -> UserInfo,
      logPrefix: String
  ) {
    if (!NetworkUtils.isNetworkAvailable(context)) {
      return
    }

    try {
      val localList =
          try {
            getLocalList(ownerId)
          } catch (_: Exception) {
            emptyList()
          }

      val remoteList =
          try {
            getRemoteList(ownerId)
          } catch (_: Exception) {
            emptyList()
          }

      val mergedList = (localList + remoteList).distinct()

      if (mergedList != localList || mergedList != remoteList) {
        try {
          val localProfile = localRepo.getProfile(ownerId)
          val updatedProfile =
              localProfile.copy(userInfo = updateUserInfo(localProfile.userInfo, mergedList))
          localRepo.editProfile(updatedProfile)
        } catch (e: Exception) {
          Log.w(TAG, "Failed to update local $logPrefix", e)
        }

        try {
          val remoteProfile = remoteRepo.getProfile(ownerId)
          val updatedProfile =
              remoteProfile.copy(userInfo = updateUserInfo(remoteProfile.userInfo, mergedList))
          remoteRepo.editProfile(updatedProfile)
        } catch (e: Exception) {
          Log.w(TAG, "Failed to update remote $logPrefix", e)
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to sync $logPrefix", e)
    }
  }

  /**
   * Syncs blocked users between local and remote repositories.
   *
   * @param ownerId The identifier of the profile to sync.
   */
  private suspend fun syncBlockedUsers(ownerId: String) {
    syncList(
        ownerId = ownerId,
        getLocalList = { localRepo.getBlockedUserIds(it) },
        getRemoteList = { remoteRepo.getBlockedUserIds(it) },
        updateUserInfo = { userInfo, list -> userInfo.copy(blockedUserIds = list) },
        logPrefix = "blocked users")
  }

  /**
   * Returns the list of blocked user IDs for the given profile.
   *
   * If online, syncs local and remote lists first to merge any differences. Returns the merged list
   * from local storage.
   *
   * @param ownerId The identifier of the profile.
   * @return The list of blocked user IDs, or empty list if profile not found.
   */
  override suspend fun getBlockedUserIds(ownerId: String): List<String> {
    if (NetworkUtils.isNetworkAvailable(context)) {
      syncBlockedUsers(ownerId)
    }

    return try {
      localRepo.getBlockedUserIds(ownerId)
    } catch (e: Exception) {
      emptyList()
    }
  }

  /**
   * Adds a user to the blocked list in both local and remote repositories.
   *
   * Adds to local first (offline-first), then to remote if online. Syncs after remote operation to
   * ensure consistency. If remote operation fails, the local change is preserved.
   *
   * @param ownerId The identifier of the profile.
   * @param targetUid The identifier of the user to block.
   */
  override suspend fun addBlockedUser(ownerId: String, targetUid: String) {
    localRepo.addBlockedUser(ownerId, targetUid)

    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        remoteRepo.addBlockedUser(ownerId, targetUid)
        syncBlockedUsers(ownerId)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to add blocked user remotely, but saved locally", e)
      }
    }
  }

  /**
   * Removes a user from the blocked list in both local and remote repositories.
   *
   * Removes from local first (offline-first), then from remote if online. Syncs after remote
   * operation to ensure consistency. If remote operation fails, the local change is preserved.
   *
   * @param ownerId The identifier of the profile.
   * @param targetUid The identifier of the user to unblock.
   */
  override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {
    localRepo.removeBlockedUser(ownerId, targetUid)

    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        remoteRepo.removeBlockedUser(ownerId, targetUid)
        syncBlockedUsers(ownerId)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to remove blocked user remotely, but saved locally", e)
      }
    }
  }

  /**
   * Syncs bookmarks between local and remote repositories.
   *
   * @param ownerId The identifier of the profile to sync.
   */
  private suspend fun syncBookmarks(ownerId: String) {
    syncList(
        ownerId = ownerId,
        getLocalList = { localRepo.getBookmarkedListingIds(it) },
        getRemoteList = { remoteRepo.getBookmarkedListingIds(it) },
        updateUserInfo = { userInfo, list -> userInfo.copy(bookmarkedListingIds = list) },
        logPrefix = "bookmarks")
  }

  /**
   * Returns the list of bookmarked listing IDs for the given profile.
   *
   * If online, syncs local and remote lists first to merge any differences. Returns the merged list
   * from local storage.
   *
   * @param ownerId The identifier of the profile.
   * @return The list of bookmarked listing IDs, or empty list if profile not found.
   */
  override suspend fun getBookmarkedListingIds(ownerId: String): List<String> {
    if (NetworkUtils.isNetworkAvailable(context)) {
      syncBookmarks(ownerId)
    }

    return try {
      localRepo.getBookmarkedListingIds(ownerId)
    } catch (e: Exception) {
      emptyList()
    }
  }

  /**
   * Adds a listing to the bookmarked list in both local and remote repositories.
   *
   * Adds to local first (offline-first), then to remote if online. Syncs after remote operation to
   * ensure consistency. If remote operation fails, the local change is preserved.
   *
   * @param ownerId The identifier of the profile.
   * @param listingId The identifier of the listing to bookmark.
   */
  override suspend fun addBookmark(ownerId: String, listingId: String) {
    localRepo.addBookmark(ownerId, listingId)

    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        remoteRepo.addBookmark(ownerId, listingId)
        syncBookmarks(ownerId)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to add bookmark remotely, but saved locally", e)
      }
    }
  }

  /**
   * Removes a listing from the bookmarked list in both local and remote repositories.
   *
   * Removes from local first (offline-first), then from remote if online. Syncs after remote
   * operation to ensure consistency. If remote operation fails, the local change is preserved.
   *
   * @param ownerId The identifier of the profile.
   * @param listingId The identifier of the listing to unbookmark.
   */
  override suspend fun removeBookmark(ownerId: String, listingId: String) {
    localRepo.removeBookmark(ownerId, listingId)

    if (NetworkUtils.isNetworkAvailable(context)) {
      try {
        remoteRepo.removeBookmark(ownerId, listingId)
        syncBookmarks(ownerId)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to remove bookmark remotely, but saved locally", e)
      }
    }
  }
}
