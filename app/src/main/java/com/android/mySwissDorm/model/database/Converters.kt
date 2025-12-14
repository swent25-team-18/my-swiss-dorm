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

  /**
   * Converts a Map<String, String> to a pipe-delimited string format.
   *
   * Format: "key1:value1|key2:value2|..." Uses URL encoding for keys and values to handle special
   * characters (:, |, etc.)
   *
   * @param value The map to convert, or null.
   * @return A string representation of the map, or null if the input was null or empty.
   */
  @TypeConverter
  fun fromStringMap(value: Map<String, String>?): String? {
    if (value.isNullOrEmpty()) return null
    return value.entries.joinToString("|") {
      "${java.net.URLEncoder.encode(it.key, "UTF-8")}:${java.net.URLEncoder.encode(it.value, "UTF-8")}"
    }
  }

  /**
   * Converts a pipe-delimited string to a Map<String, String>.
   *
   * Expected format: "key1:value1|key2:value2|..." Keys and values are URL decoded to handle
   * special characters.
   *
   * @param value The string representation of the map, or null.
   * @return A map, or an empty map if the input was null, empty, or invalid.
   */
  @TypeConverter
  fun toStringMap(value: String?): Map<String, String>? {
    return if (value.isNullOrEmpty()) {
      emptyMap()
    } else {
      try {
        value
            .split("|")
            .mapNotNull { entry ->
              val parts = entry.split(":", limit = 2)
              if (parts.size == 2) {
                val key = java.net.URLDecoder.decode(parts[0], "UTF-8")
                val decodedValue = java.net.URLDecoder.decode(parts[1], "UTF-8")
                key to decodedValue
              } else {
                null
              }
            }
            .toMap()
      } catch (e: Exception) {
        emptyMap() // Return empty map on parsing error
      }
    }
  }
}
