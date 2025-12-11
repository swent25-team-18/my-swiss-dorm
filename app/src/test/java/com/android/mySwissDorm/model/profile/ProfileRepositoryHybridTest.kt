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
  fun getProfile_online_usesRemoteAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val remoteProfile = createTestProfile("user-1", name = "Remote")
    coEvery { remoteRepository.getProfile("user-1") } returns remoteProfile

    val result = hybridRepository.getProfile("user-1")

    assertEquals(remoteProfile, result)
    coVerify { remoteRepository.getProfile("user-1") }
    assertEquals(remoteProfile, localRepository.getProfile("user-1"))
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
  fun getProfile_online_fallsBackToLocalOnNetworkError() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true
    every { NetworkUtils.isNetworkException(any()) } returns true

    val localProfile = createTestProfile("user-1", name = "Local")
    localRepository.createProfile(localProfile)

    coEvery { remoteRepository.getProfile("user-1") } throws IOException("Network error")

    val result = hybridRepository.getProfile("user-1")

    assertEquals(localProfile, result)
  }

  @Test
  fun getProfile_online_createsLocalWhenNotExists() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val remoteProfile = createTestProfile("user-1")
    coEvery { remoteRepository.getProfile("user-1") } returns remoteProfile

    hybridRepository.getProfile("user-1")

    assertEquals(remoteProfile, localRepository.getProfile("user-1"))
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
  fun getBookmarkedListingIds_online_syncsAndReturnsMerged() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val localProfile = createTestProfile("user-1", bookmarkedListingIds = listOf("listing-1"))
    localRepository.createProfile(localProfile)

    coEvery { remoteRepository.getBookmarkedListingIds("user-1") } returns listOf("listing-2")
    coEvery { remoteRepository.getProfile("user-1") } returns
        createTestProfile("user-1", bookmarkedListingIds = listOf("listing-2"))
    coEvery { remoteRepository.editProfile(any()) } returns Unit

    val result = hybridRepository.getBookmarkedListingIds("user-1")

    assertTrue(result.contains("listing-1"))
    assertTrue(result.contains("listing-2"))
  }

  @Test
  fun addBookmark_online_savesToBothAndSyncs() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1", bookmarkedListingIds = listOf("listing-1"))
    localRepository.createProfile(profile)

    coEvery { remoteRepository.addBookmark("user-1", "listing-2") } returns Unit
    coEvery { remoteRepository.getBookmarkedListingIds("user-1") } returns listOf("listing-2")
    coEvery { remoteRepository.getProfile("user-1") } returns
        createTestProfile("user-1", bookmarkedListingIds = listOf("listing-2"))
    coEvery { remoteRepository.editProfile(any()) } returns Unit

    hybridRepository.addBookmark("user-1", "listing-2")

    coVerify { remoteRepository.addBookmark("user-1", "listing-2") }
    val bookmarks = localRepository.getBookmarkedListingIds("user-1")
    assertTrue(bookmarks.contains("listing-1"))
    assertTrue(bookmarks.contains("listing-2"))
  }

  @Test
  fun getBlockedUserIds_online_syncsAndReturnsMerged() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val localProfile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    localRepository.createProfile(localProfile)

    coEvery { remoteRepository.getBlockedUserIds("user-1") } returns listOf("user-3")
    coEvery { remoteRepository.getProfile("user-1") } returns
        createTestProfile("user-1", blockedUserIds = listOf("user-3"))
    coEvery { remoteRepository.editProfile(any()) } returns Unit

    val result = hybridRepository.getBlockedUserIds("user-1")

    assertTrue(result.contains("user-2"))
    assertTrue(result.contains("user-3"))
  }

  @Test
  fun addBlockedUser_online_savesToBothAndSyncs() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2"))
    localRepository.createProfile(profile)

    coEvery { remoteRepository.addBlockedUser("user-1", "user-3") } returns Unit
    coEvery { remoteRepository.getBlockedUserIds("user-1") } returns listOf("user-3")
    coEvery { remoteRepository.getProfile("user-1") } returns
        createTestProfile("user-1", blockedUserIds = listOf("user-3"))
    coEvery { remoteRepository.editProfile(any()) } returns Unit

    hybridRepository.addBlockedUser("user-1", "user-3")

    coVerify { remoteRepository.addBlockedUser("user-1", "user-3") }
    val blocked = localRepository.getBlockedUserIds("user-1")
    assertTrue(blocked.contains("user-2"))
    assertTrue(blocked.contains("user-3"))
  }

  @Test
  fun removeBlockedUser_online_savesToBothAndSyncs() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val profile = createTestProfile("user-1", blockedUserIds = listOf("user-2", "user-3"))
    localRepository.createProfile(profile)

    coEvery { remoteRepository.removeBlockedUser("user-1", "user-2") } returns Unit
    coEvery { remoteRepository.getBlockedUserIds("user-1") } returns listOf("user-3")
    coEvery { remoteRepository.getProfile("user-1") } returns
        createTestProfile("user-1", blockedUserIds = listOf("user-3"))
    coEvery { remoteRepository.editProfile(any()) } returns Unit

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
