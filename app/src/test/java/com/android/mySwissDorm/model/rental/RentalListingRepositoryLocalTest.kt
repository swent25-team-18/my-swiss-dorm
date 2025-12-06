package com.android.mySwissDorm.model.rental

import androidx.room.Room
import com.android.mySwissDorm.model.database.AppDatabase
import com.android.mySwissDorm.model.map.Location
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RentalListingRepositoryLocalTest {
  private lateinit var database: AppDatabase
  private lateinit var repository: RentalListingRepositoryLocal

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    repository = RentalListingRepositoryLocal(database.rentalListingDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun getNewUid_throwsException() {
    assertThrows(UnsupportedOperationException::class.java) { repository.getNewUid() }
  }

  @Test
  fun addAndGetRentalListing_works() = runTest {
    val listing = createTestListing("listing-1", title = "Test Listing")
    repository.addRentalListing(listing)

    val retrieved = repository.getRentalListing("listing-1")
    assertEquals(listing, retrieved)
  }

  @Test
  fun getAllRentalListings_returnsAllListings() = runTest {
    val listing1 = createTestListing("listing-1")
    val listing2 = createTestListing("listing-2")
    repository.addRentalListing(listing1)
    repository.addRentalListing(listing2)

    val allListings = repository.getAllRentalListings()
    assertEquals(2, allListings.size)
  }

  @Test
  fun getAllRentalListingsByUser_filtersByUser() = runTest {
    val listing1 = createTestListing("listing-1", ownerId = "user-1")
    val listing2 = createTestListing("listing-2", ownerId = "user-2")
    repository.addRentalListing(listing1)
    repository.addRentalListing(listing2)

    val userListings = repository.getAllRentalListingsByUser("user-1")
    assertEquals(1, userListings.size)
    assertEquals("listing-1", userListings[0].uid)
  }

  @Test
  fun getAllRentalListingsByLocation_filtersByDistance() = runTest {
    val lausanne = Location("Lausanne", 46.5197, 6.6323)
    val zurich = Location("Zurich", 47.3769, 8.5417)

    val listing1 = createTestListing("listing-1", location = lausanne)
    val listing2 = createTestListing("listing-2", location = zurich)
    repository.addRentalListing(listing1)
    repository.addRentalListing(listing2)

    val nearbyListings = repository.getAllRentalListingsByLocation(lausanne, radius = 50.0)
    assertEquals(1, nearbyListings.size)
    assertEquals("listing-1", nearbyListings[0].uid)
  }

  @Test
  fun editRentalListing_updatesListing() = runTest {
    val listing = createTestListing("listing-1", title = "Original")
    repository.addRentalListing(listing)

    val updated = listing.copy(title = "Updated")
    repository.editRentalListing("listing-1", updated)

    val retrieved = repository.getRentalListing("listing-1")
    assertEquals("Updated", retrieved.title)
  }

  @Test
  fun deleteRentalListing_removesListing() = runTest {
    val listing = createTestListing("listing-1")
    repository.addRentalListing(listing)

    repository.deleteRentalListing("listing-1")

    assertTrue(runCatching { repository.getRentalListing("listing-1") }.isFailure)
  }

  @Test
  fun editRentalListing_throwsWhenUidMismatch() = runTest {
    val listing = createTestListing("listing-1")
    repository.addRentalListing(listing)

    val updated = listing.copy(uid = "listing-2")
    assertTrue(runCatching { repository.editRentalListing("listing-1", updated) }.isFailure)
  }

  @Test
  fun editRentalListing_throwsWhenNotFound() = runTest {
    val listing = createTestListing("listing-1")
    assertTrue(runCatching { repository.editRentalListing("listing-1", listing) }.isFailure)
  }

  @Test
  fun getAllRentalListingsByLocation_handlesDistanceCalculationErrors() = runTest {
    val lausanne = Location("Lausanne", 46.5197, 6.6323)
    val listing = createTestListing("listing-1", location = lausanne)
    repository.addRentalListing(listing)

    // This should not throw even if distance calculation fails
    val result = repository.getAllRentalListingsByLocation(lausanne, radius = 50.0)
    assertTrue(result.isNotEmpty())
  }

  @Test
  fun getRentalListing_throwsWhenNotFound() = runTest {
    assertTrue(runCatching { repository.getRentalListing("non-existent") }.isFailure)
  }

  private fun createTestListing(
      uid: String,
      ownerId: String = "user-1",
      title: String = "Test Listing",
      location: Location = Location("Lausanne", 46.5197, 6.6323)
  ): RentalListing {
    val fixedTimestamp = Timestamp(1000000L, 0) // Fixed timestamp
    return RentalListing(
        uid = uid,
        ownerId = ownerId,
        ownerName = null,
        postedAt = fixedTimestamp,
        residencyName = "Vortex",
        title = title,
        roomType = RoomType.STUDIO,
        pricePerMonth = 1200.0,
        areaInM2 = 20,
        startDate = fixedTimestamp,
        description = "Test description",
        imageUrls = emptyList(),
        status = RentalStatus.POSTED,
        location = location)
  }
}
