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
}
