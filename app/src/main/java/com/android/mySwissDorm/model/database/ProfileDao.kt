package com.android.mySwissDorm.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object for [ProfileEntity] operations.
 *
 * Provides methods to query, insert, update, and delete profiles from the local database.
 *
 * Note: The local database only stores the current logged-in user's profile for offline access.
 * When a new user logs in, the previous user's profile should be cleared before inserting the new
 * one. This is handled in the repository layer by calling [deleteAllProfiles] before
 * [insertProfile].
 */
@Dao
interface ProfileDao {
  /**
   * Retrieves the current user's profile from the database.
   *
   * Since only one profile is stored locally at a time (the current user's), this method returns
   * the single profile if it exists.
   *
   * @return The [ProfileEntity] for the current user, or null if not found.
   */
  @Query("SELECT * FROM profiles LIMIT 1") suspend fun getUserProfile(): ProfileEntity?

  /**
   * Inserts a profile into the database.
   *
   * If a profile with the same [ProfileEntity.ownerId] already exists, it will be replaced.
   *
   * Note: To ensure only one profile exists (when switching users), call [deleteAllProfiles] before
   * this method in the repository layer.
   *
   * @param profile The [ProfileEntity] to insert.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProfile(profile: ProfileEntity)

  /**
   * Updates an existing profile in the database.
   *
   * @param profile The [ProfileEntity] to update. Must have an existing [ProfileEntity.ownerId].
   */
  @Update suspend fun updateProfile(profile: ProfileEntity)

  /**
   * Deletes all profiles from the database.
   *
   * This should be called:
   * - When a user logs out (to clear their profile)
   * - Before inserting a new user's profile (to ensure only one profile exists)
   */
  @Query("DELETE FROM profiles") suspend fun deleteAllProfiles()
}
