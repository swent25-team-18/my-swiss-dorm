package com.android.mySwissDorm.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main Room database for the application.
 *
 * This database stores reviews, rental listings, and the current user's profile locally for offline
 * access. It uses a singleton pattern to ensure only one database instance exists throughout the
 * application lifecycle.
 *
 * The database includes:
 * - [ReviewEntity]: Stores user reviews
 * - [RentalListingEntity]: Stores rental listings
 * - [ProfileEntity]: Stores the current user's profile
 *
 * Use [getDatabase] to obtain the database instance.
 */
@Database(
    entities = [ReviewEntity::class, RentalListingEntity::class, ProfileEntity::class],
    version = 6,
    exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
  /**
   * Returns the Data Access Object for review operations.
   *
   * @return A [ReviewDao] instance for querying and modifying reviews.
   */
  abstract fun reviewDao(): ReviewDao

  /**
   * Returns the Data Access Object for rental listing operations.
   *
   * @return A [RentalListingDao] instance for querying and modifying rental listings.
   */
  abstract fun rentalListingDao(): RentalListingDao

  /**
   * Returns the Data Access Object for profile operations.
   *
   * @return A [ProfileDao] instance for querying and modifying the current user's profile.
   */
  abstract fun profileDao(): ProfileDao

  companion object {
    @Volatile private var INSTANCE: AppDatabase? = null

    /**
     * Migration from version 1 to 2.
     *
     * Adds the `ownerName` column to both `reviews` and `rental_listings` tables. The column is
     * nullable, so existing data will have null values which is acceptable.
     */
    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
          override fun migrate(db: SupportSQLiteDatabase) {
            // Add ownerName column to reviews table
            db.execSQL("ALTER TABLE reviews ADD COLUMN ownerName TEXT")
            // Add ownerName column to rental_listings table
            db.execSQL("ALTER TABLE rental_listings ADD COLUMN ownerName TEXT")
          }
        }

    /**
     * Migration from version 2 to 3.
     *
     * Creates the `profiles` table to store the current user's profile for offline access.
     */
    private val MIGRATION_2_3 =
        object : Migration(2, 3) {
          override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS profiles (
                    ownerId TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    lastName TEXT NOT NULL,
                    email TEXT NOT NULL,
                    phoneNumber TEXT NOT NULL,
                    universityName TEXT,
                    location TEXT,
                    residencyName TEXT,
                    profilePicture TEXT,
                    minPrice REAL,
                    maxPrice REAL,
                    minSize INTEGER,
                    maxSize INTEGER,
                    preferredRoomTypes TEXT NOT NULL,
                    bookmarkedListingIds TEXT,
                    blockedUserIds TEXT,
                    language TEXT NOT NULL,
                    isPublic INTEGER NOT NULL,
                    isPushNotified INTEGER NOT NULL,
                    darkMode INTEGER
                )
                """
                    .trimIndent())
          }
        }

    /**
     * Migration from version 3 to 4.
     *
     * Fixes schema hash mismatch. The profiles table created in migration 2→3 had a hash mismatch.
     * This migration recreates the table, allowing Room to recalculate and store the correct schema
     * hash based on the current ProfileEntity definition.
     *
     * WARNING: This will delete all existing profile data. This is acceptable for development, but
     * consider data migration if users have important profile data.
     */
    private val MIGRATION_3_4 =
        object : Migration(3, 4) {
          override fun migrate(db: SupportSQLiteDatabase) {
            // Drop the existing table
            db.execSQL("DROP TABLE IF EXISTS profiles")
            // Recreate it - Room will recalculate the schema hash after this migration
            db.execSQL(
                """
                CREATE TABLE profiles (
                    ownerId TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    lastName TEXT NOT NULL,
                    email TEXT NOT NULL,
                    phoneNumber TEXT NOT NULL,
                    universityName TEXT,
                    location TEXT,
                    residencyName TEXT,
                    profilePicture TEXT,
                    minPrice REAL,
                    maxPrice REAL,
                    minSize INTEGER,
                    maxSize INTEGER,
                    preferredRoomTypes TEXT NOT NULL,
                    bookmarkedListingIds TEXT NOT NULL,
                    blockedUserIds TEXT NOT NULL,
                    language TEXT NOT NULL,
                    isPublic INTEGER NOT NULL,
                    isPushNotified INTEGER NOT NULL,
                    darkMode INTEGER
                )
                """
                    .trimIndent())
          }
        }

    /**
     * Migration from version 4 to 5.
     *
     * Adds the `blockedUserNames` column to the `profiles` table. This column stores a map of
     * blocked user IDs to their display names, used for offline access to blocked user names. The
     * column is nullable and defaults to empty (no names stored initially).
     */
    private val MIGRATION_4_5 =
        object : Migration(4, 5) {
          override fun migrate(db: SupportSQLiteDatabase) {
            // Add column as NOT NULL with default empty string
            // Empty string will be converted to emptyMap() by TypeConverter
            db.execSQL("ALTER TABLE profiles ADD COLUMN blockedUserNames TEXT NOT NULL DEFAULT ''")
          }
        }

    /**
     * Migration from version 5 to 6.
     *
     * Clears all local listings and reviews to resolve sync issues where old/deleted items from
     * Firestore were still appearing in the local database. This forces a fresh sync from Firestore
     * on the next app launch, ensuring the local database only contains current data.
     *
     * This migration addresses the issue where:
     * - Old listings/reviews that were deleted from Firestore remained in local storage
     * - When coming back online, stale data would appear before a proper refresh
     * - The sync logic only adds/updates items but doesn't remove deleted ones
     *
     * Profile data is preserved as it's user-specific and not subject to the same sync issues.
     */
    private val MIGRATION_5_6 =
        object : Migration(5, 6) {
          override fun migrate(db: SupportSQLiteDatabase) {
            // Clear all listings and reviews to force fresh sync from Firestore
            db.execSQL("DELETE FROM rental_listings")
            db.execSQL("DELETE FROM reviews")
          }
        }

    /**
     * Gets the singleton instance of the database.
     *
     * This method uses double-checked locking to ensure thread-safe singleton creation. The
     * database is created with the name "app_database" and uses the application context to prevent
     * memory leaks.
     *
     * For first-time users: Room will create a new database at version 6, and no migration will
     * run. For existing users: The appropriate migrations will run (1→2, 2→3, 3→4, 4→5, 5→6) to add
     * new columns and tables, and clear stale data.
     *
     * @param context The application context.
     * @return The [AppDatabase] instance.
     */
    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE
          ?: synchronized(this) {
            val instance =
                Room.databaseBuilder(
                        context.applicationContext, AppDatabase::class.java, "app_database")
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
            INSTANCE = instance
            instance
          }
    }
  }
}
