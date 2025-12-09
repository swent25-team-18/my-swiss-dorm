package com.android.mySwissDorm.model.database

import androidx.room.Room
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.Language
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProfileDaoTest {
  private lateinit var database: AppDatabase
  private lateinit var profileDao: ProfileDao

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    profileDao = database.profileDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun insertProfile_insertsSuccessfully() = runTest {
    val entity = createTestProfileEntity("user-1")
    profileDao.insertProfile(entity)

    val retrieved = profileDao.getUserProfile()
    assertNotNull(retrieved)
    assertEquals("user-1", retrieved!!.ownerId)
    assertEquals("John", retrieved.name)
  }

  @Test
  fun getUserProfile_returnsNullWhenNotFound() = runTest {
    val result = profileDao.getUserProfile()
    assertNull(result)
  }

  @Test
  fun getUserProfile_returnsOnlyProfile() = runTest {
    val entity1 = createTestProfileEntity("user-1", name = "John")
    val entity2 = createTestProfileEntity("user-2", name = "Jane")

    // Insert first profile
    profileDao.insertProfile(entity1)
    var retrieved = profileDao.getUserProfile()
    assertNotNull(retrieved)
    assertEquals("user-1", retrieved!!.ownerId)
    assertEquals("John", retrieved.name)

    // Delete all profiles and insert second profile (simulating repository behavior)
    // REPLACE strategy only works when primary keys match, so we need to delete first
    profileDao.deleteAllProfiles()
    profileDao.insertProfile(entity2)
    retrieved = profileDao.getUserProfile()
    assertNotNull(retrieved)
    assertEquals("user-2", retrieved!!.ownerId)
    assertEquals("Jane", retrieved.name)
  }

  @Test
  fun insertProfile_replacesOnConflict() = runTest {
    val entity1 = createTestProfileEntity("user-1", name = "John")
    profileDao.insertProfile(entity1)

    val entity2 = createTestProfileEntity("user-1", name = "John Updated")
    profileDao.insertProfile(entity2)

    val retrieved = profileDao.getUserProfile()
    assertNotNull(retrieved)
    assertEquals("user-1", retrieved!!.ownerId)
    assertEquals("John Updated", retrieved.name)
  }

  @Test
  fun updateProfile_updatesExistingProfile() = runTest {
    val entity = createTestProfileEntity("user-1", name = "John")
    profileDao.insertProfile(entity)

    val updated = entity.copy(name = "John Updated", lastName = "Doe Updated")
    profileDao.updateProfile(updated)

    val retrieved = profileDao.getUserProfile()
    assertNotNull(retrieved)
    assertEquals("John Updated", retrieved!!.name)
    assertEquals("Doe Updated", retrieved.lastName)
  }

  @Test
  fun deleteAllProfiles_deletesAllProfiles() = runTest {
    val entity1 = createTestProfileEntity("user-1")
    val entity2 = createTestProfileEntity("user-2")

    profileDao.insertProfile(entity1)
    assertNotNull(profileDao.getUserProfile())

    // Delete all and insert new one
    profileDao.deleteAllProfiles()
    assertNull(profileDao.getUserProfile())

    profileDao.insertProfile(entity2)
    val retrieved = profileDao.getUserProfile()
    assertNotNull(retrieved)
    assertEquals("user-2", retrieved!!.ownerId)
  }

  @Test
  fun insertProfile_preservesLocation() = runTest {
    val location = Location("Geneva", 46.2044, 6.1432)
    val entity = createTestProfileEntity("user-1", location = location)
    profileDao.insertProfile(entity)

    val retrieved = profileDao.getUserProfile()
    assertNotNull(retrieved)
    assertNotNull(retrieved!!.location)
    assertEquals(location.name, retrieved.location!!.name)
    assertEquals(location.latitude, retrieved.location!!.latitude, 0.0001)
    assertEquals(location.longitude, retrieved.location!!.longitude, 0.0001)
  }

  @Test
  fun insertProfile_preservesBookmarkedListingIds() = runTest {
    val bookmarkedIds = listOf("listing-1", "listing-2", "listing-3")
    val entity = createTestProfileEntity("user-1", bookmarkedListingIds = bookmarkedIds)
    profileDao.insertProfile(entity)

    val retrieved = profileDao.getUserProfile()
    assertNotNull(retrieved)
    assertEquals(bookmarkedIds, retrieved!!.bookmarkedListingIds)
  }

  @Test
  fun insertProfile_preservesPreferredRoomTypes() = runTest {
    val preferredRoomTypes = "STUDIO,APARTMENT,ROOM_IN_FLATSHARE"
    val entity = createTestProfileEntity("user-1", preferredRoomTypes = preferredRoomTypes)
    profileDao.insertProfile(entity)

    val retrieved = profileDao.getUserProfile()
    assertNotNull(retrieved)
    assertEquals(preferredRoomTypes, retrieved!!.preferredRoomTypes)
  }

  @Test
  fun insertProfile_preservesAllUserInfoFields() = runTest {
    val entity = createTestProfileEntity("user-1")
    profileDao.insertProfile(entity)

    val retrieved = profileDao.getUserProfile()
    assertNotNull(retrieved)
    val profile = retrieved!!.toProfile()

    assertEquals("John", profile.userInfo.name)
    assertEquals("Doe", profile.userInfo.lastName)
    assertEquals("john.doe@example.com", profile.userInfo.email)
    assertEquals("+1234567890", profile.userInfo.phoneNumber)
    assertEquals("EPFL", profile.userInfo.universityName)
    assertEquals("Vortex", profile.userInfo.residencyName)
    assertEquals("profile.jpg", profile.userInfo.profilePicture)
    val minPrice = requireNotNull(profile.userInfo.minPrice)
    val maxPrice = requireNotNull(profile.userInfo.maxPrice)
    assertEquals(500.0, minPrice, 0.01)
    assertEquals(1000.0, maxPrice, 0.01)
    assertEquals(20, profile.userInfo.minSize)
    assertEquals(50, profile.userInfo.maxSize)
  }

  @Test
  fun insertProfile_preservesAllUserSettingsFields() = runTest {
    val entity = createTestProfileEntity("user-1")
    profileDao.insertProfile(entity)

    val retrieved = profileDao.getUserProfile()
    assertNotNull(retrieved)
    val profile = retrieved!!.toProfile()

    assertEquals(Language.ENGLISH, profile.userSettings.language)
    assertEquals(true, profile.userSettings.isPublic)
    assertEquals(true, profile.userSettings.isPushNotified)
    assertEquals(false, profile.userSettings.darkMode)
  }

  @Test
  fun insertProfile_handlesNullOptionalFields() = runTest {
    val entity =
        createTestProfileEntity("user-1")
            .copy(
                universityName = null,
                location = null,
                residencyName = null,
                profilePicture = null,
                minPrice = null,
                maxPrice = null,
                minSize = null,
                maxSize = null,
                darkMode = null)
    profileDao.insertProfile(entity)

    val retrieved = profileDao.getUserProfile()
    assertNotNull(retrieved)
    assertNull(retrieved!!.universityName)
    assertNull(retrieved.location)
    assertNull(retrieved.residencyName)
    assertNull(retrieved.profilePicture)
    assertNull(retrieved.minPrice)
    assertNull(retrieved.maxPrice)
    assertNull(retrieved.minSize)
    assertNull(retrieved.maxSize)
    assertNull(retrieved.darkMode)
  }

  private fun createTestProfileEntity(
      ownerId: String,
      name: String = "John",
      location: Location? = Location("Lausanne", 46.5197, 6.6323),
      preferredRoomTypes: String = "STUDIO,APARTMENT",
      bookmarkedListingIds: List<String> = listOf("listing-1", "listing-2")
  ): ProfileEntity {
    return ProfileEntity(
        ownerId = ownerId,
        name = name,
        lastName = "Doe",
        email = "john.doe@example.com",
        phoneNumber = "+1234567890",
        universityName = "EPFL",
        location = location,
        residencyName = "Vortex",
        profilePicture = "profile.jpg",
        minPrice = 500.0,
        maxPrice = 1000.0,
        minSize = 20,
        maxSize = 50,
        preferredRoomTypes = preferredRoomTypes,
        bookmarkedListingIds = bookmarkedListingIds,
        language = "ENGLISH",
        isPublic = true,
        isPushNotified = true,
        darkMode = false)
  }
}
