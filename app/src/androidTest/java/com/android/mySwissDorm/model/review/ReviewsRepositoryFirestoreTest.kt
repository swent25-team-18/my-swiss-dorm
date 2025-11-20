package com.android.mySwissDorm.model.review

import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ReviewsRepositoryFirestoreTest : FirestoreTest() {
  override fun createRepositories() {
    ReviewsRepositoryProvider.repository = ReviewsRepositoryFirestore(FirebaseEmulator.firestore)
  }

  private val repo = ReviewsRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun getNewUidReturnsUniqueIDs() = runTest {
    val numberIDs = 100
    val uids = (0 until numberIDs).toSet<Int>().map { repo.getNewUid() }.toSet()
    TestCase.assertEquals(uids.size, numberIDs)
  }

  @Test
  fun getFailsWithNotAddedUid() = runTest {
    switchToUser(FakeUser.FakeUser1)
    assertEquals(true, runCatching { repo.getReview("abc") }.isFailure)
  }

  @Test
  fun canAddAndGetReviewFromRepository() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val reviewToAdd =
        reviewVortex1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.addReview(reviewToAdd)
    assertEquals(reviewToAdd, repo.getReview(reviewToAdd.uid))
  }

  @Test
  fun getAllReviewsWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val firstReview =
        reviewVortex1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    val secondReview =
        reviewVortex2.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    val allReviews = listOf(firstReview, secondReview)
    allReviews.forEach { repo.addReview(it) }
    assertEquals(allReviews.toSet(), repo.getAllReviews().toSet())
  }

  @Test
  fun getAllReviewsByUserWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val firstUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val firstReview = reviewVortex1.copy(ownerId = firstUserId)
    val secondReview = reviewWoko1.copy(ownerId = firstUserId)
    repo.addReview(firstReview)
    repo.addReview(secondReview)

    switchToUser(FakeUser.FakeUser2)
    val secondUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val thirdReview = reviewVortex2.copy(ownerId = secondUserId)
    repo.addReview(thirdReview)

    switchToUser(FakeUser.FakeUser1)
    assertEquals(
        listOf(firstReview, secondReview).toSet(), repo.getAllReviewsByUser(firstUserId).toSet())
  }

  @Test
  fun getAllReviewsByResidencyWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val firstUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val firstReview = reviewVortex1.copy(ownerId = firstUserId)
    val secondReview = reviewWoko1.copy(ownerId = firstUserId)
    repo.addReview(firstReview)
    repo.addReview(secondReview)

    switchToUser(FakeUser.FakeUser2)
    val secondUserId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val thirdReview = reviewVortex2.copy(ownerId = secondUserId)
    repo.addReview(thirdReview)

    switchToUser(FakeUser.FakeUser1)
    assertEquals(
        listOf(firstReview, thirdReview).toSet(), repo.getAllReviewsByResidency("Vortex").toSet())
  }

  @Test
  fun editReviewWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review =
        reviewVortex1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    val editedReview =
        review.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            title = "Modified Title",
            grade = 3.0)
    repo.addReview(review)
    assertEquals(true, runCatching { repo.editReview(review.uid, editedReview) }.isSuccess)
    assertEquals(editedReview, repo.getReview(review.uid))
  }

  @Test
  fun editReviewButDifferentUidFails() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val firstReview =
        reviewVortex1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    val secondReview =
        reviewVortex2.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.addReview(firstReview)
    assertEquals(true, runCatching { repo.editReview(firstReview.uid, secondReview) }.isFailure)
  }

  @Test
  fun deleteReviewWorks() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val review =
        reviewVortex1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"))
    repo.addReview(review)
    assertEquals(true, runCatching { repo.getReview(review.uid) }.isSuccess)
    repo.deleteReview(review.uid)
    assertEquals(true, runCatching { repo.getReview(review.uid) }.isFailure)
  }

  @Test
  fun voteListsArePersistedAndLoaded() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    val original =
        reviewVortex1.copy(
            ownerId = ownerId,
            upvotedBy = setOf(ownerId, "other-user"),
            downvotedBy = setOf("downvoter-1"))

    repo.addReview(original)
    val loaded = repo.getReview(original.uid)

    assertEquals(original.upvotedBy, loaded.upvotedBy)
    assertEquals(original.downvotedBy, loaded.downvotedBy)
  }

  @Test
  fun reviewWithoutUpvotedByReturnsNull() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    // Write a document without upvotedBy field (old schema simulation)
    val id = repo.getNewUid()
    val data =
        mapOf(
            "ownerId" to ownerId,
            "postedAt" to Timestamp.now(),
            "title" to "Title",
            "reviewText" to "Text",
            "grade" to 4.0,
            "residencyName" to "Vortex",
            "roomType" to RoomType.STUDIO.name,
            "pricePerMonth" to 1000.0,
            "areaInM2" to 20.0,
            "imageUrls" to emptyList<String>(),
            "downvotedBy" to emptyList<String>())
    // Note: upvotedBy is intentionally missing

    FirebaseEmulator.firestore.collection(REVIEWS_COLLECTION_PATH).document(id).set(data).await()

    // Should return null (and getReview throws when null)
    assertEquals(true, runCatching { repo.getReview(id) }.isFailure)
  }

  @Test
  fun reviewWithoutDownvotedByReturnsNull() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    // Write a document without downvotedBy field (old schema simulation)
    val id = repo.getNewUid()
    val data =
        mapOf(
            "ownerId" to ownerId,
            "postedAt" to Timestamp.now(),
            "title" to "Title",
            "reviewText" to "Text",
            "grade" to 4.0,
            "residencyName" to "Vortex",
            "roomType" to RoomType.STUDIO.name,
            "pricePerMonth" to 1000.0,
            "areaInM2" to 20.0,
            "imageUrls" to emptyList<String>(),
            "upvotedBy" to emptyList<String>())
    // Note: downvotedBy is intentionally missing

    FirebaseEmulator.firestore.collection(REVIEWS_COLLECTION_PATH).document(id).set(data).await()

    // Should return null (and getReview throws when null)
    assertEquals(true, runCatching { repo.getReview(id) }.isFailure)
  }

  @Test
  fun upvoteReviewAddsUpvote() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val review = reviewVortex1.copy(ownerId = ownerId)
    repo.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val voterId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    repo.upvoteReview(review.uid, voterId)
    val loaded = repo.getReview(review.uid)

    assertEquals(true, voterId in loaded.upvotedBy)
    assertEquals(false, voterId in loaded.downvotedBy)
    assertEquals(1, loaded.upvotedBy.size)
    assertEquals(0, loaded.downvotedBy.size)
  }

  @Test
  fun upvoteReviewTogglesOffIfAlreadyUpvoted() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val voterId = "voter-id"
    val review = reviewVortex1.copy(ownerId = ownerId, upvotedBy = setOf(voterId))
    repo.addReview(review)

    repo.upvoteReview(review.uid, voterId)
    val loaded = repo.getReview(review.uid)

    assertEquals(false, voterId in loaded.upvotedBy)
    assertEquals(0, loaded.upvotedBy.size)
  }

  @Test
  fun upvoteReviewSwitchesFromDownvoteToUpvote() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val voterId = "voter-id"
    val review = reviewVortex1.copy(ownerId = ownerId, downvotedBy = setOf(voterId))
    repo.addReview(review)

    repo.upvoteReview(review.uid, voterId)
    val loaded = repo.getReview(review.uid)

    assertEquals(true, voterId in loaded.upvotedBy)
    assertEquals(false, voterId in loaded.downvotedBy)
    assertEquals(1, loaded.upvotedBy.size)
    assertEquals(0, loaded.downvotedBy.size)
  }

  @Test
  fun downvoteReviewAddsDownvote() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val review = reviewVortex1.copy(ownerId = ownerId)
    repo.addReview(review)

    switchToUser(FakeUser.FakeUser2)
    val voterId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    repo.downvoteReview(review.uid, voterId)
    val loaded = repo.getReview(review.uid)

    assertEquals(false, voterId in loaded.upvotedBy)
    assertEquals(true, voterId in loaded.downvotedBy)
    assertEquals(0, loaded.upvotedBy.size)
    assertEquals(1, loaded.downvotedBy.size)
  }

  @Test
  fun downvoteReviewTogglesOffIfAlreadyDownvoted() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val voterId = "voter-id"
    val review = reviewVortex1.copy(ownerId = ownerId, downvotedBy = setOf(voterId))
    repo.addReview(review)

    repo.downvoteReview(review.uid, voterId)
    val loaded = repo.getReview(review.uid)

    assertEquals(false, voterId in loaded.downvotedBy)
    assertEquals(0, loaded.downvotedBy.size)
  }

  @Test
  fun downvoteReviewSwitchesFromUpvoteToDownvote() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val voterId = "voter-id"
    val review = reviewVortex1.copy(ownerId = ownerId, upvotedBy = setOf(voterId))
    repo.addReview(review)

    repo.downvoteReview(review.uid, voterId)
    val loaded = repo.getReview(review.uid)

    assertEquals(false, voterId in loaded.upvotedBy)
    assertEquals(true, voterId in loaded.downvotedBy)
    assertEquals(0, loaded.upvotedBy.size)
    assertEquals(1, loaded.downvotedBy.size)
  }

  @Test
  fun removeVoteRemovesUpvote() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val voterId = "voter-id"
    val review = reviewVortex1.copy(ownerId = ownerId, upvotedBy = setOf(voterId))
    repo.addReview(review)

    repo.removeVote(review.uid, voterId)
    val loaded = repo.getReview(review.uid)

    assertEquals(false, voterId in loaded.upvotedBy)
    assertEquals(false, voterId in loaded.downvotedBy)
  }

  @Test
  fun removeVoteRemovesDownvote() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val voterId = "voter-id"
    val review = reviewVortex1.copy(ownerId = ownerId, downvotedBy = setOf(voterId))
    repo.addReview(review)

    repo.removeVote(review.uid, voterId)
    val loaded = repo.getReview(review.uid)

    assertEquals(false, voterId in loaded.upvotedBy)
    assertEquals(false, voterId in loaded.downvotedBy)
  }

  @Test
  fun upvoteReviewPreventsSelfVoting() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val review = reviewVortex1.copy(ownerId = ownerId)
    repo.addReview(review)

    assertEquals(true, runCatching { repo.upvoteReview(review.uid, ownerId) }.isFailure)
  }

  @Test
  fun downvoteReviewPreventsSelfVoting() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val review = reviewVortex1.copy(ownerId = ownerId)
    repo.addReview(review)

    assertEquals(true, runCatching { repo.downvoteReview(review.uid, ownerId) }.isFailure)
  }

  @Test
  fun removeVotePreventsSelfVoting() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val ownerId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val review = reviewVortex1.copy(ownerId = ownerId)
    repo.addReview(review)

    assertEquals(true, runCatching { repo.removeVote(review.uid, ownerId) }.isFailure)
  }

  @Test
  fun upvoteReviewFailsForNonExistentReview() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val voterId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    assertEquals(true, runCatching { repo.upvoteReview("non-existent-id", voterId) }.isFailure)
  }

  @Test
  fun downvoteReviewFailsForNonExistentReview() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val voterId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    assertEquals(true, runCatching { repo.downvoteReview("non-existent-id", voterId) }.isFailure)
  }

  @Test
  fun removeVoteFailsForNonExistentReview() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val voterId =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")

    assertEquals(true, runCatching { repo.removeVote("non-existent-id", voterId) }.isFailure)
  }
}
