package com.android.mySwissDorm.model.rental

import android.content.Context
import androidx.room.Room
import com.android.mySwissDorm.model.database.AppDatabase
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.profile.Language
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.utils.NetworkUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.io.IOException
import java.net.UnknownHostException
import kotlin.NoSuchElementException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RentalListingRepositoryHybridTest {
  private lateinit var context: Context
  private lateinit var remoteRepository: RentalListingRepositoryFirestore
  private lateinit var localRepository: RentalListingRepositoryLocal
  private lateinit var database: AppDatabase
  private lateinit var hybridRepository: RentalListingRepositoryHybrid
  private lateinit var profileRepository: ProfileRepository

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    // Initialize Firebase before accessing ProfileRepositoryProvider to prevent initialization
    // errors
    try {
      FirebaseApp.initializeApp(context)
    } catch (e: IllegalStateException) {
      // Firebase already initialized, ignore
    }
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    localRepository = RentalListingRepositoryLocal(database.rentalListingDao())
    remoteRepository = mockk(relaxed = true)
    // Initialize ProfileRepositoryProvider with a mock to prevent NoClassDefFoundError
    profileRepository = mockk(relaxed = true)
    ProfileRepositoryProvider.repository = profileRepository
    hybridRepository = RentalListingRepositoryHybrid(context, remoteRepository, localRepository)
  }

  @After
  fun tearDown() {
    database.close()
    unmockkAll()
  }

  @Test
  fun getAllRentalListings_online_usesRemoteAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val remoteListings = listOf(createTestListing("listing-1"), createTestListing("listing-2"))
    coEvery { remoteRepository.getAllRentalListings() } returns remoteListings

    val result = hybridRepository.getAllRentalListings()

    assertEquals(remoteListings, result)
    coVerify { remoteRepository.getAllRentalListings() }
    assertEquals(2, localRepository.getAllRentalListings().size)
  }

  @Test
  fun getAllRentalListings_online_fallsBackToLocalOnNetworkError() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true
    every { NetworkUtils.isNetworkException(any()) } returns true

    val localListing = createTestListing("listing-1")
    localRepository.addRentalListing(localListing)

    coEvery { remoteRepository.getAllRentalListings() } throws IOException("Network error")

    val result = hybridRepository.getAllRentalListings()

    assertEquals(listOf(localListing), result)
  }

  @Test
  fun getRentalListing_online_fallsBackToLocalOnNotFound() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val localListing = createTestListing("listing-1")
    localRepository.addRentalListing(localListing)

    coEvery { remoteRepository.getRentalListing("listing-1") } throws
        NoSuchElementException("Not found")

    val result = hybridRepository.getRentalListing("listing-1")
    assertEquals(localListing, result)
  }

  @Test
  fun getAllRentalListings_offline_usesLocalImmediately() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val localListing = createTestListing("listing-1")
    localRepository.addRentalListing(localListing)

    val result = hybridRepository.getAllRentalListings()

    assertEquals(listOf(localListing), result)
    coVerify(exactly = 0) { remoteRepository.getAllRentalListings() }
  }

  @Test
  fun addRentalListing_online_succeedsAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val listing = createTestListing("listing-1")
    coEvery { remoteRepository.addRentalListing(listing) } returns Unit

    hybridRepository.addRentalListing(listing)

    coVerify { remoteRepository.addRentalListing(listing) }
    assertEquals(listing, localRepository.getRentalListing("listing-1"))
  }

  @Test
  fun addRentalListing_offline_throwsUnsupportedOperationException() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val listing = createTestListing("listing-1")
    val result = runCatching { hybridRepository.addRentalListing(listing) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    coVerify(exactly = 0) { remoteRepository.addRentalListing(any()) }
  }

  @Test
  fun addRentalListing_online_throwsOnNetworkError() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true
    every { NetworkUtils.isNetworkException(any()) } returns true

    val listing = createTestListing("listing-1")
    coEvery { remoteRepository.addRentalListing(listing) } throws IOException("Network error")

    val result = runCatching { hybridRepository.addRentalListing(listing) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
  }

  @Test
  fun getNewUid_throwsOnNetworkError() {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true
    every { NetworkUtils.isNetworkException(any()) } returns true

    every { remoteRepository.getNewUid() } throws UnknownHostException("DNS error")

    val result = runCatching { hybridRepository.getNewUid() }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
  }

  @Test
  fun getAllRentalListings_online_propagatesNonNetworkExceptions() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true
    every { NetworkUtils.isNetworkException(any()) } returns false

    coEvery { remoteRepository.getAllRentalListings() } throws
        IllegalArgumentException("Invalid argument")

    val result = runCatching { hybridRepository.getAllRentalListings() }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun getAllRentalListingsByLocation_online_usesRemoteAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val location = Location("Test Location", 46.5, 6.5)
    val remoteListings = listOf(createTestListing("listing-1"))
    coEvery { remoteRepository.getAllRentalListingsByLocation(location, 1000.0) } returns
        remoteListings

    val result = hybridRepository.getAllRentalListingsByLocation(location, 1000.0)

    assertEquals(remoteListings, result)
    coVerify { remoteRepository.getAllRentalListingsByLocation(location, 1000.0) }
    assertEquals(1, localRepository.getAllRentalListings().size)
  }

  @Test
  fun getAllRentalListingsByLocation_offline_usesLocalImmediately() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val location = Location("Test Location", 46.5, 6.5)
    val localListing = createTestListing("listing-1")
    localRepository.addRentalListing(localListing)

    val result = hybridRepository.getAllRentalListingsByLocation(location, 1000.0)

    assertEquals(listOf(localListing), result)
    coVerify(exactly = 0) { remoteRepository.getAllRentalListingsByLocation(any(), any()) }
  }

  @Test
  fun getAllRentalListingsByUser_online_usesRemoteAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val remoteListings = listOf(createTestListing("listing-1", ownerId = "user-1"))
    coEvery { remoteRepository.getAllRentalListingsByUser("user-1") } returns remoteListings

    val result = hybridRepository.getAllRentalListingsByUser("user-1")

    assertEquals(remoteListings, result)
    coVerify { remoteRepository.getAllRentalListingsByUser("user-1") }
    assertEquals(1, localRepository.getAllRentalListingsByUser("user-1").size)
  }

  @Test
  fun getAllRentalListingsByUser_offline_usesLocalImmediately() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val localListing = createTestListing("listing-1", ownerId = "user-1")
    localRepository.addRentalListing(localListing)

    val result = hybridRepository.getAllRentalListingsByUser("user-1")

    assertEquals(listOf(localListing), result)
    coVerify(exactly = 0) { remoteRepository.getAllRentalListingsByUser(any()) }
  }

  @Test
  fun editRentalListing_online_succeedsAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val listing = createTestListing("listing-1")
    localRepository.addRentalListing(listing)

    val updatedListing = listing.copy(title = "Updated Title")
    coEvery { remoteRepository.editRentalListing("listing-1", updatedListing) } returns Unit

    hybridRepository.editRentalListing("listing-1", updatedListing)

    coVerify { remoteRepository.editRentalListing("listing-1", updatedListing) }
    assertEquals(updatedListing.title, localRepository.getRentalListing("listing-1").title)
  }

  @Test
  fun editRentalListing_offline_throwsUnsupportedOperationException() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val listing = createTestListing("listing-1")
    val result = runCatching { hybridRepository.editRentalListing("listing-1", listing) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    coVerify(exactly = 0) { remoteRepository.editRentalListing(any(), any()) }
  }

  @Test
  fun deleteRentalListing_online_succeedsAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val listing = createTestListing("listing-1")
    localRepository.addRentalListing(listing)
    coEvery { remoteRepository.deleteRentalListing("listing-1") } returns Unit

    hybridRepository.deleteRentalListing("listing-1")

    coVerify { remoteRepository.deleteRentalListing("listing-1") }
    val result = runCatching { localRepository.getRentalListing("listing-1") }
    assertTrue(result.isFailure)
  }

  @Test
  fun deleteRentalListing_offline_throwsUnsupportedOperationException() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val result = runCatching { hybridRepository.deleteRentalListing("listing-1") }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    coVerify(exactly = 0) { remoteRepository.deleteRentalListing(any()) }
  }

  @Test
  fun getAllRentalListings_online_emptyListDoesNotSync() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    coEvery { remoteRepository.getAllRentalListings() } returns emptyList()

    val result = hybridRepository.getAllRentalListings()

    assertEquals(emptyList<RentalListing>(), result)
    assertEquals(0, localRepository.getAllRentalListings().size)
  }

  @Test
  fun syncListingsToLocal_handlesIndividualListingSyncErrors() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val listing1 = createTestListing("listing-1")
    val listing2 = createTestListing("listing-2")
    val listing3 = createTestListing("listing-3")

    // Add listing2 first so it will fail when trying to sync again
    localRepository.addRentalListing(listing2)

    coEvery { remoteRepository.getAllRentalListings() } returns listOf(listing1, listing2, listing3)

    // Should not throw even if one listing fails to sync
    val result = hybridRepository.getAllRentalListings()

    assertEquals(3, result.size)
  }

  @Test
  fun addRentalListing_online_localSyncFailureDoesNotCrash() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val listing = createTestListing("listing-1")
    coEvery { remoteRepository.addRentalListing(listing) } returns Unit
    // Add it first so addRentalListing will fail
    localRepository.addRentalListing(listing)

    // Should not throw even if local sync fails
    hybridRepository.addRentalListing(listing)

    coVerify { remoteRepository.addRentalListing(listing) }
  }

  @Test
  fun editRentalListing_online_handlesLocalSyncError() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val listing = createTestListing("listing-1")
    val updatedListing = listing.copy(title = "Updated Title")

    coEvery { remoteRepository.editRentalListing("listing-1", updatedListing) } returns Unit
    // Don't add to local first so edit will fail

    // Should not throw even if local sync fails
    hybridRepository.editRentalListing("listing-1", updatedListing)

    coVerify { remoteRepository.editRentalListing("listing-1", updatedListing) }
  }

  @Test
  fun getRentalListing_online_usesRemoteAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val remoteListing = createTestListing("listing-1")
    coEvery { remoteRepository.getRentalListing("listing-1") } returns remoteListing

    val result = hybridRepository.getRentalListing("listing-1")

    assertEquals(remoteListing, result)
    coVerify { remoteRepository.getRentalListing("listing-1") }
    assertEquals(remoteListing, localRepository.getRentalListing("listing-1"))
  }

  @Test
  fun getRentalListing_offline_usesLocalImmediately() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val localListing = createTestListing("listing-1")
    localRepository.addRentalListing(localListing)

    val result = hybridRepository.getRentalListing("listing-1")

    assertEquals(localListing, result)
    coVerify(exactly = 0) { remoteRepository.getRentalListing(any()) }
  }

  @Test
  fun getNewUid_succeedsWhenNetworkAvailable() {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true
    every { NetworkUtils.isNetworkException(any()) } returns false

    every { remoteRepository.getNewUid() } returns "new-uid-123"

    val result = hybridRepository.getNewUid()

    assertEquals("new-uid-123", result)
  }

  @Test
  fun getNewUid_propagatesNonNetworkExceptions() {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true
    every { NetworkUtils.isNetworkException(any()) } returns false

    every { remoteRepository.getNewUid() } throws IllegalArgumentException("Invalid")

    val result = runCatching { hybridRepository.getNewUid() }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun getAllRentalListings_online_fetchesOwnerNameWhenMissing() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val testProfileRepository = mockk<ProfileRepository>(relaxed = true)
    ProfileRepositoryProvider.repository = testProfileRepository

    val testProfile =
        Profile(
            userInfo = UserInfo("John", "Doe", "john@example.com", "", null, null, null),
            userSettings = UserSettings(Language.ENGLISH, false),
            ownerId = "user-1")

    coEvery { testProfileRepository.getProfile("user-1") } returns testProfile

    val listingWithoutName = createTestListing("listing-1", ownerId = "user-1", ownerName = null)
    coEvery { remoteRepository.getAllRentalListings() } returns listOf(listingWithoutName)

    val result = hybridRepository.getAllRentalListings()

    assertEquals(1, result.size)
    coVerify { testProfileRepository.getProfile("user-1") }
    // Verify the listing was stored with ownerName
    val storedListing = localRepository.getRentalListing("listing-1")
    assertEquals("John Doe", storedListing.ownerName)
    // Restore original mock
    ProfileRepositoryProvider.repository = profileRepository
  }

  @Test
  fun getAllRentalListings_online_doesNotFetchOwnerNameWhenAlreadyPresent() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val listingWithName =
        createTestListing("listing-1", ownerId = "user-1", ownerName = "Jane Smith")
    coEvery { remoteRepository.getAllRentalListings() } returns listOf(listingWithName)

    val result = hybridRepository.getAllRentalListings()

    assertEquals(1, result.size)
    coVerify(exactly = 0) { profileRepository.getProfile(any()) }
    // Verify the listing was stored with existing ownerName
    val storedListing = localRepository.getRentalListing("listing-1")
    assertEquals("Jane Smith", storedListing.ownerName)
  }

  @Test
  fun getAllRentalListings_online_handlesProfileFetchFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val testProfileRepository = mockk<ProfileRepository>(relaxed = true)
    ProfileRepositoryProvider.repository = testProfileRepository

    coEvery { testProfileRepository.getProfile("user-1") } throws
        NoSuchElementException("Profile not found")

    val listingWithoutName = createTestListing("listing-1", ownerId = "user-1", ownerName = null)
    coEvery { remoteRepository.getAllRentalListings() } returns listOf(listingWithoutName)

    val result = hybridRepository.getAllRentalListings()

    assertEquals(1, result.size)
    // Verify the listing was stored with null ownerName when profile fetch fails
    val storedListing = localRepository.getRentalListing("listing-1")
    assertNull(storedListing.ownerName)
    // Restore original mock
    ProfileRepositoryProvider.repository = profileRepository
  }

  @Test
  fun getAllRentalListings_online_handlesEmptyOwnerName() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val testProfileRepository = mockk<ProfileRepository>(relaxed = true)
    ProfileRepositoryProvider.repository = testProfileRepository

    val testProfile =
        Profile(
            userInfo = UserInfo("", "", "john@example.com", "", null, null, null),
            userSettings = UserSettings(Language.ENGLISH, false),
            ownerId = "user-1")

    coEvery { testProfileRepository.getProfile("user-1") } returns testProfile

    val listingWithoutName = createTestListing("listing-1", ownerId = "user-1", ownerName = null)
    coEvery { remoteRepository.getAllRentalListings() } returns listOf(listingWithoutName)

    val result = hybridRepository.getAllRentalListings()

    assertEquals(1, result.size)
    // Verify the listing was stored with null ownerName when name is empty
    val storedListing = localRepository.getRentalListing("listing-1")
    assertNull(storedListing.ownerName)
    // Restore original mock
    ProfileRepositoryProvider.repository = profileRepository
  }

  @Test
  fun getAllRentalListingsForUser_nullUserId_returnsAllListings() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val listings = listOf(createTestListing("listing-1"), createTestListing("listing-2"))
    coEvery { remoteRepository.getAllRentalListings() } returns listings

    val result = hybridRepository.getAllRentalListingsForUser(null)

    assertEquals(listings, result)
    coVerify(exactly = 0) { profileRepository.getBlockedUserIds(any()) }
  }

  @Test
  fun getAllRentalListingsForUser_filtersWhenCurrentUserBlockedOwner() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val listing1 = createTestListing("listing-1", ownerId = user1Id)
    val listing2 = createTestListing("listing-2", ownerId = user2Id)
    val listing3 = createTestListing("listing-3", ownerId = "user-3")

    coEvery { remoteRepository.getAllRentalListings() } returns listOf(listing1, listing2, listing3)
    // User1 has blocked user2
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns listOf(user2Id)
    // User2 and user3 have not blocked anyone
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns emptyList()
    coEvery { profileRepository.getBlockedUserIds("user-3") } returns emptyList()

    val result = hybridRepository.getAllRentalListingsForUser(user1Id)

    // Should filter out listing2 (user2's listing) because user1 blocked user2
    assertEquals(2, result.size)
    assertTrue(result.contains(listing1))
    assertTrue(result.contains(listing3))
    assertFalse(result.contains(listing2))
  }

  @Test
  fun getAllRentalListingsForUser_filtersWhenOwnerBlockedCurrentUser() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val listing1 = createTestListing("listing-1", ownerId = user1Id)
    val listing2 = createTestListing("listing-2", ownerId = user2Id)
    val listing3 = createTestListing("listing-3", ownerId = "user-3")

    coEvery { remoteRepository.getAllRentalListings() } returns listOf(listing1, listing2, listing3)
    // User1 has not blocked anyone
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns emptyList()
    // User2 has blocked user1
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns listOf(user1Id)
    coEvery { profileRepository.getBlockedUserIds("user-3") } returns emptyList()

    val result = hybridRepository.getAllRentalListingsForUser(user1Id)

    // Should filter out listing2 (user2's listing) because user2 blocked user1
    assertEquals(2, result.size)
    assertTrue(result.contains(listing1))
    assertTrue(result.contains(listing3))
    assertFalse(result.contains(listing2))
  }

  @Test
  fun getAllRentalListingsForUser_returnsOwnListings() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val listing1 = createTestListing("listing-1", ownerId = user1Id)
    val listing2 = createTestListing("listing-2", ownerId = "user-2")

    coEvery { remoteRepository.getAllRentalListings() } returns listOf(listing1, listing2)
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns emptyList()
    coEvery { profileRepository.getBlockedUserIds("user-2") } returns emptyList()

    val result = hybridRepository.getAllRentalListingsForUser(user1Id)

    // Should include own listing even if blocked
    assertTrue(result.contains(listing1))
  }

  @Test
  fun getRentalListingForUser_nullUserId_returnsListing() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val listing = createTestListing("listing-1")
    coEvery { remoteRepository.getRentalListing("listing-1") } returns listing

    val result = hybridRepository.getRentalListingForUser("listing-1", null)

    assertEquals(listing, result)
    coVerify(exactly = 0) { profileRepository.getBlockedUserIds(any()) }
  }

  @Test
  fun getRentalListingForUser_ownerViewingOwnListing_returnsListing() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val listing = createTestListing("listing-1", ownerId = user1Id)
    coEvery { remoteRepository.getRentalListing("listing-1") } returns listing

    val result = hybridRepository.getRentalListingForUser("listing-1", user1Id)

    assertEquals(listing, result)
    coVerify(exactly = 0) { profileRepository.getBlockedUserIds(any()) }
  }

  @Test
  fun getRentalListingForUser_throwsWhenCurrentUserBlockedOwner() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val listing = createTestListing("listing-1", ownerId = user2Id)

    coEvery { remoteRepository.getRentalListing("listing-1") } returns listing
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns listOf(user2Id)
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns emptyList()

    val result = runCatching { hybridRepository.getRentalListingForUser("listing-1", user1Id) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
    assertTrue(result.exceptionOrNull()?.message?.contains("blocking restrictions") == true)
  }

  @Test
  fun getRentalListingForUser_throwsWhenOwnerBlockedCurrentUser() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val listing = createTestListing("listing-1", ownerId = user2Id)

    coEvery { remoteRepository.getRentalListing("listing-1") } returns listing
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns emptyList()
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns listOf(user1Id)

    val result = runCatching { hybridRepository.getRentalListingForUser("listing-1", user1Id) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
    assertTrue(result.exceptionOrNull()?.message?.contains("blocking restrictions") == true)
  }

  @Test
  fun getRentalListingForUser_returnsWhenNotBlocked() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val listing = createTestListing("listing-1", ownerId = user2Id)

    coEvery { remoteRepository.getRentalListing("listing-1") } returns listing
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns emptyList()
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns emptyList()

    val result = hybridRepository.getRentalListingForUser("listing-1", user1Id)

    assertEquals(listing, result)
  }

  @Test
  fun getAllRentalListingsForUser_handlesBlockedListFetchFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val listing1 = createTestListing("listing-1", ownerId = user1Id)
    val listing2 = createTestListing("listing-2", ownerId = "user-2")

    coEvery { remoteRepository.getAllRentalListings() } returns listOf(listing1, listing2)
    // Simulate failure when fetching blocked list - should default to empty list
    coEvery { profileRepository.getBlockedUserIds(user1Id) } throws IOException("Network error")

    val result = hybridRepository.getAllRentalListingsForUser(user1Id)

    // Should return all listings when blocked list fetch fails (defaults to empty)
    assertEquals(2, result.size)
  }

  @Test
  fun getRentalListingForUser_handlesBlockedListFetchFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val listing = createTestListing("listing-1", ownerId = user2Id)

    coEvery { remoteRepository.getRentalListing("listing-1") } returns listing
    // Simulate failure when fetching blocked lists - should default to empty (not blocked)
    coEvery { profileRepository.getBlockedUserIds(user1Id) } throws IOException("Network error")
    coEvery { profileRepository.getBlockedUserIds(user2Id) } throws IOException("Network error")

    val result = hybridRepository.getRentalListingForUser("listing-1", user1Id)

    // Should return listing when blocked list fetch fails (defaults to not blocked)
    assertEquals(listing, result)
  }

  @Test
  fun getAllRentalListingsForUser_usesCacheForMultipleListingsFromSameOwner() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val listing1 = createTestListing("listing-1", ownerId = user1Id)
    val listing2 = createTestListing("listing-2", ownerId = user2Id)
    val listing3 = createTestListing("listing-3", ownerId = user2Id) // Same owner as listing2

    coEvery { remoteRepository.getAllRentalListings() } returns listOf(listing1, listing2, listing3)
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns emptyList()
    // Should only be called once for user2, even though there are 2 listings
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns listOf(user1Id)

    val result = hybridRepository.getAllRentalListingsForUser(user1Id)

    // Should filter out both listings from user2
    assertEquals(1, result.size)
    assertTrue(result.contains(listing1))
    // Verify getBlockedUserIds was called only once for user2 (cache used)
    coVerify(exactly = 1) { profileRepository.getBlockedUserIds(user2Id) }
  }

  private fun createTestListing(
      uid: String,
      ownerId: String = "user-1",
      title: String = "Test Listing",
      residencyName: String = "Vortex",
      ownerName: String? = null
  ): RentalListing {
    val fixedTimestamp = Timestamp(1000000L, 0)
    return RentalListing(
        uid = uid,
        ownerId = ownerId,
        ownerName = ownerName,
        postedAt = fixedTimestamp,
        residencyName = residencyName,
        title = title,
        roomType = RoomType.STUDIO,
        pricePerMonth = 1200.0,
        areaInM2 = 25,
        startDate = fixedTimestamp,
        description = "Test description",
        imageUrls = emptyList(),
        location = Location("Test Location", 46.5, 6.5),
        status = RentalStatus.POSTED)
  }
}
