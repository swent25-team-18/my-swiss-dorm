package com.android.mySwissDorm.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Main Room database for the application.
 *
 * This database stores reviews and rental listings locally for offline access. It uses a singleton
 * pattern to ensure only one database instance exists throughout the application lifecycle.
 *
 * The database includes:
 * - [ReviewEntity]: Stores user reviews
 * - [RentalListingEntity]: Stores rental listings
 *
 * Use [getDatabase] to obtain the database instance.
 */
@Database(
    entities = [ReviewEntity::class, RentalListingEntity::class], version = 1, exportSchema = false)
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

  companion object {
    @Volatile private var INSTANCE: AppDatabase? = null

    /**
     * Gets the singleton instance of the database.
     *
     * This method uses double-checked locking to ensure thread-safe singleton creation. The
     * database is created with the name "app_database" and uses the application context to prevent
     * memory leaks.
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
                    .build()
            INSTANCE = instance
            instance
          }
    }
  }
}
