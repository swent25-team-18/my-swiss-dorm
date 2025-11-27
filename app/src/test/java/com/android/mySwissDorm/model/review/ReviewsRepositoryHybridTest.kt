package com.android.mySwissDorm.model.review

import android.content.Context
import androidx.room.Room
import com.android.mySwissDorm.model.database.AppDatabase
import com.android.mySwissDorm.model.rental.RoomType
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReviewsRepositoryHybridTest {
  private lateinit var context: Context
  private lateinit var remoteRepository: ReviewsRepositoryFirestore
  private lateinit var localRepository: ReviewsRepositoryLocal
  private lateinit var database: AppDatabase
  private lateinit var hybridRepository: ReviewsRepositoryHybrid

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    localRepository = ReviewsRepositoryLocal(database.reviewDao())
    remoteRepository = mockk(relaxed = true)
    hybridRepository = ReviewsRepositoryHybrid(context, remoteRepository, localRepository)
  }

  @After
  fun tearDown() {
    database.close()
    unmockkObject(NetworkUtils)
  }

  @Test
  fun getAllReviews_online_usesRemoteAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val remoteReviews = listOf(createTestReview("review-1"), createTestReview("review-2"))
    coEvery { remoteRepository.getAllReviews() } returns remoteReviews

    val result = hybridRepository.getAllReviews()

    assertEquals(remoteReviews, result)
    coVerify { remoteRepository.getAllReviews() }
    // Verify synced to local
    assertEquals(2, localRepository.getAllReviews().size)
  }

  @Test
  fun getAllReviews_online_fallsBackToLocalOnNetworkError() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true
    every { NetworkUtils.isNetworkException(any()) } returns true

    val localReview = createTestReview("review-1")
    localRepository.addReview(localReview)

    coEvery { remoteRepository.getAllReviews() } throws IOException("Network error")

    val result = hybridRepository.getAllReviews()

    assertEquals(listOf(localReview), result)
  }

  @Test
  fun getAllReviews_online_fallsBackToLocalOnTimeout() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true
    every { NetworkUtils.isNetworkException(any()) } returns false

    val localReview = createTestReview("review-1")
    localRepository.addReview(localReview)

    coEvery { remoteRepository.getAllReviews() } coAnswers
        {
          withTimeout(1) {
            delay(100)
            emptyList<Review>()
          }
        }

    val result = hybridRepository.getAllReviews()
    assertEquals(listOf(localReview), result)
  }

  @Test
  fun getReview_online_fallsBackToLocalOnNotFound() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val localReview = createTestReview("review-1")
    localRepository.addReview(localReview)

    coEvery { remoteRepository.getReview("review-1") } throws NoSuchElementException("Not found")

    val result = hybridRepository.getReview("review-1")
    assertEquals(localReview, result)
  }

  @Test
  fun getAllReviews_offline_usesLocalImmediately() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val localReview = createTestReview("review-1")
    localRepository.addReview(localReview)

    val result = hybridRepository.getAllReviews()

    assertEquals(listOf(localReview), result)
    coVerify(exactly = 0) { remoteRepository.getAllReviews() }
  }

  @Test
  fun addReview_online_succeedsAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val review = createTestReview("review-1")
    coEvery { remoteRepository.addReview(review) } returns Unit

    hybridRepository.addReview(review)

    coVerify { remoteRepository.addReview(review) }
    assertEquals(review, localRepository.getReview("review-1"))
  }

  @Test
  fun addReview_offline_throwsUnsupportedOperationException() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val review = createTestReview("review-1")
    val result = runCatching { hybridRepository.addReview(review) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    coVerify(exactly = 0) { remoteRepository.addReview(any()) }
  }

  @Test
  fun addReview_online_throwsOnNetworkError() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true
    every { NetworkUtils.isNetworkException(any()) } returns true

    val review = createTestReview("review-1")
    coEvery { remoteRepository.addReview(review) } throws IOException("Network error")

    val result = runCatching { hybridRepository.addReview(review) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
  }

  @Test
  fun upvoteReview_offline_throwsUnsupportedOperationException() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val result = runCatching { hybridRepository.upvoteReview("review-1", "user-2") }

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
  fun getAllReviews_online_propagatesNonNetworkExceptions() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true
    every { NetworkUtils.isNetworkException(any()) } returns false

    coEvery { remoteRepository.getAllReviews() } throws IllegalArgumentException("Invalid argument")

    val result = runCatching { hybridRepository.getAllReviews() }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun getAllReviewsByUser_online_usesRemoteAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val remoteReviews = listOf(createTestReview("review-1"), createTestReview("review-2"))
    coEvery { remoteRepository.getAllReviewsByUser("user-1") } returns remoteReviews

    val result = hybridRepository.getAllReviewsByUser("user-1")

    assertEquals(remoteReviews, result)
    coVerify { remoteRepository.getAllReviewsByUser("user-1") }
    assertEquals(2, localRepository.getAllReviewsByUser("user-1").size)
  }

  @Test
  fun getAllReviewsByUser_offline_usesLocalImmediately() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val localReview = createTestReview("review-1", ownerId = "user-1")
    localRepository.addReview(localReview)

    val result = hybridRepository.getAllReviewsByUser("user-1")

    assertEquals(listOf(localReview), result)
    coVerify(exactly = 0) { remoteRepository.getAllReviewsByUser(any()) }
  }

  @Test
  fun getAllReviewsByResidency_online_usesRemoteAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val remoteReviews = listOf(createTestReview("review-1", residencyName = "Vortex"))
    coEvery { remoteRepository.getAllReviewsByResidency("Vortex") } returns remoteReviews

    val result = hybridRepository.getAllReviewsByResidency("Vortex")

    assertEquals(remoteReviews, result)
    coVerify { remoteRepository.getAllReviewsByResidency("Vortex") }
    assertEquals(1, localRepository.getAllReviewsByResidency("Vortex").size)
  }

  @Test
  fun getReview_online_usesRemoteAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val remoteReview = createTestReview("review-1")
    coEvery { remoteRepository.getReview("review-1") } returns remoteReview

    val result = hybridRepository.getReview("review-1")

    assertEquals(remoteReview, result)
    coVerify { remoteRepository.getReview("review-1") }
    assertEquals(remoteReview, localRepository.getReview("review-1"))
  }

  @Test
  fun getReview_offline_usesLocalImmediately() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val localReview = createTestReview("review-1")
    localRepository.addReview(localReview)

    val result = hybridRepository.getReview("review-1")

    assertEquals(localReview, result)
    coVerify(exactly = 0) { remoteRepository.getReview(any()) }
  }

  @Test
  fun editReview_online_succeedsAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val review = createTestReview("review-1")
    val updatedReview = review.copy(title = "Updated Title")
    coEvery { remoteRepository.editReview("review-1", updatedReview) } returns Unit

    hybridRepository.editReview("review-1", updatedReview)

    coVerify { remoteRepository.editReview("review-1", updatedReview) }
    assertEquals(updatedReview.title, localRepository.getReview("review-1").title)
  }

  @Test
  fun editReview_offline_throwsUnsupportedOperationException() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val review = createTestReview("review-1")
    val result = runCatching { hybridRepository.editReview("review-1", review) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    coVerify(exactly = 0) { remoteRepository.editReview(any(), any()) }
  }

  @Test
  fun deleteReview_online_succeedsAndSyncsToLocal() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val review = createTestReview("review-1")
    localRepository.addReview(review)
    coEvery { remoteRepository.deleteReview("review-1") } returns Unit

    hybridRepository.deleteReview("review-1")

    coVerify { remoteRepository.deleteReview("review-1") }
    val result = runCatching { localRepository.getReview("review-1") }
    assertTrue(result.isFailure)
  }

  @Test
  fun deleteReview_offline_throwsUnsupportedOperationException() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val result = runCatching { hybridRepository.deleteReview("review-1") }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    coVerify(exactly = 0) { remoteRepository.deleteReview(any()) }
  }

  @Test
  fun downvoteReview_offline_throwsUnsupportedOperationException() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val result = runCatching { hybridRepository.downvoteReview("review-1", "user-2") }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
  }

  @Test
  fun removeVote_offline_throwsUnsupportedOperationException() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val result = runCatching { hybridRepository.removeVote("review-1", "user-2") }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
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
  fun addReview_online_localSyncFailureDoesNotCrash() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val review = createTestReview("review-1")
    coEvery { remoteRepository.addReview(review) } returns Unit
    // Make local sync fail
    localRepository.addReview(review) // Add it first so addReview will fail

    // Should not throw even if local sync fails
    hybridRepository.addReview(review)

    coVerify { remoteRepository.addReview(review) }
  }

  @Test
  fun getAllReviews_online_emptyListDoesNotSync() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    coEvery { remoteRepository.getAllReviews() } returns emptyList()

    val result = hybridRepository.getAllReviews()

    assertEquals(emptyList<Review>(), result)
    assertEquals(0, localRepository.getAllReviews().size)
  }

  private fun createTestReview(
      uid: String,
      ownerId: String = "user-1",
      title: String = "Test Review",
      residencyName: String = "Vortex",
      upvotedBy: Set<String> = emptySet(),
      downvotedBy: Set<String> = emptySet()
  ): Review {
    val fixedTimestamp = Timestamp(1000000L, 0)
    return Review(
        uid = uid,
        ownerId = ownerId,
        postedAt = fixedTimestamp,
        title = title,
        reviewText = "Test text",
        grade = 4.0,
        residencyName = residencyName,
        roomType = RoomType.STUDIO,
        pricePerMonth = 1200.0,
        areaInM2 = 20,
        imageUrls = emptyList(),
        upvotedBy = upvotedBy,
        downvotedBy = downvotedBy,
        isAnonymous = false)
  }
}
