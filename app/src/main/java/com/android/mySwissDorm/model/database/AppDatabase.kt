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
    version = 3,
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
          override fun migrate(database: SupportSQLiteDatabase) {
            // Add ownerName column to reviews table
            database.execSQL("ALTER TABLE reviews ADD COLUMN ownerName TEXT")
            // Add ownerName column to rental_listings table
            database.execSQL("ALTER TABLE rental_listings ADD COLUMN ownerName TEXT")
          }
        }

    /**
     * Migration from version 2 to 3.
     *
     * Creates the `profiles` table to store the current user's profile for offline access.
     */
    private val MIGRATION_2_3 =
        object : Migration(2, 3) {
          override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
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
     * Gets the singleton instance of the database.
     *
     * This method uses double-checked locking to ensure thread-safe singleton creation. The
     * database is created with the name "app_database" and uses the application context to prevent
     * memory leaks.
     *
     * For first-time users: Room will create a new database at version 3, and no migration will
     * run. For existing users: The appropriate migrations will run (1→2, 2→3) to add new columns
     * and tables.
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
            INSTANCE = instance
            instance
          }
    }
  }
}
