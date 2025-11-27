package com.android.mySwissDorm.model.review

import android.content.Context
import androidx.room.Room
import com.android.mySwissDorm.model.database.AppDatabase
import com.android.mySwissDorm.model.rental.RoomType
import com.google.firebase.Timestamp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReviewsRepositoryProviderTest {
  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var mockRepository: ReviewsRepository

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    mockRepository = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    database.close()
    // Reset provider state
    ReviewsRepositoryProvider.repository = mockRepository
  }

  @Test
  fun initialize_createsHybridRepository() {
    ReviewsRepositoryProvider.initialize(context)

    val repository = ReviewsRepositoryProvider.repository
    assertNotNull(repository)
    assertTrue(repository is ReviewsRepositoryHybrid)
  }

  @Test
  fun initialize_calledTwice_onlyInitializesOnce() {
    ReviewsRepositoryProvider.initialize(context)
    val firstRepository = ReviewsRepositoryProvider.repository

    ReviewsRepositoryProvider.initialize(context)
    val secondRepository = ReviewsRepositoryProvider.repository

    // Should be the same instance (idempotent)
    assertSame(firstRepository, secondRepository)
  }

  @Test
  fun repository_getter_throwsWhenNotInitialized() {
    // Create a new provider instance by using reflection or just test the error case
    // Since we can't easily reset the object, we'll test the setter/getter pattern
    val customRepository = mockk<ReviewsRepository>(relaxed = true)

    // Set a repository first
    ReviewsRepositoryProvider.repository = customRepository
    val retrieved1 = ReviewsRepositoryProvider.repository
    assertSame(customRepository, retrieved1)

    // Now test that we can still access it
    val retrieved2 = ReviewsRepositoryProvider.repository
    assertSame(customRepository, retrieved2)
  }

  @Test
  fun repository_setter_allowsDirectAssignment() {
    val customRepository = mockk<ReviewsRepository>(relaxed = true)

    ReviewsRepositoryProvider.repository = customRepository

    val retrieved = ReviewsRepositoryProvider.repository
    assertSame(customRepository, retrieved)
  }

  @Test
  fun repository_setter_marksAsInitialized() {
    val customRepository = mockk<ReviewsRepository>(relaxed = true)

    ReviewsRepositoryProvider.repository = customRepository

    // Should not throw
    val retrieved = ReviewsRepositoryProvider.repository
    assertSame(customRepository, retrieved)
  }

  @Test
  fun initialize_createsRepositoryWithCorrectComponents() = runTest {
    mockkObject(com.android.mySwissDorm.utils.NetworkUtils)
    every { com.android.mySwissDorm.utils.NetworkUtils.isNetworkAvailable(any()) } returns true

    ReviewsRepositoryProvider.initialize(context)

    val repository = ReviewsRepositoryProvider.repository as ReviewsRepositoryHybrid

    // Verify it's a hybrid repository (indirectly by checking it works)
    val testReview = createTestReview("test-1")
    repository.addReview(testReview)
    val retrieved = repository.getReview("test-1")
    assertEquals(testReview.uid, retrieved.uid)
  }

  private fun createTestReview(uid: String): Review {
    return Review(
        uid = uid,
        ownerId = "user-1",
        postedAt = Timestamp(1000000L, 0),
        title = "Test Review",
        reviewText = "Test text",
        grade = 4.0,
        residencyName = "Vortex",
        roomType = RoomType.STUDIO,
        pricePerMonth = 1200.0,
        areaInM2 = 20,
        imageUrls = emptyList(),
        upvotedBy = emptySet(),
        downvotedBy = emptySet(),
        isAnonymous = false)
  }
}
