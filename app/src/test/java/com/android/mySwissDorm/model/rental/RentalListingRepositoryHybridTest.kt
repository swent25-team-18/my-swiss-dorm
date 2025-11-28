package com.android.mySwissDorm.model.rental

import android.content.Context
import androidx.room.Room
import com.android.mySwissDorm.model.database.AppDatabase
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.utils.NetworkUtils
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
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

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    localRepository = RentalListingRepositoryLocal(database.rentalListingDao())
    remoteRepository = mockk(relaxed = true)
    hybridRepository = RentalListingRepositoryHybrid(context, remoteRepository, localRepository)
  }

  @After
  fun tearDown() {
    database.close()
    unmockkObject(NetworkUtils)
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

  private fun createTestListing(
      uid: String,
      ownerId: String = "user-1",
      title: String = "Test Listing",
      residencyName: String = "Vortex"
  ): RentalListing {
    val fixedTimestamp = Timestamp(1000000L, 0)
    return RentalListing(
        uid = uid,
        ownerId = ownerId,
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
