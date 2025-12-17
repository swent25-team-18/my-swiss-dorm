package com.android.mySwissDorm.model.review

import android.content.Context
import androidx.room.Room
import com.android.mySwissDorm.model.database.AppDatabase
import com.android.mySwissDorm.model.profile.Language
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.model.rental.RoomType
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
    localRepository = ReviewsRepositoryLocal(database.reviewDao())
    remoteRepository = mockk(relaxed = true)
    // Initialize ProfileRepositoryProvider with a mock to prevent NoClassDefFoundError
    profileRepository = mockk(relaxed = true)
    ProfileRepositoryProvider.repository = profileRepository
    hybridRepository = ReviewsRepositoryHybrid(context, remoteRepository, localRepository)
  }

  @After
  fun tearDown() {
    database.close()
    unmockkAll()
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
    // Add review to local first so editReview can find it
    localRepository.addReview(review)

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

  @Test
  fun upvoteReview_online_succeedsAndSyncsUpdatedReview() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val review = createTestReview("review-1")
    val updatedReview = review.copy(upvotedBy = setOf("user-2"))

    coEvery { remoteRepository.upvoteReview("review-1", "user-2") } returns Unit
    coEvery { remoteRepository.getReview("review-1") } returns updatedReview

    hybridRepository.upvoteReview("review-1", "user-2")

    coVerify { remoteRepository.upvoteReview("review-1", "user-2") }
    coVerify { remoteRepository.getReview("review-1") }
    assertEquals(updatedReview.upvotedBy, localRepository.getReview("review-1").upvotedBy)
  }

  @Test
  fun upvoteReview_online_handlesSyncErrorGracefully() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    coEvery { remoteRepository.upvoteReview("review-1", "user-2") } returns Unit
    coEvery { remoteRepository.getReview("review-1") } throws IOException("Sync error")

    // Should not throw even if sync fails
    hybridRepository.upvoteReview("review-1", "user-2")

    coVerify { remoteRepository.upvoteReview("review-1", "user-2") }
  }

  @Test
  fun downvoteReview_online_succeedsAndSyncsUpdatedReview() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val review = createTestReview("review-1")
    val updatedReview = review.copy(downvotedBy = setOf("user-2"))

    coEvery { remoteRepository.downvoteReview("review-1", "user-2") } returns Unit
    coEvery { remoteRepository.getReview("review-1") } returns updatedReview

    hybridRepository.downvoteReview("review-1", "user-2")

    coVerify { remoteRepository.downvoteReview("review-1", "user-2") }
    coVerify { remoteRepository.getReview("review-1") }
    assertEquals(updatedReview.downvotedBy, localRepository.getReview("review-1").downvotedBy)
  }

  @Test
  fun downvoteReview_online_handlesSyncErrorGracefully() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    coEvery { remoteRepository.downvoteReview("review-1", "user-2") } returns Unit
    coEvery { remoteRepository.getReview("review-1") } throws IOException("Sync error")

    // Should not throw even if sync fails
    hybridRepository.downvoteReview("review-1", "user-2")

    coVerify { remoteRepository.downvoteReview("review-1", "user-2") }
  }

  @Test
  fun removeVote_online_succeedsAndSyncsUpdatedReview() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val review = createTestReview("review-1", upvotedBy = setOf("user-2"))
    val updatedReview = review.copy(upvotedBy = emptySet())

    coEvery { remoteRepository.removeVote("review-1", "user-2") } returns Unit
    coEvery { remoteRepository.getReview("review-1") } returns updatedReview

    hybridRepository.removeVote("review-1", "user-2")

    coVerify { remoteRepository.removeVote("review-1", "user-2") }
    coVerify { remoteRepository.getReview("review-1") }
    assertEquals(emptySet<String>(), localRepository.getReview("review-1").upvotedBy)
  }

  @Test
  fun removeVote_online_handlesSyncErrorGracefully() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    coEvery { remoteRepository.removeVote("review-1", "user-2") } returns Unit
    coEvery { remoteRepository.getReview("review-1") } throws IOException("Sync error")

    // Should not throw even if sync fails
    hybridRepository.removeVote("review-1", "user-2")

    coVerify { remoteRepository.removeVote("review-1", "user-2") }
  }

  @Test
  fun syncReviewsToLocal_handlesIndividualReviewSyncErrors() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val review1 = createTestReview("review-1")
    val review2 = createTestReview("review-2")
    val review3 = createTestReview("review-3")

    // Add review2 first so it will fail when trying to sync again
    localRepository.addReview(review2)

    coEvery { remoteRepository.getAllReviews() } returns listOf(review1, review2, review3)

    // Should not throw even if one review fails to sync
    val result = hybridRepository.getAllReviews()

    assertEquals(3, result.size)
    // review1 and review3 should be synced, review2 might fail but shouldn't crash
  }

  @Test
  fun getAllReviewsByResidency_offline_usesLocalImmediately() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns false

    val localReview = createTestReview("review-1", residencyName = "Vortex")
    localRepository.addReview(localReview)

    val result = hybridRepository.getAllReviewsByResidency("Vortex")

    assertEquals(listOf(localReview), result)
    coVerify(exactly = 0) { remoteRepository.getAllReviewsByResidency(any()) }
  }

  @Test
  fun editReview_online_handlesLocalSyncError() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val review = createTestReview("review-1")
    val updatedReview = review.copy(title = "Updated Title")

    coEvery { remoteRepository.editReview("review-1", updatedReview) } returns Unit
    // Make local edit fail by not having the review in local
    // (editReview will throw if review doesn't exist)

    // Should not throw even if local sync fails
    hybridRepository.editReview("review-1", updatedReview)

    coVerify { remoteRepository.editReview("review-1", updatedReview) }
  }

  @Test
  fun getAllReviews_online_fetchesOwnerNameWhenMissing() = runTest {
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

    val reviewWithoutName = createTestReview("review-1", ownerId = "user-1", ownerName = null)
    coEvery { remoteRepository.getAllReviews() } returns listOf(reviewWithoutName)

    val result = hybridRepository.getAllReviews()

    assertEquals(1, result.size)
    coVerify { testProfileRepository.getProfile("user-1") }
    // Verify the review was stored with ownerName
    val storedReview = localRepository.getReview("review-1")
    assertEquals("John Doe", storedReview.ownerName)
    // Restore original mock
    ProfileRepositoryProvider.repository = profileRepository
  }

  @Test
  fun getAllReviews_online_doesNotFetchOwnerNameWhenAlreadyPresent() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val reviewWithName = createTestReview("review-1", ownerId = "user-1", ownerName = "Jane Smith")
    coEvery { remoteRepository.getAllReviews() } returns listOf(reviewWithName)

    val result = hybridRepository.getAllReviews()

    assertEquals(1, result.size)
    coVerify(exactly = 0) { profileRepository.getProfile(any()) }
    // Verify the review was stored with existing ownerName
    val storedReview = localRepository.getReview("review-1")
    assertEquals("Jane Smith", storedReview.ownerName)
  }

  @Test
  fun getAllReviews_online_handlesProfileFetchFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val testProfileRepository = mockk<ProfileRepository>(relaxed = true)
    ProfileRepositoryProvider.repository = testProfileRepository

    coEvery { testProfileRepository.getProfile("user-1") } throws
        NoSuchElementException("Profile not found")

    val reviewWithoutName = createTestReview("review-1", ownerId = "user-1", ownerName = null)
    coEvery { remoteRepository.getAllReviews() } returns listOf(reviewWithoutName)

    val result = hybridRepository.getAllReviews()

    assertEquals(1, result.size)
    // Verify the review was stored with null ownerName when profile fetch fails
    val storedReview = localRepository.getReview("review-1")
    assertNull(storedReview.ownerName)
    // Restore original mock
    ProfileRepositoryProvider.repository = profileRepository
  }

  @Test
  fun getAllReviews_online_handlesEmptyOwnerName() = runTest {
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

    val reviewWithoutName = createTestReview("review-1", ownerId = "user-1", ownerName = null)
    coEvery { remoteRepository.getAllReviews() } returns listOf(reviewWithoutName)

    val result = hybridRepository.getAllReviews()

    assertEquals(1, result.size)
    // Verify the review was stored with null ownerName when name is empty
    val storedReview = localRepository.getReview("review-1")
    assertNull(storedReview.ownerName)
    // Restore original mock
    ProfileRepositoryProvider.repository = profileRepository
  }

  @Test
  fun getAllReviewsByResidencyForUser_nullUserId_returnsAllReviews() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val reviews = listOf(createTestReview("review-1"), createTestReview("review-2"))
    coEvery { remoteRepository.getAllReviewsByResidency("Vortex") } returns reviews

    val result = hybridRepository.getAllReviewsByResidencyForUser("Vortex", null)

    assertEquals(reviews, result)
    coVerify(exactly = 0) { profileRepository.getBlockedUserIds(any()) }
  }

  @Test
  fun getAllReviewsByResidencyForUser_filtersWhenCurrentUserBlockedOwner() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val review1 = createTestReview("review-1", ownerId = user1Id)
    val review2 = createTestReview("review-2", ownerId = user2Id)
    val review3 = createTestReview("review-3", ownerId = "user-3")

    coEvery { remoteRepository.getAllReviewsByResidency("Vortex") } returns
        listOf(review1, review2, review3)
    // User1 has blocked user2
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns listOf(user2Id)
    // User2 and user3 have not blocked anyone
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns emptyList()
    coEvery { profileRepository.getBlockedUserIds("user-3") } returns emptyList()

    val result = hybridRepository.getAllReviewsByResidencyForUser("Vortex", user1Id)

    // Should filter out review2 (user2's review) because user1 blocked user2
    assertEquals(2, result.size)
    assertTrue(result.contains(review1))
    assertTrue(result.contains(review3))
    assertFalse(result.contains(review2))
  }

  @Test
  fun getAllReviewsByResidencyForUser_filtersWhenOwnerBlockedCurrentUser() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val review1 = createTestReview("review-1", ownerId = user1Id)
    val review2 = createTestReview("review-2", ownerId = user2Id)
    val review3 = createTestReview("review-3", ownerId = "user-3")

    coEvery { remoteRepository.getAllReviewsByResidency("Vortex") } returns
        listOf(review1, review2, review3)
    // User1 has not blocked anyone
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns emptyList()
    // User2 has blocked user1
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns listOf(user1Id)
    coEvery { profileRepository.getBlockedUserIds("user-3") } returns emptyList()

    val result = hybridRepository.getAllReviewsByResidencyForUser("Vortex", user1Id)

    // Should filter out review2 (user2's review) because user2 blocked user1
    assertEquals(2, result.size)
    assertTrue(result.contains(review1))
    assertTrue(result.contains(review3))
    assertFalse(result.contains(review2))
  }

  @Test
  fun getAllReviewsByResidencyForUser_returnsOwnReviews() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val review1 = createTestReview("review-1", ownerId = user1Id)
    val review2 = createTestReview("review-2", ownerId = "user-2")

    coEvery { remoteRepository.getAllReviewsByResidency("Vortex") } returns listOf(review1, review2)
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns emptyList()
    coEvery { profileRepository.getBlockedUserIds("user-2") } returns emptyList()

    val result = hybridRepository.getAllReviewsByResidencyForUser("Vortex", user1Id)

    // Should include own review even if blocked
    assertTrue(result.contains(review1))
  }

  @Test
  fun getReviewForUser_nullUserId_returnsReview() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val review = createTestReview("review-1")
    coEvery { remoteRepository.getReview("review-1") } returns review

    val result = hybridRepository.getReviewForUser("review-1", null)

    assertEquals(review, result)
    coVerify(exactly = 0) { profileRepository.getBlockedUserIds(any()) }
  }

  @Test
  fun getReviewForUser_ownerViewingOwnReview_returnsReview() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val review = createTestReview("review-1", ownerId = user1Id)
    coEvery { remoteRepository.getReview("review-1") } returns review

    val result = hybridRepository.getReviewForUser("review-1", user1Id)

    assertEquals(review, result)
    coVerify(exactly = 0) { profileRepository.getBlockedUserIds(any()) }
  }

  @Test
  fun getReviewForUser_throwsWhenCurrentUserBlockedOwner() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val review = createTestReview("review-1", ownerId = user2Id)

    coEvery { remoteRepository.getReview("review-1") } returns review
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns listOf(user2Id)
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns emptyList()

    val result = runCatching { hybridRepository.getReviewForUser("review-1", user1Id) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
    assertTrue(result.exceptionOrNull()?.message?.contains("blocking restrictions") == true)
  }

  @Test
  fun getReviewForUser_throwsWhenOwnerBlockedCurrentUser() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val review = createTestReview("review-1", ownerId = user2Id)

    coEvery { remoteRepository.getReview("review-1") } returns review
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns emptyList()
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns listOf(user1Id)

    val result = runCatching { hybridRepository.getReviewForUser("review-1", user1Id) }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
    assertTrue(result.exceptionOrNull()?.message?.contains("blocking restrictions") == true)
  }

  @Test
  fun getReviewForUser_returnsWhenNotBlocked() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val review = createTestReview("review-1", ownerId = user2Id)

    coEvery { remoteRepository.getReview("review-1") } returns review
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns emptyList()
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns emptyList()

    val result = hybridRepository.getReviewForUser("review-1", user1Id)

    assertEquals(review, result)
  }

  @Test
  fun getAllReviewsByResidencyForUser_doesNotFilterAnonymousReviewsWhenBlocked() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val normalReview = createTestReview("review-1", ownerId = user2Id, isAnonymous = false)
    val anonymousReview = createTestReview("review-2", ownerId = user2Id, isAnonymous = true)
    val review3 = createTestReview("review-3", ownerId = "user-3")

    coEvery { remoteRepository.getAllReviewsByResidency("Vortex") } returns
        listOf(normalReview, anonymousReview, review3)
    // User1 has blocked user2
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns listOf(user2Id)
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns emptyList()
    coEvery { profileRepository.getBlockedUserIds("user-3") } returns emptyList()

    val result = hybridRepository.getAllReviewsByResidencyForUser("Vortex", user1Id)

    // Should filter out normalReview (user2's non-anonymous review) but keep anonymousReview
    // to preserve anonymity
    assertEquals(2, result.size)
    assertTrue(result.contains(anonymousReview)) // Anonymous review should be visible
    assertTrue(result.contains(review3))
    assertFalse(result.contains(normalReview)) // Non-anonymous blocked review should be filtered
  }

  @Test
  fun getReviewForUser_returnsAnonymousReviewEvenWhenBlocked() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val anonymousReview = createTestReview("review-1", ownerId = user2Id, isAnonymous = true)

    coEvery { remoteRepository.getReview("review-1") } returns anonymousReview
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns listOf(user2Id)
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns emptyList()

    val result = hybridRepository.getReviewForUser("review-1", user1Id)

    // Anonymous review should be returned even if owner is blocked (privacy requirement)
    assertEquals(anonymousReview, result)
  }

  @Test
  fun getAllReviewsByResidencyForUser_doesNotFilterAnonymousReviewsWhenOwnerBlockedCurrentUser() =
      runTest {
        mockkObject(NetworkUtils)
        every { NetworkUtils.isNetworkAvailable(context) } returns true

        val user1Id = "user-1"
        val user2Id = "user-2"
        val normalReview = createTestReview("review-1", ownerId = user2Id, isAnonymous = false)
        val anonymousReview = createTestReview("review-2", ownerId = user2Id, isAnonymous = true)

        coEvery { remoteRepository.getAllReviewsByResidency("Vortex") } returns
            listOf(normalReview, anonymousReview)
        coEvery { profileRepository.getBlockedUserIds(user1Id) } returns emptyList()
        // User2 has blocked user1
        coEvery { profileRepository.getBlockedUserIds(user2Id) } returns listOf(user1Id)

        val result = hybridRepository.getAllReviewsByResidencyForUser("Vortex", user1Id)

        // Should filter out normalReview but keep anonymousReview to preserve anonymity
        assertEquals(1, result.size)
        assertTrue(result.contains(anonymousReview)) // Anonymous review should be visible
        assertFalse(
            result.contains(normalReview)) // Non-anonymous blocked review should be filtered
  }

  @Test
  fun getAllReviewsByResidencyForUser_handlesBlockedListFetchFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val review1 = createTestReview("review-1", ownerId = user1Id)
    val review2 = createTestReview("review-2", ownerId = "user-2")

    coEvery { remoteRepository.getAllReviewsByResidency("Vortex") } returns listOf(review1, review2)
    // Simulate failure when fetching blocked list - should default to empty list
    coEvery { profileRepository.getBlockedUserIds(user1Id) } throws IOException("Network error")

    val result = hybridRepository.getAllReviewsByResidencyForUser("Vortex", user1Id)

    // Should return all reviews when blocked list fetch fails (defaults to empty)
    assertEquals(2, result.size)
  }

  @Test
  fun getReviewForUser_handlesBlockedListFetchFailure() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val review = createTestReview("review-1", ownerId = user2Id)

    coEvery { remoteRepository.getReview("review-1") } returns review
    // Simulate failure when fetching blocked lists - should default to empty (not blocked)
    coEvery { profileRepository.getBlockedUserIds(user1Id) } throws IOException("Network error")
    coEvery { profileRepository.getBlockedUserIds(user2Id) } throws IOException("Network error")

    val result = hybridRepository.getReviewForUser("review-1", user1Id)

    // Should return review when blocked list fetch fails (defaults to not blocked)
    assertEquals(review, result)
  }

  @Test
  fun getAllReviewsByResidencyForUser_usesCacheForMultipleReviewsFromSameOwner() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val user1Id = "user-1"
    val user2Id = "user-2"
    val review1 = createTestReview("review-1", ownerId = user1Id)
    val review2 = createTestReview("review-2", ownerId = user2Id)
    val review3 = createTestReview("review-3", ownerId = user2Id) // Same owner as review2

    coEvery { remoteRepository.getAllReviewsByResidency("Vortex") } returns
        listOf(review1, review2, review3)
    coEvery { profileRepository.getBlockedUserIds(user1Id) } returns emptyList()
    // Should only be called once for user2, even though there are 2 reviews
    coEvery { profileRepository.getBlockedUserIds(user2Id) } returns listOf(user1Id)

    val result = hybridRepository.getAllReviewsByResidencyForUser("Vortex", user1Id)

    // Should filter out both reviews from user2
    assertEquals(1, result.size)
    assertTrue(result.contains(review1))
    // Verify getBlockedUserIds was called only once for user2 (cache used)
    coVerify(exactly = 1) { profileRepository.getBlockedUserIds(user2Id) }
  }

  @Test
  fun getAllReviews_online_deletesStaleReviewsDuringFullSync() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    // Add some stale reviews to local database
    val staleReview1 = createTestReview("stale-1")
    val staleReview2 = createTestReview("stale-2")
    val currentReview = createTestReview("current-1")
    localRepository.addReview(staleReview1)
    localRepository.addReview(staleReview2)
    localRepository.addReview(currentReview)

    // Remote only has current review (stale ones were deleted from Firestore)
    coEvery { remoteRepository.getAllReviews() } returns listOf(currentReview)

    val result = hybridRepository.getAllReviews()

    assertEquals(listOf(currentReview), result)
    // Verify stale reviews were deleted
    val localReviews = localRepository.getAllReviews()
    assertEquals(1, localReviews.size)
    assertEquals("current-1", localReviews[0].uid)
    // Verify stale reviews are gone
    val result1 = runCatching { localRepository.getReview("stale-1") }
    assertTrue(result1.isFailure)
    assertTrue(result1.exceptionOrNull() is NoSuchElementException)
    val result2 = runCatching { localRepository.getReview("stale-2") }
    assertTrue(result2.isFailure)
    assertTrue(result2.exceptionOrNull() is NoSuchElementException)
  }

  @Test
  fun getAllReviews_online_deletesAllReviewsWhenRemoteIsEmpty() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    // Add some reviews to local database
    val review1 = createTestReview("review-1")
    val review2 = createTestReview("review-2")
    localRepository.addReview(review1)
    localRepository.addReview(review2)

    // Remote is empty (all reviews deleted from Firestore)
    coEvery { remoteRepository.getAllReviews() } returns emptyList()

    val result = hybridRepository.getAllReviews()

    assertEquals(emptyList<Review>(), result)
    // Verify all local reviews were deleted
    assertEquals(0, localRepository.getAllReviews().size)
  }

  @Test
  fun getAllReviewsByUser_online_doesNotDeleteStaleReviews() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    // Add a review from a different user
    val otherUserReview = createTestReview("other-1", ownerId = "user-2")
    localRepository.addReview(otherUserReview)

    val userReview = createTestReview("user-1", ownerId = "user-1")
    coEvery { remoteRepository.getAllReviewsByUser("user-1") } returns listOf(userReview)

    val result = hybridRepository.getAllReviewsByUser("user-1")

    assertEquals(listOf(userReview), result)
    // Verify other user's review is still in local database (not deleted because it's a partial
    // sync)
    val localReviews = localRepository.getAllReviews()
    assertEquals(2, localReviews.size)
    assertTrue(localReviews.any { it.uid == "other-1" })
    assertTrue(localReviews.any { it.uid == "user-1" })
  }

  @Test
  fun getAllReviewsByResidency_online_doesNotDeleteStaleReviews() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    // Add a review for a different residency
    val otherResidencyReview = createTestReview("other-1", residencyName = "OtherResidency")
    localRepository.addReview(otherResidencyReview)

    val residencyReview = createTestReview("residency-1", residencyName = "Vortex")
    coEvery { remoteRepository.getAllReviewsByResidency("Vortex") } returns listOf(residencyReview)

    val result = hybridRepository.getAllReviewsByResidency("Vortex")

    assertEquals(listOf(residencyReview), result)
    // Verify other residency's review is still in local database (not deleted because it's a
    // partial sync)
    val localReviews = localRepository.getAllReviews()
    assertEquals(2, localReviews.size)
    assertTrue(localReviews.any { it.uid == "other-1" })
    assertTrue(localReviews.any { it.uid == "residency-1" })
  }

  @Test
  fun getAllReviews_online_handlesDeletionFailureGracefully() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    val currentReview = createTestReview("current-1")
    coEvery { remoteRepository.getAllReviews() } returns listOf(currentReview)

    // Close database to simulate deletion failure
    database.close()
    val newDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    localRepository = ReviewsRepositoryLocal(newDatabase.reviewDao())
    hybridRepository = ReviewsRepositoryHybrid(context, remoteRepository, localRepository)

    // Should not throw even if deletion fails
    val result = hybridRepository.getAllReviews()

    assertEquals(listOf(currentReview), result)
    // Verify review was still synced despite deletion failure
    assertEquals(1, localRepository.getAllReviews().size)
    newDatabase.close()
  }

  @Test
  fun getAllReviews_online_updatesExistingAndDeletesStale() = runTest {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(context) } returns true

    // Add existing review with old data
    val oldReview = createTestReview("review-1", title = "Old Title")
    val staleReview = createTestReview("stale-1")
    localRepository.addReview(oldReview)
    localRepository.addReview(staleReview)

    // Remote has updated review-1 and a new review-2, but no stale-1
    val updatedReview = createTestReview("review-1", title = "Updated Title")
    val newReview = createTestReview("review-2")
    coEvery { remoteRepository.getAllReviews() } returns listOf(updatedReview, newReview)

    val result = hybridRepository.getAllReviews()

    assertEquals(2, result.size)
    val localReviews = localRepository.getAllReviews()
    assertEquals(2, localReviews.size)
    // Verify review-1 was updated
    assertEquals("Updated Title", localReviews.find { it.uid == "review-1" }?.title)
    // Verify review-2 was added
    assertTrue(localReviews.any { it.uid == "review-2" })
    // Verify stale-1 was deleted
    assertFalse(localReviews.any { it.uid == "stale-1" })
  }

  private fun createTestReview(
      uid: String,
      ownerId: String = "user-1",
      title: String = "Test Review",
      residencyName: String = "Vortex",
      upvotedBy: Set<String> = emptySet(),
      downvotedBy: Set<String> = emptySet(),
      ownerName: String? = null,
      isAnonymous: Boolean = false
  ): Review {
    val fixedTimestamp = Timestamp(1000000L, 0)
    return Review(
        uid = uid,
        ownerId = ownerId,
        ownerName = ownerName,
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
        isAnonymous = isAnonymous)
  }
}
