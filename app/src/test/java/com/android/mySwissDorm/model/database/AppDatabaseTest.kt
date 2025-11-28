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
class AppDatabaseTest {
  private lateinit var database: AppDatabase
  private lateinit var reviewDao: ReviewDao
  private lateinit var listingDao: RentalListingDao

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    reviewDao = database.reviewDao()
    listingDao = database.rentalListingDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun database_hasBothDaos() {
    assertNotNull(database.reviewDao())
    assertNotNull(database.rentalListingDao())
  }

  @Test
  fun database_storesReviewsAndListingsSeparately() = runTest {
    val review = createTestReviewEntity("review-1")
    val listing = createTestRentalListingEntity("listing-1")

    reviewDao.insertReview(review)
    listingDao.insertRentalListing(listing)

    val reviews = reviewDao.getAllReviews()
    val listings = listingDao.getAllRentalListings()

    assertEquals(1, reviews.size)
    assertEquals(1, listings.size)
    assertEquals("review-1", reviews[0].uid)
    assertEquals("listing-1", listings[0].uid)
  }

  @Test
  fun database_allowsMultipleReviewsAndListings() = runTest {
    reviewDao.insertReview(createTestReviewEntity("review-1"))
    reviewDao.insertReview(createTestReviewEntity("review-2"))
    listingDao.insertRentalListing(createTestRentalListingEntity("listing-1"))
    listingDao.insertRentalListing(createTestRentalListingEntity("listing-2"))
    listingDao.insertRentalListing(createTestRentalListingEntity("listing-3"))

    val reviews = reviewDao.getAllReviews()
    val listings = listingDao.getAllRentalListings()

    assertEquals(2, reviews.size)
    assertEquals(3, listings.size)
  }

  @Test
  fun database_deletesReviewsAndListingsIndependently() = runTest {
    reviewDao.insertReview(createTestReviewEntity("review-1"))
    listingDao.insertRentalListing(createTestRentalListingEntity("listing-1"))

    reviewDao.deleteReview("review-1")

    val reviews = reviewDao.getAllReviews()
    val listings = listingDao.getAllRentalListings()

    assertTrue(reviews.isEmpty())
    assertEquals(1, listings.size)
  }

  @Test
  fun database_preservesTypeConverters() = runTest {
    val review =
        ReviewEntity(
            uid = "review-1",
            ownerId = "user-1",
            postedAt = Timestamp(1000, 500000),
            title = "Test",
            reviewText = "Text",
            grade = 4.0,
            residencyName = "Residency",
            roomType = RoomType.APARTMENT.name,
            pricePerMonth = 500.0,
            areaInM2 = 20,
            imageUrls = listOf("url1", "url2"),
            upvotedBy = listOf("user-2"),
            downvotedBy = emptyList(),
            isAnonymous = false)

    val listing =
        RentalListingEntity(
            uid = "listing-1",
            ownerId = "user-1",
            postedAt = Timestamp.now(),
            residencyName = "Residency",
            title = "Test",
            roomType = RoomType.STUDIO.name,
            pricePerMonth = 500.0,
            areaInM2 = 20,
            startDate = Timestamp.now(),
            description = "Description",
            imageUrls = listOf("img1", "img2"),
            status = RentalStatus.POSTED.name,
            location = Location("Lausanne", 46.5197, 6.6323))

    reviewDao.insertReview(review)
    listingDao.insertRentalListing(listing)

    val retrievedReview = reviewDao.getReview("review-1")
    val retrievedListing = listingDao.getRentalListing("listing-1")

    assertNotNull(retrievedReview)
    assertNotNull(retrievedListing)

    assertEquals(2, retrievedReview!!.imageUrls.size)
    assertEquals(1, retrievedReview.upvotedBy.size)
    assertEquals("Lausanne", retrievedListing!!.location.name)
    assertEquals(46.5197, retrievedListing.location.latitude, 0.0001)
  }

  private fun createTestReviewEntity(uid: String): ReviewEntity {
    return ReviewEntity(
        uid = uid,
        ownerId = "user-1",
        postedAt = Timestamp.now(),
        title = "Test Review",
        reviewText = "Test text",
        grade = 4.0,
        residencyName = "EPFL Residency",
        roomType = RoomType.STUDIO.name,
        pricePerMonth = 500.0,
        areaInM2 = 20,
        imageUrls = emptyList(),
        upvotedBy = emptyList(),
        downvotedBy = emptyList(),
        isAnonymous = false)
  }

  private fun createTestRentalListingEntity(uid: String): RentalListingEntity {
    return RentalListingEntity(
        uid = uid,
        ownerId = "user-1",
        postedAt = Timestamp.now(),
        residencyName = "EPFL Residency",
        title = "Test Listing",
        roomType = RoomType.STUDIO.name,
        pricePerMonth = 500.0,
        areaInM2 = 20,
        startDate = Timestamp.now(),
        description = "Test description",
        imageUrls = emptyList(),
        status = RentalStatus.POSTED.name,
        location = Location("Lausanne", 46.5197, 6.6323))
  }
}
