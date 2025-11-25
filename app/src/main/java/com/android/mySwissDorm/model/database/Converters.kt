package com.android.mySwissDorm.model.database

import androidx.room.TypeConverter
import com.android.mySwissDorm.model.map.Location
import com.google.firebase.Timestamp

/**
 * Type converters for Room database.
 *
 * This class provides conversion methods between domain model types and database-compatible types.
 * Room uses these converters automatically when storing and retrieving entities.
 */
class Converters {
  /**
   * Converts a Long value (milliseconds since epoch) to a Firebase Timestamp.
   *
   * @param value The timestamp in milliseconds, or null.
   * @return A Firebase Timestamp object, or null if the input was null.
   */
  @TypeConverter
  fun fromTimestamp(value: Long?): Timestamp? {
    return value?.let {
      val seconds = it / 1000
      val nanoseconds = ((it % 1000) * 1_000_000).toInt()
      Timestamp(seconds, nanoseconds)
    }
  }

  /**
   * Converts a Firebase Timestamp to a Long value (milliseconds since epoch).
   *
   * @param timestamp The Firebase Timestamp to convert, or null.
   * @return The timestamp in milliseconds, or null if the input was null.
   */
  @TypeConverter
  fun timestampToLong(timestamp: Timestamp?): Long? {
    return timestamp?.let { it.seconds * 1000L + (it.nanoseconds / 1_000_000) }
  }

  /**
   * Converts a Location object to a pipe-delimited string format.
   *
   * Format: "name|latitude|longitude"
   *
   * @param location The Location object to convert, or null.
   * @return A string representation of the location, or null if the input was null.
   */
  @TypeConverter
  fun fromLocation(location: Location?): String? {
    return location?.let { "${it.name}|${it.latitude}|${it.longitude}" }
  }

  /**
   * Converts a pipe-delimited string to a Location object.
   *
   * Expected format: "name|latitude|longitude"
   *
   * @param value The string representation of the location, or null.
   * @return A Location object, or null if the input was null, empty, or invalid.
   */
  @TypeConverter
  fun toLocation(value: String?): Location? {
    return value?.split("|")?.let { parts ->
      if (parts.size == 3) {
        try {
          Location(name = parts[0], latitude = parts[1].toDouble(), longitude = parts[2].toDouble())
        } catch (e: NumberFormatException) {
          null
        }
      } else {
        null
      }
    }
  }

  /**
   * Converts a List of Strings to a comma-delimited string.
   *
   * @param value The list of strings to convert, or null.
   * @return A comma-delimited string, or null if the input was null or empty.
   */
  @TypeConverter
  fun fromStringList(value: List<String>?): String? {
    return value?.joinToString(",")
  }

  /**
   * Converts a comma-delimited string to a List of Strings.
   *
   * @param value The comma-delimited string to convert, or null.
   * @return A list of strings, or an empty list if the input was null or empty.
   */
  @TypeConverter
  fun toStringList(value: String?): List<String>? {
    return if (value.isNullOrEmpty()) emptyList() else value.split(",")
  }
}
