package com.android.mySwissDorm.model.profile

import androidx.room.Room
import com.android.mySwissDorm.model.database.AppDatabase
import com.android.mySwissDorm.model.map.Location
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProfileRepositoryLocalTest {
  private lateinit var database: AppDatabase
  private lateinit var auth: FirebaseAuth
  private lateinit var firebaseUser: FirebaseUser
  private lateinit var repository: ProfileRepositoryLocal

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    auth = mockk(relaxed = true)
    firebaseUser = mockk(relaxed = true)
    every { auth.currentUser } returns firebaseUser
    every { firebaseUser.uid } returns "user-1"

    repository = ProfileRepositoryLocal(database.profileDao(), auth)
  }

  @After
  fun tearDown() {
    database.close()
    unmockkAll()
  }

  @Test
  fun createProfile_succeeds() = runTest {
    val profile = createTestProfile("user-1")
    repository.createProfile(profile)

    val retrieved = repository.getProfile("user-1")
    assertEquals(profile, retrieved)
  }

  @Test
  fun createProfile_clearsExistingProfile() = runTest {
    val profile1 = createTestProfile("user-1", name = "John")
    repository.createProfile(profile1)

    val profile2 = createTestProfile("user-1", name = "Jane")
    repository.createProfile(profile2)

    val retrieved = repository.getProfile("user-1")
    assertEquals("Jane", retrieved.userInfo.name)
  }

  @Test
  fun createProfile_throwsWhenOwnerIdMismatch() = runTest {
    val profile = createTestProfile("user-2")
    val result = runCatching { repository.createProfile(profile) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun createProfile_throwsWhenNoUserLoggedIn() = runTest {
    every { auth.currentUser } returns null
    val profile = createTestProfile("user-1")
    val result = runCatching { repository.createProfile(profile) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalStateException)
  }

  @Test
  fun getProfile_returnsProfile() = runTest {
    val profile = createTestProfile("user-1")
    repository.createProfile(profile)

    val retrieved = repository.getProfile("user-1")
    assertEquals(profile, retrieved)
  }

  @Test
  fun getProfile_throwsWhenNotFound() = runTest {
    val result = runCatching { repository.getProfile("user-1") }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
  }

  @Test
  fun getProfile_throwsWhenOwnerIdMismatch() = runTest {
    val profile = createTestProfile("user-1")
    repository.createProfile(profile)

    val result = runCatching { repository.getProfile("user-2") }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
  }

  @Test
  fun getAllProfile_throwsUnsupportedOperationException() = runTest {
    val result = runCatching { repository.getAllProfile() }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
  }

  @Test
  fun editProfile_updatesProfile() = runTest {
    val profile = createTestProfile("user-1", name = "John")
    repository.createProfile(profile)

    val updated = profile.copy(userInfo = profile.userInfo.copy(name = "Jane"))
    repository.editProfile(updated)

    val retrieved = repository.getProfile("user-1")
    assertEquals("Jane", retrieved.userInfo.name)
  }

  @Test
  fun editProfile_throwsWhenNotFound() = runTest {
    val profile = createTestProfile("user-1")
    val result = runCatching { repository.editProfile(profile) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
  }

  @Test
  fun editProfile_throwsWhenOwnerIdMismatch() = runTest {
    val profile = createTestProfile("user-1")
    repository.createProfile(profile)

    val updated = profile.copy(ownerId = "user-2")
    val result = runCatching { repository.editProfile(updated) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun deleteProfile_removesProfile() = runTest {
    val profile = createTestProfile("user-1")
    repository.createProfile(profile)

    repository.deleteProfile("user-1")

    val result = runCatching { repository.getProfile("user-1") }
    assertTrue(result.isFailure)
  }

  @Test
  fun deleteProfile_throwsWhenNotFound() = runTest {
    val result = runCatching { repository.deleteProfile("user-1") }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
  }

  @Test
  fun deleteProfile_throwsWhenOwnerIdMismatch() = runTest {
    val profile = createTestProfile("user-1")
    repository.createProfile(profile)

    val result = runCatching { repository.deleteProfile("user-2") }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
  }

  @Test
  fun getBookmarkedListingIds_returnsBookmarks() = runTest {
    val profile =
        createTestProfile("user-1", bookmarkedListingIds = listOf("listing-1", "listing-2"))
    repository.createProfile(profile)

    val bookmarks = repository.getBookmarkedListingIds("user-1")
    assertEquals(listOf("listing-1", "listing-2"), bookmarks)
  }

  @Test
  fun addBookmark_addsBookmark() = runTest {
    val profile = createTestProfile("user-1", bookmarkedListingIds = listOf("listing-1"))
    repository.createProfile(profile)

    repository.addBookmark("user-1", "listing-2")

    val bookmarks = repository.getBookmarkedListingIds("user-1")
    assertTrue(bookmarks.contains("listing-1"))
    assertTrue(bookmarks.contains("listing-2"))
  }

  @Test
  fun removeBookmark_removesBookmark() = runTest {
    val profile =
        createTestProfile("user-1", bookmarkedListingIds = listOf("listing-1", "listing-2"))
    repository.createProfile(profile)

    repository.removeBookmark("user-1", "listing-1")

    val bookmarks = repository.getBookmarkedListingIds("user-1")
    assertFalse(bookmarks.contains("listing-1"))
    assertTrue(bookmarks.contains("listing-2"))
  }

  @Test
  fun getBlockedUserIds_returnsBlockedUsers() = runTest {
    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2", "user-3"))
    repository.createProfile(profile)

    val blocked = repository.getBlockedUserIds("user-1")
    assertEquals(listOf("user-2", "user-3"), blocked)
  }

  @Test
  fun addBlockedUser_addsBlockedUser() = runTest {
    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    repository.createProfile(profile)

    repository.addBlockedUser("user-1", "user-3")

    val blocked = repository.getBlockedUserIds("user-1")
    assertTrue(blocked.contains("user-2"))
    assertTrue(blocked.contains("user-3"))
  }

  @Test
  fun removeBlockedUser_removesBlockedUser() = runTest {
    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2", "user-3"))
    repository.createProfile(profile)

    repository.removeBlockedUser("user-1", "user-2")

    val blocked = repository.getBlockedUserIds("user-1")
    assertFalse(blocked.contains("user-2"))
    assertTrue(blocked.contains("user-3"))
  }

  @Test
  fun getBlockedUserNames_returnsEmptyMapWhenNoNamesStored() = runTest {
    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2", "user-3"))
    repository.createProfile(profile)

    val names = repository.getBlockedUserNames("user-1")
    assertTrue(names.isEmpty())
  }

  @Test
  fun getBlockedUserNames_returnsStoredNames() = runTest {
    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    repository.createProfile(profile)

    // Add blocked user with name
    repository.addBlockedUserWithName("user-1", "user-3", "Jane Smith")

    val names = repository.getBlockedUserNames("user-1")
    assertEquals("Jane Smith", names["user-3"])
    assertEquals(1, names.size)
  }

  @Test
  fun getBlockedUserNames_returnsMultipleStoredNames() = runTest {
    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    repository.createProfile(profile)

    // Add multiple blocked users with names
    repository.addBlockedUserWithName("user-1", "user-3", "Jane Smith")
    repository.addBlockedUserWithName("user-1", "user-4", "Bob Johnson")

    val names = repository.getBlockedUserNames("user-1")
    assertEquals("Jane Smith", names["user-3"])
    assertEquals("Bob Johnson", names["user-4"])
    assertEquals(2, names.size)
  }

  @Test
  fun getBlockedUserNames_removesNameWhenUnblocking() = runTest {
    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    repository.createProfile(profile)

    // Add blocked user with name
    repository.addBlockedUserWithName("user-1", "user-3", "Jane Smith")
    var names = repository.getBlockedUserNames("user-1")
    assertEquals("Jane Smith", names["user-3"])

    // Remove blocked user
    repository.removeBlockedUser("user-1", "user-3")

    // Name should also be removed
    names = repository.getBlockedUserNames("user-1")
    assertFalse(names.containsKey("user-3"))
    assertTrue(names.isEmpty())
  }

  @Test
  fun getBlockedUserNames_throwsWhenProfileNotFound() = runTest {
    val result = runCatching { repository.getBlockedUserNames("user-1") }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
  }

  @Test
  fun getBlockedUserNames_throwsWhenOwnerIdMismatch() = runTest {
    val profile = createTestProfile("user-1")
    repository.createProfile(profile)

    val result = runCatching { repository.getBlockedUserNames("user-2") }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  private fun createTestProfile(
      ownerId: String,
      name: String = "John",
      bookmarkedListingIds: List<String> = emptyList(),
      blockedUserIds: List<String> = emptyList()
  ): Profile {
    return Profile(
        userInfo =
            UserInfo(
                name = name,
                lastName = "Doe",
                email = "john.doe@example.com",
                phoneNumber = "+1234567890",
                universityName = "EPFL",
                location = Location("Lausanne", 46.5197, 6.6323),
                residencyName = "Vortex",
                profilePicture = null,
                minPrice = 500.0,
                maxPrice = 1000.0,
                minSize = 20,
                maxSize = 50,
                preferredRoomTypes = emptyList(),
                bookmarkedListingIds = bookmarkedListingIds,
                blockedUserIds = blockedUserIds),
        userSettings = UserSettings(language = Language.ENGLISH, isPublic = true),
        ownerId = ownerId)
  }
}
