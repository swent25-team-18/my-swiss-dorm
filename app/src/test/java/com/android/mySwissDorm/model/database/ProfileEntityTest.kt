package com.android.mySwissDorm.model.database

import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.Language
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.model.rental.RoomType
import org.junit.Assert.*
import org.junit.Test

class ProfileEntityTest {
  @Test
  fun fromProfile_createsEntityCorrectly() {
    val profile = createTestProfile()
    val entity = ProfileEntity.fromProfile(profile)

    assertEquals(profile.ownerId, entity.ownerId)
    assertEquals(profile.userInfo.name, entity.name)
    assertEquals(profile.userInfo.lastName, entity.lastName)
    assertEquals(profile.userInfo.email, entity.email)
    assertEquals(profile.userInfo.phoneNumber, entity.phoneNumber)
    assertEquals(profile.userInfo.universityName, entity.universityName)
    assertEquals(profile.userInfo.location, entity.location)
    assertEquals(profile.userInfo.residencyName, entity.residencyName)
    assertEquals(profile.userInfo.profilePicture, entity.profilePicture)
    assertEquals(profile.userInfo.minPrice, entity.minPrice)
    assertEquals(profile.userInfo.maxPrice, entity.maxPrice)
    assertEquals(profile.userInfo.minSize, entity.minSize)
    assertEquals(profile.userInfo.maxSize, entity.maxSize)
    assertEquals(
        profile.userInfo.preferredRoomTypes.joinToString(",") { it.name },
        entity.preferredRoomTypes)
    assertEquals(profile.userInfo.bookmarkedListingIds, entity.bookmarkedListingIds)
    assertEquals(profile.userSettings.language.name, entity.language)
    assertEquals(profile.userSettings.isPublic, entity.isPublic)
    assertEquals(profile.userSettings.isPushNotified, entity.isPushNotified)
    assertEquals(profile.userSettings.darkMode, entity.darkMode)
  }

  @Test
  fun toProfile_createsProfileCorrectly() {
    val entity = createTestProfileEntity()
    val profile = entity.toProfile()

    assertEquals(entity.ownerId, profile.ownerId)
    assertEquals(entity.name, profile.userInfo.name)
    assertEquals(entity.lastName, profile.userInfo.lastName)
    assertEquals(entity.email, profile.userInfo.email)
    assertEquals(entity.phoneNumber, profile.userInfo.phoneNumber)
    assertEquals(entity.universityName, profile.userInfo.universityName)
    assertEquals(entity.location, profile.userInfo.location)
    assertEquals(entity.residencyName, profile.userInfo.residencyName)
    assertEquals(entity.profilePicture, profile.userInfo.profilePicture)
    assertEquals(entity.minPrice, profile.userInfo.minPrice)
    assertEquals(entity.maxPrice, profile.userInfo.maxPrice)
    assertEquals(entity.minSize, profile.userInfo.minSize)
    assertEquals(entity.maxSize, profile.userInfo.maxSize)
    assertEquals(
        entity.preferredRoomTypes.split(",").map { RoomType.valueOf(it.trim()) },
        profile.userInfo.preferredRoomTypes)
    assertEquals(entity.bookmarkedListingIds, profile.userInfo.bookmarkedListingIds)
    assertEquals(Language.valueOf(entity.language), profile.userSettings.language)
    assertEquals(entity.isPublic, profile.userSettings.isPublic)
    assertEquals(entity.isPushNotified, profile.userSettings.isPushNotified)
    assertEquals(entity.darkMode, profile.userSettings.darkMode)
  }

  @Test
  fun roundTrip_preservesAllFields() {
    val originalProfile = createTestProfile()
    val entity = ProfileEntity.fromProfile(originalProfile)
    val convertedProfile = entity.toProfile()

    assertEquals(originalProfile.ownerId, convertedProfile.ownerId)
    assertEquals(originalProfile.userInfo.name, convertedProfile.userInfo.name)
    assertEquals(originalProfile.userInfo.lastName, convertedProfile.userInfo.lastName)
    assertEquals(originalProfile.userInfo.email, convertedProfile.userInfo.email)
    assertEquals(originalProfile.userInfo.phoneNumber, convertedProfile.userInfo.phoneNumber)
    assertEquals(originalProfile.userInfo.universityName, convertedProfile.userInfo.universityName)
    val originalLocation = originalProfile.userInfo.location
    val convertedLocation = convertedProfile.userInfo.location
    assertEquals(originalLocation?.name, convertedLocation?.name)
    requireNotNull(originalLocation)
    requireNotNull(convertedLocation)
    assertEquals(originalLocation.latitude, convertedLocation.latitude, 0.0001)
    assertEquals(originalLocation.longitude, convertedLocation.longitude, 0.0001)
    assertEquals(originalProfile.userInfo.residencyName, convertedProfile.userInfo.residencyName)
    assertEquals(originalProfile.userInfo.profilePicture, convertedProfile.userInfo.profilePicture)
    assertEquals(originalProfile.userInfo.minPrice, convertedProfile.userInfo.minPrice)
    assertEquals(originalProfile.userInfo.maxPrice, convertedProfile.userInfo.maxPrice)
    assertEquals(originalProfile.userInfo.minSize, convertedProfile.userInfo.minSize)
    assertEquals(originalProfile.userInfo.maxSize, convertedProfile.userInfo.maxSize)
    assertEquals(
        originalProfile.userInfo.preferredRoomTypes, convertedProfile.userInfo.preferredRoomTypes)
    assertEquals(
        originalProfile.userInfo.bookmarkedListingIds,
        convertedProfile.userInfo.bookmarkedListingIds)
    assertEquals(originalProfile.userSettings.language, convertedProfile.userSettings.language)
    assertEquals(originalProfile.userSettings.isPublic, convertedProfile.userSettings.isPublic)
    assertEquals(
        originalProfile.userSettings.isPushNotified, convertedProfile.userSettings.isPushNotified)
    assertEquals(originalProfile.userSettings.darkMode, convertedProfile.userSettings.darkMode)
  }

  @Test
  fun toProfile_handlesEmptyPreferredRoomTypes() {
    val entity = createTestProfileEntity().copy(preferredRoomTypes = "")
    val profile = entity.toProfile()

    assertTrue(profile.userInfo.preferredRoomTypes.isEmpty())
  }

  @Test
  fun toProfile_handlesInvalidRoomType() {
    val entity =
        createTestProfileEntity().copy(preferredRoomTypes = "STUDIO,INVALID_TYPE,APARTMENT")
    val profile = entity.toProfile()

    assertEquals(2, profile.userInfo.preferredRoomTypes.size)
    assertTrue(profile.userInfo.preferredRoomTypes.contains(RoomType.STUDIO))
    assertTrue(profile.userInfo.preferredRoomTypes.contains(RoomType.APARTMENT))
    assertFalse(profile.userInfo.preferredRoomTypes.contains(RoomType.COLOCATION))
  }

  @Test
  fun toProfile_handlesInvalidLanguage() {
    val entity = createTestProfileEntity().copy(language = "INVALID_LANGUAGE")
    val profile = entity.toProfile()

    assertEquals(Language.ENGLISH, profile.userSettings.language)
  }

  @Test
  fun toProfile_handlesEmptyBookmarkedListingIds() {
    val entity = createTestProfileEntity().copy(bookmarkedListingIds = emptyList())
    val profile = entity.toProfile()

    assertTrue(profile.userInfo.bookmarkedListingIds.isEmpty())
  }

  @Test
  fun fromProfile_handlesAllRoomTypes() {
    val allRoomTypes = RoomType.entries
    val originalProfile = createTestProfile()
    val profile =
        originalProfile.copy(
            userInfo = originalProfile.userInfo.copy(preferredRoomTypes = allRoomTypes))
    val entity = ProfileEntity.fromProfile(profile)
    val converted = entity.toProfile()

    assertEquals(allRoomTypes, converted.userInfo.preferredRoomTypes)
  }

  @Test
  fun fromProfile_handlesAllLanguages() {
    Language.entries.forEach { language ->
      val originalProfile = createTestProfile()
      val profile =
          originalProfile.copy(
              userSettings = originalProfile.userSettings.copy(language = language))
      val entity = ProfileEntity.fromProfile(profile)
      val converted = entity.toProfile()

      assertEquals(language, converted.userSettings.language)
    }
  }

  @Test
  fun fromProfile_handlesNullOptionalFields() {
    val originalProfile = createTestProfile()
    val profile =
        originalProfile.copy(
            userInfo =
                originalProfile.userInfo.copy(
                    universityName = null,
                    location = null,
                    residencyName = null,
                    profilePicture = null,
                    minPrice = null,
                    maxPrice = null,
                    minSize = null,
                    maxSize = null))
    val entity = ProfileEntity.fromProfile(profile)
    val converted = entity.toProfile()

    assertNull(converted.userInfo.universityName)
    assertNull(converted.userInfo.location)
    assertNull(converted.userInfo.residencyName)
    assertNull(converted.userInfo.profilePicture)
    assertNull(converted.userInfo.minPrice)
    assertNull(converted.userInfo.maxPrice)
    assertNull(converted.userInfo.minSize)
    assertNull(converted.userInfo.maxSize)
  }

  @Test
  fun fromProfile_handlesNullDarkMode() {
    val originalProfile = createTestProfile()
    val profile =
        originalProfile.copy(userSettings = originalProfile.userSettings.copy(darkMode = null))
    val entity = ProfileEntity.fromProfile(profile)
    val converted = entity.toProfile()

    assertNull(converted.userSettings.darkMode)
  }

  @Test
  fun toProfile_handlesWhitespaceInPreferredRoomTypes() {
    val entity =
        createTestProfileEntity().copy(preferredRoomTypes = " STUDIO , APARTMENT , COLOCATION ")
    val profile = entity.toProfile()

    assertEquals(3, profile.userInfo.preferredRoomTypes.size)
    assertTrue(profile.userInfo.preferredRoomTypes.contains(RoomType.STUDIO))
    assertTrue(profile.userInfo.preferredRoomTypes.contains(RoomType.APARTMENT))
    assertTrue(profile.userInfo.preferredRoomTypes.contains(RoomType.COLOCATION))
  }

  private fun createTestProfile(): Profile {
    return Profile(
        userInfo =
            UserInfo(
                name = "John",
                lastName = "Doe",
                email = "john.doe@example.com",
                phoneNumber = "+1234567890",
                universityName = "EPFL",
                location = Location("Lausanne", 46.5197, 6.6323),
                residencyName = "Vortex",
                profilePicture = "profile.jpg",
                minPrice = 500.0,
                maxPrice = 1000.0,
                minSize = 20,
                maxSize = 50,
                preferredRoomTypes = listOf(RoomType.STUDIO, RoomType.APARTMENT),
                bookmarkedListingIds = listOf("listing-1", "listing-2")),
        userSettings =
            UserSettings(
                language = Language.ENGLISH,
                isPublic = true,
                isPushNotified = true,
                darkMode = false),
        ownerId = "user-1")
  }

  private fun createTestProfileEntity(): ProfileEntity {
    return ProfileEntity(
        ownerId = "user-1",
        name = "John",
        lastName = "Doe",
        email = "john.doe@example.com",
        phoneNumber = "+1234567890",
        universityName = "EPFL",
        location = Location("Lausanne", 46.5197, 6.6323),
        residencyName = "Vortex",
        profilePicture = "profile.jpg",
        minPrice = 500.0,
        maxPrice = 1000.0,
        minSize = 20,
        maxSize = 50,
        preferredRoomTypes = "STUDIO,APARTMENT",
        bookmarkedListingIds = listOf("listing-1", "listing-2"),
        language = "ENGLISH",
        isPublic = true,
        isPushNotified = true,
        darkMode = false)
  }
}
