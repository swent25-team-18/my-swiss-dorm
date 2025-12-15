package com.android.mySwissDorm.model.profile

/** Handles profile operations */
interface ProfileRepository {

  /**
   * Creates a new profile in the application
   *
   * @param profile the new profile
   */
  suspend fun createProfile(profile: Profile)

  /**
   * Retrieves a new profile
   *
   * @param ownerId the identifier of the profile
   * @return the profile
   * @throws Exception if the uid is not associated with any profile
   */
  suspend fun getProfile(ownerId: String): Profile

  /**
   * Retrieves all profiles
   *
   * @return a list of [Profile] registered to the app.
   */
  suspend fun getAllProfile(): List<Profile>

  /**
   * Edits an existing profile
   *
   * @throws Exception if the given [Profile] does not exist
   */
  suspend fun editProfile(profile: Profile)

  /**
   * Delete an existing profile
   *
   * @throws Exception if the given [Profile does not exist]
   */
  suspend fun deleteProfile(ownerId: String)

  /** Returns the list of blocked user ids for the given [ownerId]. */
  suspend fun getBlockedUserIds(ownerId: String): List<String>

  /**
   * Returns a map of blocked user IDs to their display names (local-only data).
   *
   * This method returns locally stored display names for blocked users, which may be incomplete if
   * names were not stored when blocking. Returns empty map if no names are stored.
   *
   * @param ownerId The identifier of the profile.
   * @return A map of blocked user IDs to their display names (may be empty if names weren't
   *   stored).
   */
  suspend fun getBlockedUserNames(ownerId: String): Map<String, String>

  /** Adds [targetUid] to the blocked list of [ownerId]. */
  suspend fun addBlockedUser(ownerId: String, targetUid: String)

  /** Removes [targetUid] from the blocked list of [ownerId]. */
  suspend fun removeBlockedUser(ownerId: String, targetUid: String)

  /** Returns the list of bookmarked listing ids for the given [ownerId]. */
  suspend fun getBookmarkedListingIds(ownerId: String): List<String>

  /** Adds [listingId] to the bookmarked list of [ownerId]. */
  suspend fun addBookmark(ownerId: String, listingId: String)

  /** Removes [listingId] from the bookmarked list of [ownerId]. */
  suspend fun removeBookmark(ownerId: String, listingId: String)
}
