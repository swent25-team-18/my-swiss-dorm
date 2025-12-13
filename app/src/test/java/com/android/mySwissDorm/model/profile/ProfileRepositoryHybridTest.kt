package com.android.mySwissDorm.model.profile

import android.content.Context
import androidx.room.Room
import com.android.mySwissDorm.model.database.AppDatabase
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.utils.NetworkUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProfileRepositoryHybridTest {
  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var localRepository: ProfileRepositoryLocal
  private lateinit var remoteRepository: ProfileRepositoryFirestore
  private lateinit var hybridRepository: ProfileRepositoryHybrid
  private lateinit var auth: FirebaseAuth
  private lateinit var firebaseUser: FirebaseUser

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    try {
      FirebaseApp.initializeApp(context)
    } catch (_: IllegalStateException) {
      // Firebase already initialized, ignore
    }

    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    auth = mockk(relaxed = true)
    firebaseUser = mockk(relaxed = true)
    every { auth.currentUser } returns firebaseUser
    every { firebaseUser.uid } returns "user-1"

    // Mock FirebaseAuth.getInstance() so ProfileRepositoryHybrid can access the current user
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns auth

    localRepository = ProfileRepositoryLocal(database.profileDao(), auth)
    remoteRepository = mockk(relaxed = true)
    hybridRepository = ProfileRepositoryHybrid(context, localRepository, remoteRepository)
  }

  @After
  fun tearDown() {
    database.close()
    unmockkAll()
  }

  @Test
  fun createProfile_online_savesToBothRepositories() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1")
    coEvery { remoteRepository.createProfile(profile) } returns Unit

    hybridRepository.createProfile(profile)

    coVerify { remoteRepository.createProfile(profile) }
    assertEquals(profile, localRepository.getProfile("user-1"))
  }

  @Test
  fun createProfile_offline_savesLocallyOnly() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val profile = createTestProfile("user-1")
    hybridRepository.createProfile(profile)

    assertEquals(profile, localRepository.getProfile("user-1"))
    coVerify(exactly = 0) { remoteRepository.createProfile(any()) }
  }

  @Test
  fun createProfile_online_handlesRemoteFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1")
    coEvery { remoteRepository.createProfile(profile) } throws IOException("Network error")

    hybridRepository.createProfile(profile)

    // Local save should still succeed
    assertEquals(profile, localRepository.getProfile("user-1"))
  }

  @Test
  fun getProfile_online_syncsLocalToRemoteThenReturnsLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val localProfile = createTestProfile("user-1", name = "Local")
    localRepository.createProfile(localProfile)

    coEvery { remoteRepository.editProfile(localProfile) } returns Unit

    val result = hybridRepository.getProfile("user-1")

    // Should return local profile (local is source of truth)
    assertEquals(localProfile, result)
    // Should push local to remote
    coVerify { remoteRepository.editProfile(localProfile) }
  }

  @Test
  fun getProfile_offline_usesLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val localProfile = createTestProfile("user-1", name = "Local")
    localRepository.createProfile(localProfile)

    val result = hybridRepository.getProfile("user-1")

    assertEquals(localProfile, result)
    coVerify(exactly = 0) { remoteRepository.getProfile(any()) }
  }

  @Test
  fun getProfile_online_handlesSyncFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val localProfile = createTestProfile("user-1", name = "Local")
    localRepository.createProfile(localProfile)

    // Sync fails (e.g., network error when pushing to remote)
    coEvery { remoteRepository.editProfile(localProfile) } throws IOException("Network error")

    // Should still return local profile even if sync fails
    val result = hybridRepository.getProfile("user-1")

    assertEquals(localProfile, result)
  }

  @Test
  fun getProfile_online_fetchesRemoteWhenLocalNotExists() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val remoteProfile = createTestProfile("user-1", name = "Remote")
    coEvery { remoteRepository.getProfile("user-1") } returns remoteProfile

    val result = hybridRepository.getProfile("user-1")

    // Should return remote profile
    assertEquals(remoteProfile, result)
    // Should sync remote to local for future offline access
    assertEquals(remoteProfile, localRepository.getProfile("user-1"))
  }

  @Test
  fun getProfile_online_handlesSyncFailureWhenProfileBelongsToDifferentUser() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    // Create a profile for user-1 locally (current user)
    val localProfile = createTestProfile("user-1")
    localRepository.createProfile(localProfile)

    // Try to get profile for user-2 from remote (different user)
    val remoteProfile = createTestProfile("user-2")
    coEvery { remoteRepository.getProfile("user-2") } returns remoteProfile

    // Should still return remote profile even if sync fails (can't create user-2's profile when
    // current user is user-1)
    val result = hybridRepository.getProfile("user-2")

    assertEquals(remoteProfile, result)
    // Local should still have user-1's profile (sync failed, so it wasn't replaced)
    assertEquals(localProfile, localRepository.getProfile("user-1"))
  }

  @Test
  fun editProfile_online_savesToBothRepositories() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1", name = "John")
    localRepository.createProfile(profile)

    val updated = profile.copy(userInfo = profile.userInfo.copy(name = "Jane"))
    coEvery { remoteRepository.editProfile(updated) } returns Unit

    hybridRepository.editProfile(updated)

    coVerify { remoteRepository.editProfile(updated) }
    assertEquals("Jane", localRepository.getProfile("user-1").userInfo.name)
  }

  @Test
  fun editProfile_offline_savesLocallyOnly() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val profile = createTestProfile("user-1", name = "John")
    localRepository.createProfile(profile)

    val updated = profile.copy(userInfo = profile.userInfo.copy(name = "Jane"))
    hybridRepository.editProfile(updated)

    assertEquals("Jane", localRepository.getProfile("user-1").userInfo.name)
    coVerify(exactly = 0) { remoteRepository.editProfile(any()) }
  }

  @Test
  fun editProfile_online_handlesRemoteFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1", name = "John")
    localRepository.createProfile(profile)

    val updated = profile.copy(userInfo = profile.userInfo.copy(name = "Jane"))
    coEvery { remoteRepository.editProfile(updated) } throws IOException("Network error")

    hybridRepository.editProfile(updated)

    // Local save should still succeed
    assertEquals("Jane", localRepository.getProfile("user-1").userInfo.name)
  }

  @Test
  fun deleteProfile_online_deletesFromBothRepositories() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1")
    localRepository.createProfile(profile)
    coEvery { remoteRepository.deleteProfile("user-1") } returns Unit

    hybridRepository.deleteProfile("user-1")

    coVerify { remoteRepository.deleteProfile("user-1") }
    val result = runCatching { localRepository.getProfile("user-1") }
    assertTrue(result.isFailure)
  }

  @Test
  fun deleteProfile_online_handlesRemoteFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1")
    localRepository.createProfile(profile)
    coEvery { remoteRepository.deleteProfile("user-1") } throws IOException("Network error")

    hybridRepository.deleteProfile("user-1")

    // Local delete should still succeed
    val result = runCatching { localRepository.getProfile("user-1") }
    assertTrue(result.isFailure)
  }

  @Test
  fun getAllProfile_online_usesRemote() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profiles = listOf(createTestProfile("user-1"), createTestProfile("user-2"))
    coEvery { remoteRepository.getAllProfile() } returns profiles

    val result = hybridRepository.getAllProfile()

    assertEquals(profiles, result)
    coVerify { remoteRepository.getAllProfile() }
  }

  @Test
  fun getAllProfile_offline_throwsUnsupportedOperationException() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val result = runCatching { hybridRepository.getAllProfile() }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
  }

  @Test
  fun getBookmarkedListingIds_online_syncsLocalToRemote() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val localProfile = createTestProfile("user-1", bookmarkedListingIds = listOf("listing-1"))
    localRepository.createProfile(localProfile)

    coEvery { remoteRepository.editProfile(localProfile) } returns Unit

    val result = hybridRepository.getBookmarkedListingIds("user-1")

    // Should return local bookmarks (local is source of truth)
    assertEquals(listOf("listing-1"), result)
    // Should push local profile to remote
    coVerify { remoteRepository.editProfile(localProfile) }
  }

  @Test
  fun addBookmark_online_savesToBoth() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1", bookmarkedListingIds = listOf("listing-1"))
    localRepository.createProfile(profile)

    coEvery { remoteRepository.addBookmark("user-1", "listing-2") } returns Unit

    hybridRepository.addBookmark("user-1", "listing-2")

    coVerify { remoteRepository.addBookmark("user-1", "listing-2") }
    val bookmarks = localRepository.getBookmarkedListingIds("user-1")
    assertTrue(bookmarks.contains("listing-1"))
    assertTrue(bookmarks.contains("listing-2"))
  }

  @Test
  fun getBlockedUserIds_online_syncsLocalToRemote() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val localProfile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    localRepository.createProfile(localProfile)

    coEvery { remoteRepository.editProfile(localProfile) } returns Unit

    val result = hybridRepository.getBlockedUserIds("user-1")

    // Should return local blocked users (local is source of truth)
    assertEquals(listOf("user-2"), result)
    // Should push local profile to remote
    coVerify { remoteRepository.editProfile(localProfile) }
  }

  @Test
  fun addBlockedUser_online_savesToBoth() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    localRepository.createProfile(profile)

    coEvery { remoteRepository.addBlockedUser("user-1", "user-3") } returns Unit

    hybridRepository.addBlockedUser("user-1", "user-3")

    coVerify { remoteRepository.addBlockedUser("user-1", "user-3") }
    val blocked = localRepository.getBlockedUserIds("user-1")
    assertTrue(blocked.contains("user-2"))
    assertTrue(blocked.contains("user-3"))
  }

  @Test
  fun removeBlockedUser_online_savesToBoth() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2", "user-3"))
    localRepository.createProfile(profile)

    coEvery { remoteRepository.removeBlockedUser("user-1", "user-2") } returns Unit

    hybridRepository.removeBlockedUser("user-1", "user-2")

    coVerify { remoteRepository.removeBlockedUser("user-1", "user-2") }
    val blocked = localRepository.getBlockedUserIds("user-1")
    assertFalse(blocked.contains("user-2"))
    assertTrue(blocked.contains("user-3"))
  }

  @Test
  fun getBookmarkedListingIds_offline_usesLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val profile = createTestProfile("user-1", bookmarkedListingIds = listOf("listing-1"))
    localRepository.createProfile(profile)

    val result = hybridRepository.getBookmarkedListingIds("user-1")

    assertEquals(listOf("listing-1"), result)
    coVerify(exactly = 0) { remoteRepository.getBookmarkedListingIds(any()) }
  }

  @Test
  fun getBlockedUserIds_offline_usesLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    localRepository.createProfile(profile)

    val result = hybridRepository.getBlockedUserIds("user-1")

    assertEquals(listOf("user-2"), result)
    coVerify(exactly = 0) { remoteRepository.getBlockedUserIds(any()) }
  }

  @Test
  fun addBookmark_offline_savesLocallyOnly() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val profile = createTestProfile("user-1", bookmarkedListingIds = listOf("listing-1"))
    localRepository.createProfile(profile)

    hybridRepository.addBookmark("user-1", "listing-2")

    val bookmarks = localRepository.getBookmarkedListingIds("user-1")
    assertTrue(bookmarks.contains("listing-1"))
    assertTrue(bookmarks.contains("listing-2"))
    coVerify(exactly = 0) { remoteRepository.addBookmark(any(), any()) }
  }

  @Test
  fun addBookmark_online_handlesRemoteFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1", bookmarkedListingIds = listOf("listing-1"))
    localRepository.createProfile(profile)

    coEvery { remoteRepository.addBookmark("user-1", "listing-2") } throws
        IOException("Network error")

    hybridRepository.addBookmark("user-1", "listing-2")

    // Local save should still succeed
    val bookmarks = localRepository.getBookmarkedListingIds("user-1")
    assertTrue(bookmarks.contains("listing-1"))
    assertTrue(bookmarks.contains("listing-2"))
  }

  @Test
  fun removeBookmark_online_handlesRemoteFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile =
        createTestProfile("user-1", bookmarkedListingIds = listOf("listing-1", "listing-2"))
    localRepository.createProfile(profile)

    coEvery { remoteRepository.removeBookmark("user-1", "listing-1") } throws
        IOException("Network error")

    hybridRepository.removeBookmark("user-1", "listing-1")

    // Local save should still succeed
    val bookmarks = localRepository.getBookmarkedListingIds("user-1")
    assertFalse(bookmarks.contains("listing-1"))
    assertTrue(bookmarks.contains("listing-2"))
  }

  @Test
  fun syncProfile_handlesLocalProfileNotExists() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    // Don't create local profile, so syncProfile will handle NoSuchElementException

    // Should not throw, should handle gracefully
    val result = hybridRepository.getBlockedUserIds("user-1")

    // Should return empty list when local profile doesn't exist
    assertEquals(emptyList<String>(), result)
    // Should not try to sync (no local profile to sync)
    coVerify(exactly = 0) { remoteRepository.editProfile(any()) }
  }

  @Test
  fun syncProfile_handlesRemoteEditFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val localProfile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    localRepository.createProfile(localProfile)

    // Remote edit fails (e.g., network error)
    coEvery { remoteRepository.editProfile(localProfile) } throws IOException("Network error")

    // Should not throw, should return local data even if sync fails
    val result = hybridRepository.getBlockedUserIds("user-1")

    assertEquals(listOf("user-2"), result)
  }

  @Test
  fun syncProfile_handlesRemoteGetProfileFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val localProfile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    localRepository.createProfile(localProfile)

    // Remote edit fails (e.g., network error when getting profile for edit)
    coEvery { remoteRepository.editProfile(localProfile) } throws IOException("Network error")

    // Should not throw, should return local data even if sync fails
    val result = hybridRepository.getBlockedUserIds("user-1")

    assertEquals(listOf("user-2"), result)
  }

  @Test
  fun syncProfile_skipsWhenNotCurrentUser() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    // Create a profile for user-1 locally (current user)
    val localProfile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    localRepository.createProfile(localProfile)

    // Try to get blocked users for user-2 (not current user)
    // syncProfile should skip because ownerId != currentUserId
    hybridRepository.getBlockedUserIds("user-2")

    // Should not try to sync user-2's profile to remote (syncProfile skips)
    coVerify(exactly = 0) { remoteRepository.editProfile(any()) }
  }

  @Test
  fun getBlockedUserIds_otherUser_online_fetchesFromRemote() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    // Try to get blocked users for user-2 (not current user, current user is user-1)
    val remoteBlockedUsers = listOf("user-3", "user-4")
    coEvery { remoteRepository.getBlockedUserIds("user-2") } returns remoteBlockedUsers

    val result = hybridRepository.getBlockedUserIds("user-2")

    // Should fetch from remote (not try to sync local to remote)
    coVerify(exactly = 0) { remoteRepository.editProfile(any()) }
    coVerify { remoteRepository.getBlockedUserIds("user-2") }
    // Should return remote data
    assertEquals(remoteBlockedUsers, result)
  }

  @Test
  fun getBlockedUserIds_otherUser_offline_returnsEmpty() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    // Try to get blocked users for user-2 (not current user) while offline
    val result = hybridRepository.getBlockedUserIds("user-2")

    // Should return empty list (can't fetch other users' data offline)
    assertEquals(emptyList<String>(), result)
    coVerify(exactly = 0) { remoteRepository.getBlockedUserIds(any()) }
  }

  @Test
  fun getBookmarkedListingIds_otherUser_online_fetchesFromRemote() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    // Try to get bookmarks for user-2 (not current user, current user is user-1)
    val remoteBookmarks = listOf("listing-1", "listing-2")
    coEvery { remoteRepository.getBookmarkedListingIds("user-2") } returns remoteBookmarks

    val result = hybridRepository.getBookmarkedListingIds("user-2")

    // Should fetch from remote (not try to sync local to remote)
    coVerify(exactly = 0) { remoteRepository.editProfile(any()) }
    coVerify { remoteRepository.getBookmarkedListingIds("user-2") }
    // Should return remote data
    assertEquals(remoteBookmarks, result)
  }

  @Test
  fun getBookmarkedListingIds_otherUser_offline_returnsEmpty() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    // Try to get bookmarks for user-2 (not current user) while offline
    val result = hybridRepository.getBookmarkedListingIds("user-2")

    // Should return empty list (can't fetch other users' data offline)
    assertEquals(emptyList<String>(), result)
    coVerify(exactly = 0) { remoteRepository.getBookmarkedListingIds(any()) }
  }

  @Test
  fun syncProfileToLocal_skipsWhenNotCurrentUser() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    // Try to get profile for user-2 from remote (different user)
    val remoteProfile = createTestProfile("user-2", name = "Other User")
    coEvery { remoteRepository.getProfile("user-2") } returns remoteProfile

    val result = hybridRepository.getProfile("user-2")

    // Should return remote profile
    assertEquals(remoteProfile, result)
    // Should NOT sync to local (skipped because not current user)
    // Verify local doesn't have user-2's profile
    try {
      localRepository.getProfile("user-2")
      fail("Expected NoSuchElementException - user-2's profile should not be in local storage")
    } catch (e: NoSuchElementException) {
      // Expected - profile should not be synced to local
    }
  }

  @Test
  fun addBlockedUser_offline_savesLocallyOnly() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    localRepository.createProfile(profile)

    hybridRepository.addBlockedUser("user-1", "user-3")

    val blocked = localRepository.getBlockedUserIds("user-1")
    assertTrue(blocked.contains("user-2"))
    assertTrue(blocked.contains("user-3"))
    coVerify(exactly = 0) { remoteRepository.addBlockedUser(any(), any()) }
  }

  @Test
  fun addBlockedUser_online_handlesRemoteFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    localRepository.createProfile(profile)

    coEvery { remoteRepository.addBlockedUser("user-1", "user-3") } throws
        IOException("Network error")

    hybridRepository.addBlockedUser("user-1", "user-3")

    // Local save should still succeed
    val blocked = localRepository.getBlockedUserIds("user-1")
    assertTrue(blocked.contains("user-2"))
    assertTrue(blocked.contains("user-3"))
  }

  @Test
  fun removeBlockedUser_online_handlesRemoteFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2", "user-3"))
    localRepository.createProfile(profile)

    coEvery { remoteRepository.removeBlockedUser("user-1", "user-2") } throws
        IOException("Network error")

    hybridRepository.removeBlockedUser("user-1", "user-2")

    // Local save should still succeed
    val blocked = localRepository.getBlockedUserIds("user-1")
    assertFalse(blocked.contains("user-2"))
    assertTrue(blocked.contains("user-3"))
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
