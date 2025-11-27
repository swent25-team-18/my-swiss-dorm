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
