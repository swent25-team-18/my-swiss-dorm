package com.android.mySwissDorm.model.profile

import com.android.mySwissDorm.model.database.ProfileDao
import com.android.mySwissDorm.model.database.ProfileEntity

/**
 * Room-backed local implementation of [ProfileRepository].
 *
 * This repository provides offline access to the current user's profile by storing it in a local
 * Room database. It implements the same interface as [ProfileRepositoryFirestore], allowing
 * seamless switching between local and remote data sources.
 *
 * Note: This repository only stores the current logged-in user's profile. Operations that require
 * other users' profiles (e.g., [getAllProfile], [getBlockedUserIds] for other users) are not
 * supported offline and will throw [UnsupportedOperationException].
 */
class ProfileRepositoryLocal(private val profileDao: ProfileDao) : ProfileRepository {

  /**
   * Creates a new profile in the local database.
   *
   * This method clears any existing profile before inserting the new one, ensuring only one profile
   * exists at a time (the current user's).
   *
   * @param profile The profile to create. Must have a valid ownerId.
   */
  override suspend fun createProfile(profile: Profile) {
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
    if (existingEntity.ownerId != profile.ownerId) {
      throw IllegalArgumentException(
          "ProfileRepositoryLocal: Cannot edit profile with ownerId ${profile.ownerId}. " +
              "Only the current user's profile (${existingEntity.ownerId}) can be edited locally.")
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
   * This method only works for the current user's profile. Blocked users are not stored locally as
   * they are not part of the Profile entity structure.
   *
   * @throws UnsupportedOperationException Always, as blocked users are not stored locally.
   */
  override suspend fun getBlockedUserIds(ownerId: String): List<String> {
    throw UnsupportedOperationException(
        "ProfileRepositoryLocal: Cannot retrieve blocked users offline. " +
            "Blocked users are not stored locally. Please connect to the internet.")
  }

  /**
   * Adds a user to the blocked list.
   *
   * This method is not supported in the local repository, as blocked users are not stored locally.
   *
   * @throws UnsupportedOperationException Always, as blocked users cannot be managed offline.
   */
  override suspend fun addBlockedUser(ownerId: String, targetUid: String) {
    throw UnsupportedOperationException(
        "ProfileRepositoryLocal: Cannot block users offline. " +
            "Please connect to the internet to manage blocked users.")
  }

  /**
   * Removes a user from the blocked list.
   *
   * This method is not supported in the local repository, as blocked users are not stored locally.
   *
   * @throws UnsupportedOperationException Always, as blocked users cannot be managed offline.
   */
  override suspend fun removeBlockedUser(ownerId: String, targetUid: String) {
    throw UnsupportedOperationException(
        "ProfileRepositoryLocal: Cannot unblock users offline. " +
            "Please connect to the internet to manage blocked users.")
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
    val profile = getProfile(ownerId)
    val updatedBookmarks = profile.userInfo.bookmarkedListingIds + listingId
    val updatedUserInfo = profile.userInfo.copy(bookmarkedListingIds = updatedBookmarks)
    val updatedProfile = profile.copy(userInfo = updatedUserInfo)
    editProfile(updatedProfile)
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
    val profile = getProfile(ownerId)
    val updatedBookmarks = profile.userInfo.bookmarkedListingIds - listingId
    val updatedUserInfo = profile.userInfo.copy(bookmarkedListingIds = updatedBookmarks)
    val updatedProfile = profile.copy(userInfo = updatedUserInfo)
    editProfile(updatedProfile)
  }
}
