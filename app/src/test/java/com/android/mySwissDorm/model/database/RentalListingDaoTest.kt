package com.android.mySwissDorm.model.database

import androidx.room.Room
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.rental.RoomType
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
class RentalListingDaoTest {
  private lateinit var database: AppDatabase
  private lateinit var listingDao: RentalListingDao

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    listingDao = database.rentalListingDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun insertRentalListing_insertsSuccessfully() = runTest {
    val entity = createTestRentalListingEntity("listing-1")
    listingDao.insertRentalListing(entity)

    val retrieved = listingDao.getRentalListing("listing-1")
    assertNotNull(retrieved)
    assertEquals("listing-1", retrieved!!.uid)
  }

  @Test
  fun getRentalListing_returnsNullWhenNotFound() = runTest {
    val result = listingDao.getRentalListing("non-existent")
    assertNull(result)
  }

  @Test
  fun getAllRentalListings_returnsAllListings() = runTest {
    val entity1 = createTestRentalListingEntity("listing-1")
    val entity2 = createTestRentalListingEntity("listing-2")
    val entity3 = createTestRentalListingEntity("listing-3")

    listingDao.insertRentalListing(entity1)
    listingDao.insertRentalListing(entity2)
    listingDao.insertRentalListing(entity3)

    val allListings = listingDao.getAllRentalListings()
    assertEquals(3, allListings.size)
  }

  @Test
  fun getAllRentalListings_returnsEmptyListWhenNoListings() = runTest {
    val allListings = listingDao.getAllRentalListings()
    assertTrue(allListings.isEmpty())
  }

  @Test
  fun getAllRentalListingsByUser_returnsOnlyUserListings() = runTest {
    val entity1 = createTestRentalListingEntity("listing-1", ownerId = "user-1")
    val entity2 = createTestRentalListingEntity("listing-2", ownerId = "user-2")
    val entity3 = createTestRentalListingEntity("listing-3", ownerId = "user-1")

    listingDao.insertRentalListing(entity1)
    listingDao.insertRentalListing(entity2)
    listingDao.insertRentalListing(entity3)

    val userListings = listingDao.getAllRentalListingsByUser("user-1")
    assertEquals(2, userListings.size)
    assertTrue(userListings.all { it.ownerId == "user-1" })
  }

  @Test
  fun insertRentalListings_insertsMultipleListings() = runTest {
    val entities =
        listOf(
            createTestRentalListingEntity("listing-1"),
            createTestRentalListingEntity("listing-2"),
            createTestRentalListingEntity("listing-3"))

    listingDao.insertRentalListings(entities)

    val allListings = listingDao.getAllRentalListings()
    assertEquals(3, allListings.size)
  }

  @Test
  fun updateRentalListing_updatesExistingListing() = runTest {
    val entity = createTestRentalListingEntity("listing-1", title = "Original Title")
    listingDao.insertRentalListing(entity)

    val updated = entity.copy(title = "Updated Title")
    listingDao.updateRentalListing(updated)

    val retrieved = listingDao.getRentalListing("listing-1")
    assertNotNull(retrieved)
    assertEquals("Updated Title", retrieved!!.title)
  }

  @Test
  fun insertRentalListing_replacesOnConflict() = runTest {
    val entity1 = createTestRentalListingEntity("listing-1", title = "Original")
    listingDao.insertRentalListing(entity1)

    val entity2 = createTestRentalListingEntity("listing-1", title = "Replaced")
    listingDao.insertRentalListing(entity2)

    val retrieved = listingDao.getRentalListing("listing-1")
    assertNotNull(retrieved)
    assertEquals("Replaced", retrieved!!.title)
  }

  @Test
  fun deleteRentalListing_deletesSpecificListing() = runTest {
    val entity1 = createTestRentalListingEntity("listing-1")
    val entity2 = createTestRentalListingEntity("listing-2")
    listingDao.insertRentalListing(entity1)
    listingDao.insertRentalListing(entity2)

    listingDao.deleteRentalListing("listing-1")

    assertNull(listingDao.getRentalListing("listing-1"))
    assertNotNull(listingDao.getRentalListing("listing-2"))
  }

  @Test
  fun getAllRentalListingsByUser_returnsEmptyWhenUserHasNoListings() = runTest {
    listingDao.insertRentalListing(createTestRentalListingEntity("listing-1", ownerId = "user-1"))

    val userListings = listingDao.getAllRentalListingsByUser("user-2")
    assertTrue(userListings.isEmpty())
  }

  @Test
  fun insertRentalListing_preservesLocation() = runTest {
    val location = Location("Geneva", 46.2044, 6.1432)
    val entity = createTestRentalListingEntity("listing-1", location = location)
    listingDao.insertRentalListing(entity)

    val retrieved = listingDao.getRentalListing("listing-1")
    assertNotNull(retrieved)
    assertEquals(location.name, retrieved!!.location.name)
    assertEquals(location.latitude, retrieved.location.latitude, 0.0001)
    assertEquals(location.longitude, retrieved.location.longitude, 0.0001)
  }

  @Test
  fun insertRentalListing_preservesImageUrls() = runTest {
    val imageUrls = listOf("url1", "url2", "url3")
    val entity = createTestRentalListingEntity("listing-1", imageUrls = imageUrls)
    listingDao.insertRentalListing(entity)

    val retrieved = listingDao.getRentalListing("listing-1")
    assertNotNull(retrieved)
    assertEquals(imageUrls, retrieved!!.imageUrls)
  }

  private fun createTestRentalListingEntity(
      uid: String,
      ownerId: String = "user-1",
      title: String = "Test Listing",
      location: Location = Location("Lausanne", 46.5197, 6.6323),
      imageUrls: List<String> = emptyList()
  ): RentalListingEntity {
    return RentalListingEntity(
        uid = uid,
        ownerId = ownerId,
        postedAt = Timestamp.now(),
        residencyName = "EPFL Residency",
        title = title,
        roomType = RoomType.STUDIO.name,
        pricePerMonth = 500.0,
        areaInM2 = 20,
        startDate = Timestamp.now(),
        description = "Test description",
        imageUrls = imageUrls,
        status = RentalStatus.POSTED.name,
        location = location)
  }
}
