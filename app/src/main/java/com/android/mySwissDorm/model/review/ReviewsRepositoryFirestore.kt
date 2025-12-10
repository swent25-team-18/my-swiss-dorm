package com.android.mySwissDorm.model.review

import android.util.Log
import com.android.mySwissDorm.model.rental.RoomType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val REVIEWS_COLLECTION_PATH = "reviews"

/**
 * Firestore-backed implementation of [ReviewsRepository].
 *
 * This repository is responsible for persisting and retrieving [Review] documents from the
 * `reviews` collection. It also performs mapping between Firestore documents and the domain model,
 * handling backward compatibility for newly added fields (for example vote-related lists).
 */
class ReviewsRepositoryFirestore(private val db: FirebaseFirestore) : ReviewsRepository {

  private val ownerAttributeName = "ownerId"
  private val residencyAttributeName = "residencyName"

  /** Returns a new unique identifier that can be used as a review document id. */
  override fun getNewUid(): String {
    return db.collection(REVIEWS_COLLECTION_PATH).document().id
  }

  /** Fetches all reviews in the collection. */
  override suspend fun getAllReviews(): List<Review> {
    val snapshot = db.collection(REVIEWS_COLLECTION_PATH).get().await()
    return snapshot.mapNotNull { documentToReview(it) }
  }

  /**
   * Fetches all reviews created by the user with the given [userId].
   *
   * This uses a Firestore query on the `ownerId` field.
   */
  override suspend fun getAllReviewsByUser(userId: String): List<Review> {
    val snapshot =
        db.collection(REVIEWS_COLLECTION_PATH)
            .whereEqualTo(ownerAttributeName, userId)
            .get()
            .await()
    return snapshot.mapNotNull { documentToReview(it) }
  }

  /**
   * Fetches all reviews associated with the given [residencyName].
   *
   * This uses a Firestore query on the `residencyName` field.
   */
  override suspend fun getAllReviewsByResidency(residencyName: String): List<Review> {
    val snapshot =
        db.collection(REVIEWS_COLLECTION_PATH)
            .whereEqualTo(residencyAttributeName, residencyName)
            .get()
            .await()
    return snapshot.mapNotNull { documentToReview(it) }
  }

  /**
   * Fetches a single review by its [reviewId].
   *
   * @throws NoSuchElementException if the document does not exist or cannot be converted to
   *   [Review].
   */
  override suspend fun getReview(reviewId: String): Review {
    val document = db.collection(REVIEWS_COLLECTION_PATH).document(reviewId).get().await()
    return documentToReview(document)
        ?: throw NoSuchElementException("ReviewsRepositoryFirestore: Review $reviewId not found")
  }

  /** Inserts a new [review] document or overwrites an existing document with the same id. */
  override suspend fun addReview(review: Review) {
    // Convert Sets to Lists for Firestore storage and ensure isAnonymous is explicitly saved
    // (Firestore may skip default values, but isAnonymous must always be present for privacy)
    val data = reviewToFirestoreMap(review)
    db.collection(REVIEWS_COLLECTION_PATH).document(review.uid).set(data).await()
    // Explicitly update isAnonymous to ensure it's always present (even if false)
    db.collection(REVIEWS_COLLECTION_PATH)
        .document(review.uid)
        .update("isAnonymous", review.isAnonymous)
        .await()
  }

  /**
   * Replaces an existing review identified by [reviewId] with [newValue].
   *
   * @throws Exception if [newValue.uid] does not match [reviewId].
   */
  override suspend fun editReview(reviewId: String, newValue: Review) {
    if (newValue.uid != reviewId) {
      throw Exception("ReviewsRepositoryFirestore: Provided reviewId does not match newValue.uid")
    }
    // Convert Sets to Lists for Firestore storage and ensure isAnonymous is explicitly saved
    // (Firestore may skip default values, but isAnonymous must always be present for privacy)
    val data = reviewToFirestoreMap(newValue)
    db.collection(REVIEWS_COLLECTION_PATH).document(reviewId).set(data).await()
    // Explicitly update isAnonymous to ensure it's always present (even if false)
    db.collection(REVIEWS_COLLECTION_PATH)
        .document(reviewId)
        .update("isAnonymous", newValue.isAnonymous)
        .await()
  }

  /**
   * Converts a [Review] domain model to a Map suitable for Firestore storage.
   *
   * Converts Sets to Lists since Firestore only supports Lists.
   */
  private fun reviewToFirestoreMap(review: Review): Map<String, Any?> {
    return mapOf(
        "ownerId" to review.ownerId,
        "ownerName" to (review.ownerName ?: ""),
        "postedAt" to review.postedAt,
        "title" to review.title,
        "reviewText" to review.reviewText,
        "grade" to review.grade,
        "residencyName" to review.residencyName,
        "roomType" to review.roomType.name,
        "pricePerMonth" to review.pricePerMonth,
        "areaInM2" to review.areaInM2,
        "imageUrls" to review.imageUrls,
        "upvotedBy" to review.upvotedBy.toList(),
        "downvotedBy" to review.downvotedBy.toList(),
        "isAnonymous" to review.isAnonymous)
  }

  /** Deletes the review document with the given [reviewId]. */
  override suspend fun deleteReview(reviewId: String) {
    db.collection(REVIEWS_COLLECTION_PATH).document(reviewId).delete().await()
  }

  /**
   * Applies an upvote to the review by the given user.
   *
   * Uses a Firestore transaction to ensure atomicity. If the user has already upvoted, the upvote
   * is removed. If the user has downvoted, the downvote is removed and replaced with an upvote.
   *
   * @throws Exception if the review is not found or if the user is the owner of the review.
   */
  override suspend fun upvoteReview(reviewId: String, userId: String) {
    updateVoteLists(reviewId, userId) { newUpvotedBy, newDownvotedBy ->
      when {
        userId in newUpvotedBy -> {
          // Already upvoted: remove upvote
          newUpvotedBy.remove(userId)
        }
        userId in newDownvotedBy -> {
          // Downvoted: remove downvote and add upvote
          newDownvotedBy.remove(userId)
          newUpvotedBy.add(userId)
        }
        else -> {
          // No vote: add upvote
          newUpvotedBy.add(userId)
        }
      }
    }
  }

  /**
   * Applies a downvote to the review by the given user.
   *
   * Uses a Firestore transaction to ensure atomicity. If the user has already downvoted, the
   * downvote is removed. If the user has upvoted, the upvote is removed and replaced with a
   * downvote.
   *
   * @throws Exception if the review is not found or if the user is the owner of the review.
   */
  override suspend fun downvoteReview(reviewId: String, userId: String) {
    updateVoteLists(reviewId, userId) { newUpvotedBy, newDownvotedBy ->
      when {
        userId in newDownvotedBy -> {
          // Already downvoted: remove downvote
          newDownvotedBy.remove(userId)
        }
        userId in newUpvotedBy -> {
          // Upvoted: remove upvote and add downvote
          newUpvotedBy.remove(userId)
          newDownvotedBy.add(userId)
        }
        else -> {
          // No vote: add downvote
          newDownvotedBy.add(userId)
        }
      }
    }
  }

  /**
   * Removes any existing vote (upvote or downvote) from the review by the given user.
   *
   * Uses a Firestore transaction to ensure atomicity.
   *
   * @throws Exception if the review is not found or if the user is the owner of the review.
   */
  override suspend fun removeVote(reviewId: String, userId: String) {
    updateVoteLists(reviewId, userId) { newUpvotedBy, newDownvotedBy ->
      newUpvotedBy.remove(userId)
      newDownvotedBy.remove(userId)
    }
  }

  /**
   * Helper function that performs the common transaction logic for vote operations.
   *
   * Validates the review exists and the user is not the owner, then applies the vote modification
   * logic provided by [voteModifier].
   *
   * @param reviewId The unique identifier of the review.
   * @param userId The unique identifier of the user casting the vote.
   * @param voteModifier A function that modifies the upvotedBy and downvotedBy sets based on the
   *   vote operation.
   * @throws Exception if the review is not found or if the user is the owner of the review.
   */
  private suspend fun updateVoteLists(
      reviewId: String,
      userId: String,
      voteModifier: (MutableSet<String>, MutableSet<String>) -> Unit
  ) {
    db.runTransaction { transaction ->
          val docRef = db.collection(REVIEWS_COLLECTION_PATH).document(reviewId)
          val snapshot = transaction.get(docRef)

          if (!snapshot.exists()) {
            throw Exception("ReviewsRepositoryFirestore: Review $reviewId not found")
          }

          val ownerId = snapshot.getString("ownerId")
          if (ownerId == userId) {
            throw Exception("ReviewsRepositoryFirestore: Users cannot vote on their own reviews")
          }

          // Read from Firestore as List (Firestore stores arrays as lists)
          @Suppress("UNCHECKED_CAST")
          val upvotedByList =
              (snapshot.get("upvotedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
          @Suppress("UNCHECKED_CAST")
          val downvotedByList =
              (snapshot.get("downvotedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

          // Convert to Set to deduplicate and work with domain model
          val newUpvotedBy = upvotedByList.toMutableSet()
          val newDownvotedBy = downvotedByList.toMutableSet()

          voteModifier(newUpvotedBy, newDownvotedBy)

          // Convert back to List for Firestore storage
          transaction.update(docRef, "upvotedBy", newUpvotedBy.toList())
          transaction.update(docRef, "downvotedBy", newDownvotedBy.toList())
        }
        .await()
  }

  // Converts a Firestore DocumentSnapshot into a Review instance.
  private fun documentToReview(document: DocumentSnapshot): Review? {
    return try {
      val uid = document.id
      val ownerId = document.getString("ownerId") ?: return null
      val ownerName = document.getString("ownerName")?.takeIf { it.isNotEmpty() }
      val postedAt = document.getTimestamp("postedAt") ?: return null
      val title = document.getString("title") ?: return null
      val reviewText = document.getString("reviewText") ?: return null
      val grade = document.getDouble("grade") ?: return null
      val residencyName = document.getString("residencyName") ?: return null
      val roomTypeString = document.getString("roomType") ?: return null
      val roomType =
          RoomType.entries.firstOrNull { it.name == roomTypeString }
              ?: return null // will take the value if its in the num otherwise null for safety
      val pricePerMonth = document.getDouble("pricePerMonth") ?: return null
      val areaInM2 = (document.getDouble("areaInM2") ?: return null).toInt()
      val imageUrls =
          (document.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: return null

      @Suppress("UNCHECKED_CAST")
      val upvotedByList =
          (document.get("upvotedBy") as? List<*>)?.mapNotNull { it as? String } ?: return null
      @Suppress("UNCHECKED_CAST")
      val downvotedByList =
          (document.get("downvotedBy") as? List<*>)?.mapNotNull { it as? String } ?: return null
      val isAnonymous = document.getBoolean("isAnonymous") ?: return null

      val upvotedBy = upvotedByList.toSet()
      val downvotedBy = downvotedByList.toSet()

      return Review(
          uid = uid,
          ownerId = ownerId,
          ownerName = ownerName,
          postedAt = postedAt,
          title = title,
          reviewText = reviewText,
          grade = grade,
          residencyName = residencyName,
          roomType = roomType,
          pricePerMonth = pricePerMonth,
          areaInM2 = areaInM2,
          imageUrls = imageUrls,
          upvotedBy = upvotedBy,
          downvotedBy = downvotedBy,
          isAnonymous = isAnonymous)
    } catch (e: Exception) {
      Log.e("ReviewsRepositoryFirestore", "Error converting document to Review", e)
      null
    }
  }
}
