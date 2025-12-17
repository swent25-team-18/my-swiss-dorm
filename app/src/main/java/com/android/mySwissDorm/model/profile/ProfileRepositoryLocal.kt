package com.android.mySwissDorm.model.profile

import com.android.mySwissDorm.model.database.ProfileDao
import com.android.mySwissDorm.model.database.ProfileEntity
import com.google.firebase.auth.FirebaseAuth

/**
 * Room-backed local implementation of [ProfileRepository].
 *
 * This repository provides offline access to the current user's profile by storing it in a local
 * Room database. It implements the same interface as [ProfileRepositoryFirestore], allowing
 * seamless switching between local and remote data sources.
 *
 * Note: This repository only stores the current logged-in user's profile. Operations that require
 * other users' profiles (e.g., [getAllProfile], [getBlockedUserIds] for other users) are not
 * supported offline and will throw [UnsupportedOperationException]. Blocking users is supported
 * offline for the current user's profile.
 */
class ProfileRepositoryLocal(private val profileDao: ProfileDao, private val auth: FirebaseAuth) :
    ProfileRepository {

  /**
   * Creates a new profile in the local database.
   *
   * This method clears any existing profile before inserting the new one, ensuring only one profile
   * exists at a time (the current user's).
   *
   * @param profile The profile to create. Must have a valid ownerId that matches the current user.
   * @throws IllegalArgumentException if the profile's ownerId doesn't match the current logged-in
   *   user.
   */
  override suspend fun createProfile(profile: Profile) {
    val currentUserId =
        auth.currentUser?.uid
            ?: throw IllegalStateException(
                "ProfileRepositoryLocal: Cannot create profile. No user is currently logged in.")
    require(profile.ownerId == currentUserId) {
      "ProfileRepositoryLocal: Cannot create profile for ownerId ${profile.ownerId}. " +
          "Only the current user's profile (${currentUserId}) can be created locally."
    }

    // Clear any existing profile before inserting (handles user switching)
    profileDao.deleteAllProfiles()
    profileDao.insertProfile(ProfileEntity.fromProfile(profile))
  }

  /**
   * Retrieves a profile from the local database.
   *
   * This method only works for the current user's profile, which is the only profile stored
   * locally. If the requested ownerId doesn't match the stored profile, or if no profile exists, it
   * will throw an exception.
   *
   * @param ownerId The identifier of the profile to retrieve.
   * @return The profile with the specified identifier.
   * @throws NoSuchElementException if the profile is not found or doesn't match the requested
   *   ownerId.
   */
  override suspend fun getProfile(ownerId: String): Profile {
    val entity =
        profileDao.getUserProfile()
            ?: throw NoSuchElementException(
                "ProfileRepositoryLocal: Profile not found. No profile stored locally.")

    // Verify the stored profile matches the requested ownerId
    if (entity.ownerId != ownerId) {
      throw NoSuchElementException(
          "ProfileRepositoryLocal: Profile with ownerId $ownerId not found. " +
              "Only the current user's profile is stored locally (stored: ${entity.ownerId}).")
    }

    return entity.toProfile()
  }

  /**
   * Retrieves all profiles from the local database.
   *
   * This method is not supported in the local repository, as only the current user's profile is
   * stored locally. To get all profiles, use the remote repository or hybrid repository.
   *
   * @throws UnsupportedOperationException Always, as only one profile is stored locally.
   */
  override suspend fun getAllProfile(): List<Profile> {
    throw UnsupportedOperationException(
        "ProfileRepositoryLocal: Cannot retrieve all profiles offline. " +
            "Only the current user's profile is stored locally. Please connect to the internet.")
  }

  /**
   * Edits an existing profile in the local database.
   *
   * This method only works for the current user's profile. It verifies the profile exists before
   * updating.
   *
   * @param profile The updated profile. Must have a valid ownerId that matches the stored profile.
   * @throws NoSuchElementException if the profile is not found.
   */
  override suspend fun editProfile(profile: Profile) {
    val existingEntity =
        profileDao.getUserProfile()
            ?: throw NoSuchElementException(
                "ProfileRepositoryLocal: Profile not found. Cannot edit non-existent profile.")

    // Verify the profile being edited matches the stored profile
    require(existingEntity.ownerId == profile.ownerId) {
      "ProfileRepositoryLocal: Cannot edit profile with ownerId ${profile.ownerId}. " +
          "Only the current user's profile (${existingEntity.ownerId}) can be edited locally."
    }

    profileDao.updateProfile(ProfileEntity.fromProfile(profile))
  }

  /**
   * Deletes a profile from the local database.
   *
   * This method only works for the current user's profile. It clears all profiles from local
   * storage (since only one exists at a time).
   *
   * @param ownerId The identifier of the profile to delete.
   * @throws NoSuchElementException if the profile is not found or doesn't match the requested
   *   ownerId.
   */
  override suspend fun deleteProfile(ownerId: String) {
    val entity =
        profileDao.getUserProfile()
            ?: throw NoSuchElementException(
                "ProfileRepositoryLocal: Profile not found. Cannot delete non-existent profile.")

    // Verify the profile being deleted matches the stored profile
    if (entity.ownerId != ownerId) {
      throw NoSuchElementException(
          "ProfileRepositoryLocal: Profile with ownerId $ownerId not found. " +
              "Only the current user's profile (${entity.ownerId}) is stored locally.")
    }

    profileDao.deleteAllProfiles()
  }

  /**
   * Returns the list of blocked user IDs for the given ownerId.
   *
   * This method only works for the current user's profile, as blocked users are stored as part of
   * the profile.
   *
   * @param ownerId The identifier of the profile.
   * @return The list of blocked user IDs.
   * @throws NoSuchElementException if the profile is not found or doesn't match the requested
   *   ownerId.
   */
  override suspend fun getBlockedUserIds(ownerId: String): List<String> {
    val profile = getProfile(ownerId)
    return profile.userInfo.blockedUserIds
  }

  /**
   * Returns a map of blocked user IDs to their display names (local-only data).
   *
   * This method only works for the current user's profile. Returns the locally stored display names
   * for blocked users, which may be incomplete if names were not stored when blocking.
   *
   * @param ownerId The identifier of the profile.
   * @return A map of blocked user IDs to their display names (may be empty if names weren't
   *   stored).
   * @throws NoSuchElementException if the profile is not found or doesn't match the requested
   *   ownerId.
   */
  override suspend fun getBlockedUserNames(ownerId: String): Map<String, String> {
    val entity =
        profileDao.getUserProfile()
            ?: throw NoSuchElementException(
                "ProfileRepositoryLocal: Profile not found. Cannot get blocked user names for non-existent profile.")

    require(entity.ownerId == ownerId) {
      "ProfileRepositoryLocal: Cannot get blocked user names for profile with ownerId $ownerId. " +
          "Only the current user's profile (${entity.ownerId}) is stored locally."
    }

    return entity.blockedUserNames
  }

  /**
   * Updates a list field in UserInfo and saves the profile.
   *
   * @param ownerId The identifier of the profile.
   * @param updateUserInfo Function to create updated UserInfo with the new list value.
   */
  private suspend fun updateListField(ownerId: String, updateUserInfo: (UserInfo) -> UserInfo) {
    val profile = getProfile(ownerId)
    val updatedUserInfo = updateUserInfo(profile.userInfo)
    val updatedProfile = profile.copy(userInfo = updatedUserInfo)
    editProfile(updatedProfile)
  }

  /**
   * Adds a user to the blocked list.
   *
   * This method only works for the current user's profile. It retrieves the profile, adds the
   * blocked user, and saves it back to local storage.
   *
   * @param ownerId The identifier of the profile.
   * @param targetUid The identifier of the user to block.
   * @throws NoSuchElementException if the profile is not found or doesn't match the requested
   *   ownerId.
   */
  override suspend fun addBlockedUser(ownerId: String, targetUid: String) {
    addBlockedUserWithName(ownerId, targetUid, null)
  }

  /**
   * Adds a user to the blocked list with optional display name (internal method).
   *
   * This method only works for the current user's profile. It retrieves the profile, adds the
   * blocked user, and saves it back to local storage. If a display name is provided, it's stored
   * locally for offline access.
   *
   * @param ownerId The identifier of the profile.
   * @param targetUid The identifier of the user to block.
   * @param targetDisplayName Optional display name of the blocked user (for local storage only).
   * @throws NoSuchElementException if the profile is not found or doesn't match the requested
   *   ownerId.
   */
  suspend fun addBlockedUserWithName(
      ownerId: String,
      targetUid: String,
      targetDisplayName: String?
  ) {
    val entity =
        profileDao.getUserProfile()
            ?: throw NoSuchElementException(
                "ProfileRepositoryLocal: Profile not found. Cannot add blocked user to non-existent profile.")

    require(entity.ownerId == ownerId) {
      "ProfileRepositoryLocal: Cannot add blocked user to profile with ownerId $ownerId. " +
          "Only the current user's profile (${entity.ownerId}) can be modified locally."
    }

    val profile = entity.toProfile()
    // Only add if not already blocked to prevent duplicates
    val updatedBlockedUsers =
        if (profile.userInfo.blockedUserIds.contains(targetUid)) {
          profile.userInfo.blockedUserIds // Already blocked, return as-is
        } else {
          profile.userInfo.blockedUserIds + targetUid
        }
    val updatedUserInfo = profile.userInfo.copy(blockedUserIds = updatedBlockedUsers)
    val updatedProfile = profile.copy(userInfo = updatedUserInfo)

    // Update the entity with the new blocked user and optionally store the display name
    val updatedEntity = ProfileEntity.fromProfile(updatedProfile)
    val updatedNames =
        if (targetDisplayName != null) {
          entity.blockedUserNames + (targetUid to targetDisplayName)
        } else {
          entity.blockedUserNames
        }

    profileDao.updateProfile(updatedEntity.copy(blockedUserNames = updatedNames))
  }

  /**
   * Removes a user from the blocked list.
   *
   * This method only works for the current user's profile. It retrieves the profile, removes the
   * blocked user, and saves it back to local storage. Also removes the stored display name.
   *
   * @param ownerId The identifier of the profile.
   * @param targetUid The identifier of the user to unblock.
   * @throws NoSuchElementException if the profile is not found or doesn't match the requested
   *   ownerId.
   */
  override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {
    val entity =
        profileDao.getUserProfile()
            ?: throw NoSuchElementException(
                "ProfileRepositoryLocal: Profile not found. Cannot remove blocked user from non-existent profile.")

    require(entity.ownerId == ownerId) {
      "ProfileRepositoryLocal: Cannot remove blocked user from profile with ownerId $ownerId. " +
          "Only the current user's profile (${entity.ownerId}) can be modified locally."
    }

    val profile = entity.toProfile()
    val updatedUserInfo =
        profile.userInfo.copy(blockedUserIds = profile.userInfo.blockedUserIds - targetUid)
    val updatedProfile = profile.copy(userInfo = updatedUserInfo)

    // Update the entity and remove the stored display name
    val updatedEntity = ProfileEntity.fromProfile(updatedProfile)
    val updatedNames = entity.blockedUserNames - targetUid

    profileDao.updateProfile(updatedEntity.copy(blockedUserNames = updatedNames))
  }

  /**
   * Returns the list of bookmarked listing IDs for the given ownerId.
   *
   * This method only works for the current user's profile, as bookmarks are stored as part of the
   * profile.
   *
   * @param ownerId The identifier of the profile.
   * @return The list of bookmarked listing IDs.
   * @throws NoSuchElementException if the profile is not found or doesn't match the requested
   *   ownerId.
   */
  override suspend fun getBookmarkedListingIds(ownerId: String): List<String> {
    val profile = getProfile(ownerId)
    return profile.userInfo.bookmarkedListingIds
  }

  /**
   * Adds a listing to the bookmarked list.
   *
   * This method only works for the current user's profile. It retrieves the profile, adds the
   * bookmark, and saves it back to local storage.
   *
   * @param ownerId The identifier of the profile.
   * @param listingId The identifier of the listing to bookmark.
   * @throws NoSuchElementException if the profile is not found or doesn't match the requested
   *   ownerId.
   */
  override suspend fun addBookmark(ownerId: String, listingId: String) {
    updateListField(ownerId) { it.copy(bookmarkedListingIds = it.bookmarkedListingIds + listingId) }
  }

  /**
   * Removes a listing from the bookmarked list.
   *
   * This method only works for the current user's profile. It retrieves the profile, removes the
   * bookmark, and saves it back to local storage.
   *
   * @param ownerId The identifier of the profile.
   * @param listingId The identifier of the listing to unbookmark.
   * @throws NoSuchElementException if the profile is not found or doesn't match the requested
   *   ownerId.
   */
  override suspend fun removeBookmark(ownerId: String, listingId: String) {
    updateListField(ownerId) { it.copy(bookmarkedListingIds = it.bookmarkedListingIds - listingId) }
  }
}
