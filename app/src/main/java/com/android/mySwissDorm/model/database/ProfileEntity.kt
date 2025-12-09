package com.android.mySwissDorm.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.Language
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.model.rental.RoomType

/**
 * Room entity representing a user profile in the local database.
 *
 * This entity is used to store the current user's profile offline. It can be converted to and from
 * the domain [Profile] model using [toProfile] and [fromProfile].
 *
 * The entity flattens the nested [UserInfo] and [UserSettings] structures into a single table for
 * Room database storage.
 *
 * @property ownerId Unique identifier of the user (primary key).
 * @property name User's first name.
 * @property lastName User's last name.
 * @property email User's email address.
 * @property phoneNumber User's phone number.
 * @property universityName Optional name of the user's university.
 * @property location Optional preferred location (stored using TypeConverter).
 * @property residencyName Optional name of the user's residency.
 * @property profilePicture Optional URL or path to the user's profile picture.
 * @property minPrice Optional minimum price preference.
 * @property maxPrice Optional maximum price preference.
 * @property minSize Optional minimum size preference in square meters.
 * @property maxSize Optional maximum size preference in square meters.
 * @property preferredRoomTypes List of preferred room types (stored as comma-separated enum names).
 * @property bookmarkedListingIds List of bookmarked listing IDs (stored using TypeConverter).
 * @property language User's preferred language (stored as enum name string).
 * @property isPublic Whether the user's profile is public.
 * @property isPushNotified Whether the user has push notifications enabled.
 * @property darkMode Optional dark mode preference (null means follow system).
 */
@Entity(tableName = "profiles")
@TypeConverters(Converters::class)
data class ProfileEntity(
    @PrimaryKey val ownerId: String,
    // UserInfo fields
    val name: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val universityName: String? = null,
    val location: Location? = null,
    val residencyName: String? = null,
    val profilePicture: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minSize: Int? = null,
    val maxSize: Int? = null,
    val preferredRoomTypes: String, // Comma-separated enum names (e.g., "STUDIO,APARTMENT")
    val bookmarkedListingIds: List<String> = emptyList(),
    // UserSettings fields
    val language: String, // Enum name (e.g., "ENGLISH", "FRENCH")
    val isPublic: Boolean = false,
    val isPushNotified: Boolean = true,
    val darkMode: Boolean? = null, // null means follow system
) {
  /**
   * Converts this entity to a domain [Profile] model.
   *
   * @return A [Profile] object with the same data as this entity.
   */
  fun toProfile(): Profile {
    // Parse preferredRoomTypes from comma-separated string
    val roomTypes =
        if (preferredRoomTypes.isBlank()) {
          emptyList()
        } else {
          preferredRoomTypes.split(",").mapNotNull { typeName ->
            try {
              RoomType.valueOf(typeName.trim())
            } catch (e: IllegalArgumentException) {
              null // Skip invalid enum values
            }
          }
        }

    // Parse language enum
    val languageEnum =
        try {
          Language.valueOf(language)
        } catch (e: IllegalArgumentException) {
          Language.ENGLISH // Default fallback
        }

    val userInfo =
        UserInfo(
            name = name,
            lastName = lastName,
            email = email,
            phoneNumber = phoneNumber,
            universityName = universityName,
            location = location,
            residencyName = residencyName,
            profilePicture = profilePicture,
            minPrice = minPrice,
            maxPrice = maxPrice,
            minSize = minSize,
            maxSize = maxSize,
            preferredRoomTypes = roomTypes,
            bookmarkedListingIds = bookmarkedListingIds)

    val userSettings =
        UserSettings(
            language = languageEnum,
            isPublic = isPublic,
            isPushNotified = isPushNotified,
            darkMode = darkMode)

    return Profile(userInfo = userInfo, userSettings = userSettings, ownerId = ownerId)
  }

  companion object {
    /**
     * Creates a [ProfileEntity] from a domain [Profile] model.
     *
     * @param profile The domain profile model to convert.
     * @return A [ProfileEntity] with the same data as the profile.
     */
    fun fromProfile(profile: Profile): ProfileEntity {
      // Convert preferredRoomTypes to comma-separated string
      val roomTypesString = profile.userInfo.preferredRoomTypes.joinToString(",") { it.name }

      return ProfileEntity(
          ownerId = profile.ownerId,
          name = profile.userInfo.name,
          lastName = profile.userInfo.lastName,
          email = profile.userInfo.email,
          phoneNumber = profile.userInfo.phoneNumber,
          universityName = profile.userInfo.universityName,
          location = profile.userInfo.location,
          residencyName = profile.userInfo.residencyName,
          profilePicture = profile.userInfo.profilePicture,
          minPrice = profile.userInfo.minPrice,
          maxPrice = profile.userInfo.maxPrice,
          minSize = profile.userInfo.minSize,
          maxSize = profile.userInfo.maxSize,
          preferredRoomTypes = roomTypesString,
          bookmarkedListingIds = profile.userInfo.bookmarkedListingIds,
          language = profile.userSettings.language.name,
          isPublic = profile.userSettings.isPublic,
          isPushNotified = profile.userSettings.isPushNotified,
          darkMode = profile.userSettings.darkMode)
    }
  }
}
